package com.babai.wml.experimental;

/** Position is 1 based */
public class Position {
	private int col;
	private int line;
	
	public Position(int col, int line) {
		this.col = col;
		this.line = line;
	}
	
	public static Position start() {
		return new Position(1, 1);
	}
	
	/** Copy ctor */
	public Position(Position p) {
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
