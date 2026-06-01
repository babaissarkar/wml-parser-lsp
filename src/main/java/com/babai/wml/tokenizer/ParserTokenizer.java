package com.babai.wml.tokenizer;

import java.io.IOException;
import java.util.function.BiConsumer;

import com.babai.wml.utils.Position;

import static com.babai.wml.parser.ParseUtils.*;

public final class ParserTokenizer {
	private enum State { NORMAL, LINE_COMMENT };
	
	private static BiConsumer<Token.Kind, StringBuilder> eventHandler = null;
	
	public static void addEvent(BiConsumer<Token.Kind, StringBuilder> handler) {
		eventHandler = handler;
	}

	public static void tokenize(char[] input) throws IOException {
		CharCursor r = new CharCursor(input);
		StringBuilder buff = new StringBuilder();
		int[] counts = {0, 0};
		State state = State.NORMAL;
		Position start = Position.start();
		Token.Kind lastTextKind = Token.Kind.TEXT;

		int ch;
		while ((ch = r.read()) != -1) {
			char c = (char) ch;
			switch (state) {
			case NORMAL -> {
				if (c == '#') {
					lastTextKind = Token.Kind.TEXT;
					state = State.LINE_COMMENT;
				} else if (isWS(c)) {
					continue;
				} else if (isEOL(c)) {
					// RVAL mode (VAL) is terminated here.
					finalize(buff, lastTextKind, start);
					lastTextKind = Token.Kind.TEXT;
					
					if (readEOLToken(c, r, start)) {
						fireEvent(buff, Token.Kind.EOL);
						start.newline();
					}
				} else if (c == '"') {
					if (lastTextKind != Token.Kind.VAL) {
						finalize(buff, lastTextKind, start);
						lastTextKind = Token.Kind.TEXT;
					}
					
					readQuoteToken(r, buff, counts);
					
					if (lastTextKind != Token.Kind.VAL) {
						finalize(buff, Token.Kind.QUOTED, start, counts);
					}
				} else if (c == '<') {
					if (lastTextKind != Token.Kind.VAL) {
						finalize(buff, lastTextKind, start);
						lastTextKind = Token.Kind.TEXT;
					}
					
					readAngleQuoteToken(r, buff, counts);
					if (buff.isEmpty()) {
						buff.append(c);
					} else if (lastTextKind != Token.Kind.VAL) {
						finalize(buff, Token.Kind.ANGLE_QUOTED, start, counts);
					}
				} else if (c == '{') {
					// TODO complain if found
					if (lastTextKind != Token.Kind.VAL) {
						finalize(buff, lastTextKind, start);
						lastTextKind = Token.Kind.TEXT;
					}
					
					skipMacroToken(r);
				} else if (c == '[') {
					if (lastTextKind != Token.Kind.VAL) {
						finalize(buff, lastTextKind, start);
						lastTextKind = Token.Kind.TEXT;
					}
					
					Token.Kind type = readTagToken(r, buff);
					
					if (lastTextKind != Token.Kind.VAL) {
						finalize(buff, type, start);
					}
				} else if (c == '=' && lastTextKind == Token.Kind.TEXT) {
					// TODO complain if no '=' found in keyval line, as WML can't have plain lines
					// TODO standalone '=' in a line is also wrong

					finalize(buff, lastTextKind, start);
					
					fireEvent(buff, Token.Kind.EQL);
					start.forward(1);
					
					lastTextKind = Token.Kind.VAL;
				} else if (c == '+' && lastTextKind == Token.Kind.VAL) {
					// don't add it, VAL mode is concating anyway
				} else {
					buff.append(c);
				}
			}

			case LINE_COMMENT -> {
				if (isEOL(c)) {
					if (readEOLToken(c, r, start)) {
						start.newline();
					}
					state = State.NORMAL;
				}
			}
			}
		}

		if (ch == -1 && !buff.isEmpty()) {
			if (state == State.NORMAL) {
				finalize(buff, lastTextKind, start);
				lastTextKind = Token.Kind.TEXT;
			}
		}
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
	private static void skipMacroToken(CharCursor r) {
		int ch;
		int nlvl = 0;

		while ((ch = r.read()) != -1) {
			char c = (char) ch;
			if (c == '{') {
				nlvl++;
			} else if (c == '}') {
				if (nlvl == 0) {
					break;
				} else {
					nlvl--;
				}
			} else {
				if (isEOL(c)) {
					if (c == '\r' && r.peek() == '\n') r.read();
				}
			}
		}
	}

	private static Token.Kind readTagToken(CharCursor r, StringBuilder buff) {
		buff.setLength(0);
		Token.Kind type = Token.Kind.TAG_START;
		boolean firstChar = true;
		int ch;
		while ((ch = r.read()) != -1) {
			char c = (char) ch;
			if (c == ']') {
				break;
			} else {
				if (firstChar) { // first char after '['
					if (c == '/') {
						type = Token.Kind.TAG_END;
					} else if (c == '+') {
						/* Ignore */
					} else {
						buff.append(c);
					}
				} else {
					buff.append(c);
				}
			}
			firstChar = false;
		}
		return type;
	}

	private static boolean readEOLToken(char c, CharCursor r, Position start) {
		boolean newlineFound = false;
		if (c == '\r') {
			int c2 = r.read();
			if (c2 != -1 && ((char) c2) == '\n') {
				newlineFound = true;
			} else {
				if (c2 != -1) {
					r.unread((char) c2);
				}
				newlineFound = true;
			}
		} else {
			newlineFound = true;
		}
		
		return newlineFound;
	}

	private static void finalize(StringBuilder buff, Token.Kind kind, Position start) {
		finalize(buff, kind, start, 0, buff.length());
	}

	private static void finalize(StringBuilder buff, Token.Kind kind, Position start, int[] counts) {
		finalize(buff, kind, start, counts[0], counts[1]);
		if (!buff.isEmpty()) {
			buff.setLength(0);
		}

		// reset for next token
		counts[0] = 0;
		counts[1] = 0;
	}

	private static void finalize(StringBuilder contents, Token.Kind kind, Position start, int ncount, int npos) {
		if (!contents.isEmpty() || kind == Token.Kind.COMMENT) {
			fireEvent(contents, kind);
			if (!contents.isEmpty()) {
				contents.setLength(0);
			}
			
			if (ncount == 0) {
				start.forward(npos);
			} else {
				for (int i = 0; i < ncount; i++) start.newline();
				start.forward(npos);
			}
		}
	}

	private static void fireEvent(StringBuilder contents, Token.Kind kind) {
		if (eventHandler != null) {
			eventHandler.accept(kind, contents);
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
