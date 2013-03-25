package com.fredhopper.tools.simplessh.screen;

import java.util.Arrays;

public class ScreenBuffer {
	private final char[] characters;
	private final long[] attributes;
	private final int rows;
	private final int columns;

	public ScreenBuffer(int rows, int columns) {
		this.rows = rows;
		this.columns = columns;
		this.characters = new char[rows * columns];
		this.attributes = new long[rows * columns];
	}

	public ScreenBuffer(ScreenBuffer screenBuffer, int rows, int columns) {
		this(rows, columns);
		int copyRows = Math.min(screenBuffer.rows, rows);
		int copyColums = Math.min(screenBuffer.columns, columns);
		for (int i = 0; i < copyRows; i++) {
			System.arraycopy(screenBuffer.characters, i * screenBuffer.columns, characters, i * columns, copyColums);
			System.arraycopy(screenBuffer.attributes, i * screenBuffer.columns, attributes, i * columns, copyColums);
		}
	}

	public int getRows() {
		return rows;
	}

	public int getColumns() {
		return columns;
	}

	private int idx(int row, int col) {
		return row * columns + col;
	}

	public char getCharAt(int row, int col) {
		return characters[idx(row, col)];
	}

	public String getLine(int row) {
		StringBuilder sb = new StringBuilder();
		for (int i = row * columns; i < (row + 1) * columns; i++) {
			if (characters[i] != 0) {
				sb.append(characters[i]);
			}
		}
		return sb.toString();
	}

	public void setCharAt(int row, int col, char c) {
		characters[idx(row, col)] = c;
	}

	public void scrollUp(int lines) {
		System.arraycopy(characters, lines * columns, characters, 0, (rows - lines) * columns);
		Arrays.fill(characters, (rows - lines) * columns, characters.length, '\0');
		System.arraycopy(attributes, lines * columns, attributes, 0, (rows - lines) * columns);
		Arrays.fill(attributes, (rows - lines) * columns, attributes.length, attributes[attributes.length - 1]);
	}
}