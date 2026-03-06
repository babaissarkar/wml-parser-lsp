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
	
	public boolean isDirective() {
		if (kind != Token.Kind.COMMENT) return false;
		
		if (content.isEmpty()) return false;
		
		String[] directives = {
			"define",
			"arg",
			"undef",
			"ifdef",
			"ifndef",
			"ifhave",
			"ifnhave",
			"ifver",
			"ifnver",
			"error",
			"warning",
			"deprecated"
		};

		for (String d : directives) {
			if (content.startsWith(d)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isDirectiveName(String directiveName, boolean hasArg) {
		return hasArg ? content.startsWith(directiveName) : content.equals(directiveName);
		// TODO throw error if hasArg = false & content.startsWith(directiveName) passes.
	}

	@Override
	public String toString() {
		return "Token[content=" + content + ", kind=" + kind + "]";
	}

	public enum Kind {
		TEXT, COMMENT, EOL, WHITESPACE, QUOTED, ANGLE_QUOTED, MACRO
	}
}
