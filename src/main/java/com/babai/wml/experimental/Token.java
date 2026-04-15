package com.babai.wml.experimental;

public final class Token {
	public final static Token EMPTY = new Token("", Kind.EOF);
	
	private String content;
	private Kind kind;
	
	private int beginLine, endLine;
	private int beginColumn, endColumn;
	
	public Token(String content, Kind kind) {
		this.content = content;
		this.kind = kind;
	}
	
	public Token(String content, Kind kind, int beginLine, int beginColumn) {
		this.content = content;
		this.kind = kind;
		this.beginLine = beginLine;
		this.beginColumn = beginColumn;
	}
	
	public String content() { return content; }
	public Kind kind() { return kind; }
	public int beginLine() { return beginLine; }
	public int endLine() { return endLine; }
	public int beginColumn() { return beginColumn; }
	public int endColumn() { return endColumn; }
	
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
			"else",
			"error",
			"warning",
			"deprecated",
			"textdomain",
			"wmlscope",
			"wmllint"
		};

		for (String d : directives) {
			if (content.stripLeading().startsWith(d)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isDirectiveName(String directiveName, boolean hasArg) {
		return hasArg ? content.startsWith(directiveName) : content.equals(directiveName);
		// TODO throw error if hasArg = false & content.startsWith(directiveName) passes.
	}

	public enum Kind {
		TEXT, COMMENT, EOL, WHITESPACE, QUOTED, ANGLE_QUOTED, MACRO, TAG, EOF
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
		builder.append(", endLine=");
		builder.append(endLine);
		builder.append(", beginColumn=");
		builder.append(beginColumn);
		builder.append(", endColumn=");
		builder.append(endColumn);
		builder.append("]");
		return builder.toString();
	}
}
