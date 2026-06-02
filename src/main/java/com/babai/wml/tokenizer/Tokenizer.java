package com.babai.wml.tokenizer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.babai.wml.utils.Position;

import static com.babai.wml.parser.ParseUtils.*;

public final class Tokenizer {
	private enum State { NORMAL, LINE_COMMENT, WS };
	
	public static List<Token> tokenize(String content) throws IOException {
		return tokenize(content.toCharArray());
	}

	public static List<Token> tokenize(char[] input) throws IOException {
		CharCursor r = new CharCursor(input);
		List<Token> tokens = new ArrayList<>();
		StringBuilder buff = new StringBuilder();
		int[] counts = {0, 0};
		State state = State.NORMAL;
		Position start = Position.start();

		int ch;
		while ((ch = r.read()) != -1) {
			char c = (char) ch;
			switch (state) {
			case NORMAL -> {
				if (c == '#') {
					finalizeAndAddToken(tokens, buff, Token.Kind.TEXT, start);
					state = State.LINE_COMMENT;
				} else if (isWS(c)) {
					finalizeAndAddToken(tokens, buff, Token.Kind.TEXT, start);
					state = State.WS;
					buff.append(c);
				} else if (isEOL(c)) {
					finalizeAndAddToken(tokens, buff, Token.Kind.TEXT, start);
					handleEOLToken(tokens, c, r, start);
				} else if (c == '"') {
					finalizeAndAddToken(tokens, buff, Token.Kind.TEXT, start);
					finalizeAndAddToken(tokens, readQuoteToken(r, buff, counts), Token.Kind.QUOTED, start, counts);
				} else if (c == '<') {
					finalizeAndAddToken(tokens, buff, Token.Kind.TEXT, start);

					readAngleQuoteToken(r, buff, counts);
					if (buff.isEmpty()) {
						buff.append(c);
					} else {
						finalizeAndAddToken(tokens, buff, Token.Kind.ANGLE_QUOTED, start, counts);
					}
				} else if (c == '{') {
					finalizeAndAddToken(tokens, buff, Token.Kind.TEXT, start);
					finalizeAndAddToken(tokens, readMacroToken(r, buff, counts), Token.Kind.MACRO, start, counts);
				} else {
					buff.append(c);
				}
			}

			case LINE_COMMENT -> {
				if (isEOL(c)) {
					finalizeAndAddToken(tokens, buff, Token.Kind.COMMENT, start);
					handleEOLToken(tokens, c, r, start);
					state = State.NORMAL;
				} else {
					buff.append(c);
				}
			}

			case WS -> {
				if (!isWS(c)) {
					finalizeAndAddToken(tokens, buff, Token.Kind.WHITESPACE, start);
					if (c == '#') {
						state = State.LINE_COMMENT;
					} else {
						state = State.NORMAL;
					}
				}

				if (isEOL(c)) {
					handleEOLToken(tokens, c, r, start);
				} else if (c == '"') {
					finalizeAndAddToken(tokens, readQuoteToken(r, buff, counts), Token.Kind.QUOTED, start, counts);
				} else if (c == '<') {
					finalizeAndAddToken(tokens, readAngleQuoteToken(r, buff, counts), Token.Kind.ANGLE_QUOTED, start, counts);
				} else if (c == '{') {
					finalizeAndAddToken(tokens, readMacroToken(r, buff, counts), Token.Kind.MACRO, start, counts);
				} else {
					if (c != '#') {
						buff.append(c);
					}
				}
			}
			}
		}

		if (ch == -1 && !buff.isEmpty()) {
			if (state == State.NORMAL) {
				finalizeAndAddToken(tokens, buff, Token.Kind.TEXT, start);
			} else if (state == State.LINE_COMMENT) {
				finalizeAndAddToken(tokens, buff, Token.Kind.COMMENT, start);
			} else if (state == State.WS) {
				finalizeAndAddToken(tokens, buff, Token.Kind.WHITESPACE, start);
			}
		}

		return tokens;
	}

	// Note: this assumes that r is currently at the character '"'
	// Note: we are skipping starting and ending " from the token text itself, can be deduced from token kind
	private static StringBuilder readQuoteToken(CharCursor r, StringBuilder buff, int[] counts) {
		char prevChar = '"';
		buff.setLength(0);
		int ch;
		while ((ch = r.read()) != -1) {
			char c = (char) ch;
			if (prevChar == '"' && c == '"') {
				if (buff.isEmpty()) {
					return buff;
				} else {
					buff.append(c);
				}
			} else if (prevChar != '"' && c == '"') {
				int c2 = r.read();
				if (c2 == -1) break;
				char ch2 = (char) c2;
				r.unread(ch2);
				if (ch2 != '"') break;
			} else {
				if (isEOL(c)) {
					counts[0]++;        // newline count
					counts[1] = 0;      // reset chars-since-last-newline
					if (c == '\r' && r.peek() == '\n') {
						r.read(); // consume \n of \r\n pair
					}
				}
				buff.append(c);
			}
			prevChar = c;
			counts[1]++;
		}
		return buff;
	}

	// Note: this assumes that r is currently at the first '<' character
	// Note: we are skipping << and >> from the token text itself, can be deduced from token kind
	private static StringBuilder readAngleQuoteToken(CharCursor r, StringBuilder buff, int[] counts) {
		buff.setLength(0);
		int ch = r.read();
		if (ch == -1 || ((char) ch) != '<') {
			if (ch != -1) r.unread((char) ch);
			return buff;
		}

		while ((ch = r.read()) != -1) {
			char c = (char) ch;
			if (c == '>') {
				ch = r.read();
				if (ch != -1 && ((char) ch) == '>') {
					break;
				} else {
					if (ch != -1) r.unread((char) ch);
				}
				buff.append(c);
			} else {
				if (isEOL(c)) {
					counts[0]++;
					counts[1] = 0;
					if (c == '\r' && r.peek() == '\n') r.read();
				} else {
					counts[1]++;
				}
				buff.append(c);
			}
		}
		return buff;
	}

	// Note: this assumes that r is currently at the character '{'
	// Note: we are skipping { and } from the token text itself, can be deduced from token kind
	private static StringBuilder readMacroToken(CharCursor r, StringBuilder buff, int[] counts) {
		buff.setLength(0);
		int ch;
		int nlvl = 0;

		while ((ch = r.read()) != -1) {
			char c = (char) ch;
			if (c == '{') {
				nlvl++;
				buff.append(c);
				counts[1]++;
			} else if (c == '}') {
				if (nlvl == 0) {
					break;
				} else {
					nlvl--;
					buff.append(c);
					counts[1]++;
				}
			} else {
				if (isEOL(c)) {
					counts[0]++;
					counts[1] = 0;
					if (c == '\r' && r.peek() == '\n') r.read();
				} else {
					counts[1]++;
				}
				buff.append(c);
			}
		}
		return buff;
	}

	private static void handleEOLToken(List<Token> tokens, char c, CharCursor r, Position start) {
		if (c == '\r') {
			int c2 = r.read();
			if (c2 != -1 && ((char) c2) == '\n') {
				finalizeAndAddToken(tokens, "\r\n", Token.Kind.EOL, start, 1, 0);
			} else {
				if (c2 != -1) {
					r.unread((char) c2);
				}
				finalizeAndAddToken(tokens, "\r", Token.Kind.EOL, start, 1, 0);
			}
		} else {
			finalizeAndAddToken(tokens, String.valueOf(c), Token.Kind.EOL, start, 1, 0);
		}
	}

	private static void finalizeAndAddToken(List<Token> tokens, StringBuilder buff, Token.Kind kind, Position start) {
		finalizeAndAddToken(tokens, buff.toString(), kind, start, 0, buff.length());
		buff.setLength(0);
	}

	private static void finalizeAndAddToken(List<Token> tokens, StringBuilder buff, Token.Kind kind, Position start, int[] counts) {
		finalizeAndAddToken(tokens, buff.toString(), kind, start, counts[0], counts[1]);
		if (!buff.isEmpty()) {
			buff.delete(0, buff.length());
		}

		// reset for next token
		counts[0] = 0;
		counts[1] = 0;
	}

	private static void finalizeAndAddToken(List<Token> tokens, String contents, Token.Kind kind, Position start, int ncount, int npos) {
		if (!contents.isEmpty() || kind == Token.Kind.COMMENT) {
			tokens.add(new Token(contents, kind, start.line(), start.col()));
			if (ncount == 0) {
				start.forward(npos);
			} else {
				for (int i = 0; i < ncount; i++) start.newline();
				start.forward(npos);
			}
		}
	}

	private static final class CharCursor {
		private final char[] input;
		private int idx = 0;
		private int pushback = -1;

		private CharCursor(char[] input) { this.input = input; }

		private int peek() {
			if (pushback != -1) return pushback;
			if (idx >= input.length) return -1;
			return input[idx];  // don't advance idx
		}

		private int read() {
			if (pushback != -1) {
				int c = pushback;
				pushback = -1;
				return c;
			}
			if (idx >= input.length) return -1;
			return input[idx++];
		}

		private void unread(char c) { this.pushback = c; }
	}
}
