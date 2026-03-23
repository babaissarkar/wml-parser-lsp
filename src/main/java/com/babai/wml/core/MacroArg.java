package com.babai.wml.core;

import com.babai.wml.utils.AIGenerated;

@AIGenerated // Claude
public class MacroArg {
	private final String value;
	private final int startLine;
	private final int startChar;
	private final int endChar;

	public MacroArg(String value, int startLine, int startChar, int endChar) {
		this.value = value;
		this.startLine = startLine;
		this.startChar = startChar;
		this.endChar = endChar;
	}

	public String value()    { return value; }
	public int startLine()   { return startLine; }
	public int startChar()   { return startChar; }
	public int endChar()     { return endChar; }

	@Override
	public String toString() {
		return "MacroArg{'" + value + "' @" + startLine + ":" + startChar + "}";
	}
}