package com.babai.wml.experimental;

public class Position {
	private int col;
	private int line;
	
	public Position(int col, int line) {
		this.col = col;
		this.line = line;
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
		this.col = 0;
		this.line += 1;
	}
	
	public int col() { return col; }
	public int line() { return line; }
}
