package com.fredhopper.tools.simplessh;

import java.applet.Applet;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import com.fredhopper.tools.simplessh.screen.ScreenBuffer;
import com.fredhopper.tools.simplessh.terminal.DumbTerminalOutputStream;
import com.fredhopper.tools.simplessh.terminal.TerminalOutputStream;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

public class SimpleSSHApplet extends Applet implements KeyListener {
	private static final long serialVersionUID = 1L;

	private static class EmptyUserInfo implements UserInfo {
		public String getPassphrase() {
			return null;
		}

		public String getPassword() {
			return null;
		}

		public boolean promptPassphrase(String arg0) {
			return false;
		}

		public boolean promptPassword(String arg0) {
			return false;
		}

		public boolean promptYesNo(String arg0) {
			return false;
		}

		public void showMessage(String arg0) {
		}
	}

	private static class ChannelInputStream extends InputStream {
		private byte[] buffer = new byte[8192];
		private int writePos = 0;
		private int readPos = 0;
		
		public void write(int b) {
			synchronized(this) {
				buffer[writePos++] = (byte) b;
				if (writePos == buffer.length) {
					buffer = Arrays.copyOfRange(buffer, 0, buffer.length * 2);
				}
				notifyAll();
			}
		}
		
		private void waitAvailable() throws InterruptedException {
			synchronized(this) {
				if (readPos >= writePos) {
					wait();
				}
			}			
		}
		
		@Override
		public int read() throws IOException {
			synchronized(this) {
				try {
					waitAvailable();
				} catch (InterruptedException e) {
					throw new IOException(e);
				}
				byte b = buffer[readPos++];
				if (readPos > buffer.length / 2) {
					writePos -= readPos;
					System.arraycopy(buffer, readPos, buffer, 0, writePos);
					readPos = 0;
				}
				return b;
			}
		}
		
		@Override
	    public int read(byte b[], int off, int len) throws IOException {
			if (len == 0) {
				return 0;
			}
	    	synchronized(this) {
				try {
					waitAvailable();
				} catch (InterruptedException e) {
					throw new IOException(e);
				}
				int toCopy = Math.min(len, writePos - readPos);
				assert toCopy >= 1;
				System.arraycopy(buffer, readPos, b, off, toCopy);
				readPos += toCopy;
				if (readPos > buffer.length / 2) {
					writePos -= readPos;
					System.arraycopy(buffer, readPos, buffer, 0, writePos);
					readPos = 0;
				}
				return toCopy;
	    	}
	    }
	}
	
	private ScreenBuffer screenBuffer;
	private TerminalOutputStream terminalOutputStream;
	private Font font;
	private JSch jsch;
	private Session session;
	private Channel channel;
	private ChannelInputStream channelInputStream;
	private long lastPaintTime = 0;
	
	@Override
	public void init() {
		channelInputStream = new ChannelInputStream();
		font = new Font("Monospaced", Font.PLAIN, 12);
		screenBuffer = new ScreenBuffer(getHeight() / font.getSize(), getWidth() / font.getSize());
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		if (isActive()) {
			screenBuffer = new ScreenBuffer(screenBuffer, getHeight() / font.getSize(), getWidth() / font.getSize());
			terminalOutputStream = createTerminalOutputStream(screenBuffer, terminalOutputStream.getTerminalType());
			channel.setOutputStream(terminalOutputStream);
		}
	}
	
	@Override
	public void start() {
		try {
			jsch = new JSch();

			// jsch.setKnownHosts("/home/foo/.ssh/known_hosts");
			String user = getParameter("user");
			String host = getParameter("host");
			String password = getParameter("password");

			session = jsch.getSession(user, host, 22);
			session.setPassword(password);

			UserInfo ui = new EmptyUserInfo();
			session.setUserInfo(ui);

			// It must not be recommended, but if you want to skip host-key
			// check,
			// invoke following,
			session.setConfig("StrictHostKeyChecking", "no");

			// session.connect();
			session.connect(30000); // making a connection with timeout.

			channel = session.openChannel("shell");
			channel.setInputStream(channelInputStream);
			
			terminalOutputStream = createTerminalOutputStream(screenBuffer, /* dont care */ null);
			((ChannelShell) channel).setPtyType(terminalOutputStream.getTerminalType());
			channel.setOutputStream(terminalOutputStream);

			/*
			 * // Set environment variable "LANG" as "ja_JP.eucJP".
			 * ((ChannelShell)channel).setEnv("LANG", "ja_JP.eucJP");
			 */

			// channel.connect();
			channel.connect(3 * 1000);
		} catch (Exception e) {
			System.out.println(e);
		}
		Timer t = new Timer(true);
		t.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				validate();
				repaint(500);
			}
		}, 500, 500);
		addKeyListener(this);
	}

	protected TerminalOutputStream createTerminalOutputStream(ScreenBuffer screenBuffer, String type) {
		// TODO: implement vt100 (see http://ascii-table.com/documents/vt100/ )
		return new DumbTerminalOutputStream(screenBuffer);
	}

	@Override
	public void paint(Graphics g) {
		if (lastPaintTime < terminalOutputStream.getLastUpdate()) {
			update(g);
		}
	}
	
	@Override
	public void update(Graphics g) {
		if (channel == null || !channel.isConnected()) {
			g.clearRect(0, 0, getWidth(), getHeight());
			g.drawString("Connecting", 0, getHeight() / 2);
		} else {
			g.setFont(font);
			for (int row = 0; row < screenBuffer.getRows(); row++) {
				g.setColor(getBackground());
				g.fillRect(0, row * font.getSize(), getWidth(), font.getSize());
				
				g.setColor(getForeground());
				String line = screenBuffer.getLine(row);
				g.drawString(line, 0, (row + 1) * font.getSize());
			}
		}
		lastPaintTime = System.nanoTime();
	}

	public void keyTyped(KeyEvent e) {
		channelInputStream.write(e.getKeyChar());
	}

	public void keyPressed(KeyEvent e) {
	}

	public void keyReleased(KeyEvent e) {
	}
}
