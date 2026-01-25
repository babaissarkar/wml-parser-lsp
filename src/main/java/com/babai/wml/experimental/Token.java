package com.babai.wml.experimental;

public final class Token {
	private String content;
	private Kind kind;
	
	public Token(String content, Kind kind) {
		this.content = content;
		this.kind = kind;
	}
	
	public String getContent() {
		return content;
	}

	public Kind getKind() {
		return kind;
	}

	public enum Kind {
		TEXT, COMMENT, EOL, WHITESPACE
	}
}
