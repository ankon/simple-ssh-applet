package com.fredhopper.tools.simplessh.terminal;

import java.io.IOException;

import com.fredhopper.tools.simplessh.screen.ScreenBuffer;
import com.fredhopper.tools.simplessh.terminal.TerminalOutputStream;

public class DumbTerminalOutputStream extends TerminalOutputStream {
	public DumbTerminalOutputStream(ScreenBuffer screenBuffer) {
		super(screenBuffer);
	}

	@Override
	public void processInput(int b) throws IOException {
		if (b == '\r') {
			cursorColumn = 0;
		} else if (b == '\n') {
			if (cursorRow == screenBuffer.getRows() - 1) {
				screenBuffer.scrollUp(1);
			} else {
				cursorRow++;		
			}
		} else {
			screenBuffer.setCharAt(cursorRow, cursorColumn++, (char) b);
			if (cursorColumn == screenBuffer.getColumns()) {
				cursorColumn = 0;
				if (cursorRow == screenBuffer.getRows() - 1) {
					screenBuffer.scrollUp(1);
				} else {
					cursorRow++;						
				}
			}
		}
	}

	@Override
	public String getTerminalType() {
		return "dumb";
	}
}