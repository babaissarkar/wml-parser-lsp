package com.babai.wml.tokenizer;

public final record Token(String content, Kind kind, int beginLine, int beginColumn, boolean nested) {
	public final static Token EMPTY = new Token("", Kind.EOF, 1, 1, false);
	
	public Token(String content, Kind kind) {
		this(content, kind, 0, 0, false);
	}
	
	// TODO multiline tokens
	public int endLine() { return beginLine(); }
	// FIXME correction for escaped stuff, like {}
	public int endColumn() { return beginColumn() + content.length(); } 
	
	public boolean isDirective() {
		if (kind != Token.Kind.COMMENT) return false;
		
		if (content.isEmpty()) return false;
		
		String[] directives = {
			"#define",
			"#arg",
			"#undef",
			"#ifdef",
			"#ifndef",
			"#endif",
			"#enddef",
			"#ifhave",
			"#ifnhave",
			"#ifver",
			"#ifnver",
			"#else",
			"#error",
			"#warning",
			"#deprecated",
			"#textdomain"
		};

		for (String d : directives) {
			if (content.stripLeading().startsWith(d)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isDirectiveName(String directiveName, boolean hasArg) {
		if (kind != Token.Kind.COMMENT) return false;
		
		if (content.isEmpty()) return false;
		
		return hasArg ? content.startsWith(directiveName) : content.equals(directiveName);
		// TODO throw error if hasArg = false & content.startsWith(directiveName) passes.
	}
	
	public void raw(StringBuilder buff) {
		if (buff == null || content == null) return;
		
		if (kind != Kind.MACRO) {
			buff.append(content);
		} else {
			buff.append('{').append(content).append('}');
		}
	}

	public enum Kind {
		TEXT, COMMENT, EOL, WHITESPACE, QUOTED, ANGLE_QUOTED, MACRO, EOF,
		VAL, EQL, TAG_START, TAG_END
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Token [content=");
		builder.append(content);
		builder.append(", kind=");
		builder.append(kind);
		builder.append(", beginLine=");
		builder.append(beginLine);
		builder.append(", beginColumn=");
		builder.append(beginColumn);
		builder.append("]");
		return builder.toString();
	}

	public boolean isKind(Kind...kinds) {
		for (var k : kinds) {
			if (kind == k) return true;
		}
		return false;
	}
	
	public boolean isNotKind(Kind...kinds) {
		return !isKind(kinds);
	}
}
