package com.babai.wml.experimental;

import java.io.Reader;
import java.io.IOException;
import java.io.PushbackReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class Tokenizer {
	public static List<Token> tokenize(Path inputPath) throws IOException {
		return tokenize(Files.newBufferedReader(inputPath));
	}
	
	// TODO: macro calls: {MYMACRO ARG1 ARG2 ...}, quoted text
	public static List<Token> tokenize(Reader reader) throws IOException {
		PushbackReader r = new PushbackReader(reader);
		List<Token> tokens = new ArrayList<>();
		StringBuilder buff = new StringBuilder();
		State state = State.NORMAL;
		
		// No state change needed in case of EOL
		
		int ch;
		while((ch = r.read()) != -1) {
			char c = (char) ch;
			switch(state) {
				case NORMAL -> {
					if (c == '#') {
						finalizeAndAddToken(tokens, buff, Token.Kind.TEXT);
						state = State.LINE_COMMENT;
						buff.append(c);
					} else if (isWS(c)) {
						finalizeAndAddToken(tokens, buff, Token.Kind.TEXT);
						state = State.WS;
						buff.append(c);
					} else if (isEOL(c)) {
						handleEOLToken(tokens, c, r);
					} else {
						buff.append(c);
					}
				}
				
				case LINE_COMMENT -> {
					if (isEOL(c)) {
						finalizeAndAddToken(tokens, buff, Token.Kind.COMMENT);
						handleEOLToken(tokens, c, r);
						state = State.NORMAL;
					} else {
						buff.append(c);
					}
				}
				
				case WS -> {
					if (!isWS(c)) {
						finalizeAndAddToken(tokens, buff, Token.Kind.WHITESPACE);
						if (c == '#') {
							state = State.LINE_COMMENT;
						} else {
							state = State.NORMAL;
						}
					}
					
					if (isEOL(c)) {
						handleEOLToken(tokens, c, r);
					} else {
						buff.append(c);
					}
				}
			}
		}
		
		
		if (ch == -1 && !buff.isEmpty()) {
			// file terminated in the middle of content, finish tokens
			// TODO maybe throw exception to warn about the issue.
			if (state == State.NORMAL) {
				finalizeAndAddToken(tokens, buff, Token.Kind.TEXT);
			} else if (state == State.LINE_COMMENT) {
				finalizeAndAddToken(tokens, buff, Token.Kind.COMMENT);
			} else if (state == State.WS) {
				finalizeAndAddToken(tokens, buff, Token.Kind.WHITESPACE);
			}
		}
		
		return tokens;
	}
	
	private static void handleEOLToken(List<Token> tokens, char c, PushbackReader r) throws IOException {
		if (c == '\r') {
			char c2 = (char) r.read();
			if (c2 == '\n') {
				finalizeAndAddToken(tokens, "\r\n", Token.Kind.EOL);
			} else {
				r.unread(c2);
			}
		} else {
			finalizeAndAddToken(tokens, "" + c, Token.Kind.EOL);
		}
	}

	private static void finalizeAndAddToken(List<Token> tokens, String contents, Token.Kind kind) {
		tokens.add(new Token(contents, kind));
	}

	private static void finalizeAndAddToken(List<Token> tokens, StringBuilder buff, Token.Kind kind) {
		tokens.add(new Token(buff.toString(), kind));
		buff.delete(0, buff.length());
	}
	
	private static boolean isEOL(char c) {
		return c == '\n' || c == '\r' || c == '\u2028' || c == '\u2029';
	}

	private static boolean isWS(char c) {
		return Character.isWhitespace(c) && !isEOL(c);
	}

	
	private enum State { NORMAL, LINE_COMMENT, WS };
}
