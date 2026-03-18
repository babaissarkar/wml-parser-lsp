package com.babai.wml.experimental;

/** Position is 1 based */
public class CursorPosition {
	private int col;
	private int line;
	
	public CursorPosition(int col, int line) {
		this.col = col;
		this.line = line;
	}
	
	public static CursorPosition start() {
		return new CursorPosition(1, 1);
	}
	
	/** Copy ctor */
	public CursorPosition(CursorPosition p) {
		this.line = p.line;
		this.col = p.col;
	}
	
	public void forward(int colDelta) {
		this.col += colDelta;
	}
	
	public void newline() {
		this.col = 1;
		this.line += 1;
	}
	
	public int col() { return col; }
	public int line() { return line; }
}
