package com.fredhopper.tools.simplessh.terminal;

import java.io.IOException;
import java.io.OutputStream;

import com.fredhopper.tools.simplessh.screen.ScreenBuffer;

public abstract class TerminalOutputStream extends OutputStream {
	protected final ScreenBuffer screenBuffer;
	protected int cursorRow, cursorColumn;
	private long lastUpdate;
	
	protected TerminalOutputStream(ScreenBuffer screenBuffer) {
		this.screenBuffer = screenBuffer;
	}
	
	public long getLastUpdate() {
		return lastUpdate;
	}
	
	public int getCursorRow() {
		return cursorRow;
	}

	public void setCursorRow(int cursorRow) {
		this.cursorRow = cursorRow;
	}
	
	public int getCursorColumn() {
		return cursorColumn;
	}
	
	public void setCursorColumn(int cursorColumn) {
		this.cursorColumn = cursorColumn;
	}
	
	@Override
	public void write(int b) throws IOException {
		processInput(b);
		lastUpdate = System.nanoTime();
	}
	
	protected abstract void processInput(int b) throws IOException;

	public abstract String getTerminalType();
}