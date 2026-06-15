package com.babai.wml.preprocessor;

import java.util.List;

public class MacroCall {
	private final String name;
	private final int startLine;
	private final int startChar;
	private final List<Integer> argPos;
	private final String uri;

	public MacroCall(String name, int startLine, int startChar, List<Integer> argPos, String uri) {
		this.name = name;
		this.startLine = startLine;
		this.startChar = startChar;
		this.argPos = argPos;
		this.uri = uri;
	}

	public String name()         { return name; }
	public int startLine()       { return startLine; }
	public int startChar()       { return startChar; }
	public String uri()          { return uri; }

	public int positions(int i) {
		return i < argPos.size() ? argPos.get(i) : 0; // TODO improve bounds check
	}
	
	@Override
	public String toString() {
		return "MacroCall [name=" + name + ", startLine=" + startLine + ", startChar=" + startChar + ", argPos="
				+ argPos + ", uri=" + uri + "]";
	}
}
