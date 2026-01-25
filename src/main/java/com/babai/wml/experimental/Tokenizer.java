package com.babai.wml.experimental;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class Tokenizer {
	public static List<Token> tokenize(Path inputPath) throws IOException {
		return tokenize(Files.newBufferedReader(inputPath));
	}
	
	public static List<Token> tokenize(BufferedReader r) throws IOException {
		List<Token> tokens = new ArrayList<>();
		StringBuilder buff = new StringBuilder();
		State state = State.NORMAL;
		
		int ch;
		while((ch = r.read()) != -1) {
			char c = (char) ch;
			switch(state) {
				case NORMAL -> {
					if (c == '#') {
						tokens.add(new Token(buff.toString(), Token.Kind.TEXT));
						buff.delete(0, buff.length());
						
						state = State.LINE_COMMENT;
					}
					buff.append(c);
				}
				
				case LINE_COMMENT -> {
					if (isEOL(c)) {
						tokens.add(new Token(buff.toString(), Token.Kind.COMMENT));
						buff.delete(0, buff.length());
						
						state = State.NORMAL;
					}
					buff.append(c);
				}
			}
		}
		
		
		if (ch == -1 && !buff.isEmpty()) {
			// file terminated in the middle of content, finish tokens
			// TODO maybe throw exception to warn about the issue.
			if (state == State.NORMAL) {
				tokens.add(new Token(buff.toString(), Token.Kind.TEXT));
				buff.delete(0, buff.length());
			} else if (state == State.LINE_COMMENT) {
				tokens.add(new Token(buff.toString(), Token.Kind.COMMENT));
				buff.delete(0, buff.length());
			}
		}
		
		return tokens;
	}
	
	private static boolean isEOL(char c) {
		return c == '\n' || c == '\r' || c == '\u2028' || c == '\u2029';
	}

	private static boolean isWS(char c) {
		return Character.isWhitespace(c) && !isEOL(c);
	}

	
	private enum State { NORMAL, LINE_COMMENT };
}
