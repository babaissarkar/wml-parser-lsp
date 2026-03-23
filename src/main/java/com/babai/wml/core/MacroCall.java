package com.babai.wml.core;

import java.util.Collections;
import java.util.List;

import com.babai.wml.utils.AIGenerated;

@AIGenerated // Claude
public class MacroCall {
	private final String name;
	private final int startLine;
	private final int endLine;
	private final int startChar;
	private final int endChar;
	private final List<MacroArg> args;
	private final String uri;

	public MacroCall(String name, int startLine, int endLine, int startChar, int endChar, List<MacroArg> args, String uri) {
		this.name = name;
		this.startLine = startLine;
		this.endLine = endLine;
		this.startChar = startChar;
		this.endChar = endChar;
		this.args = Collections.unmodifiableList(args);
		this.uri = uri;
	}

	public String name()         { return name; }
	public int startLine()       { return startLine; }
	public int endLine()         { return endLine; }
	public int startChar()       { return startChar; }
	public int endChar()         { return endChar; }
	public List<MacroArg> args() { return args; }
	public String uri()          { return uri; }

	@Override
	public String toString() {
		return "MacroCall [name=" + name + ", startLine=" + startLine + ", endLine=" + endLine + ", startChar="
				+ startChar + ", endChar=" + endChar + ", args=" + args + ", uri=" + uri + "]";
	}
}
