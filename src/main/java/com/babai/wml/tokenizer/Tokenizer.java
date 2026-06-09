package com.babai.wml.tokenizer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.nio.file.Path;

import com.babai.wml.utils.Position;

import static com.babai.wml.parser.ParseUtils.*;

public final class Tokenizer {
	private enum State { NORMAL, LINE_COMMENT, WS };

	// Data extraction variables
	private static boolean enableExtraction = false;
	private static boolean extractBinPath = false;
	private static Set<Path> binaryPath = new HashSet<>();
	private static boolean extractTypeID = false;
	private static Set<String> unitTypes = new HashSet<>();
	private static boolean extractDefine = false;
	private static String mainDefine = "";
	private static boolean getNextTok;

	public static List<Token> tokenize(String content) throws IOException {
		return tokenize(content.toCharArray());
	}

	public static List<Token> tokenize(char[] input) throws IOException {
		CharCursor r = new CharCursor(input);
		List<Token> tokens = new ArrayList<>();
		StringBuilder buff = new StringBuilder(256);
		State state = State.NORMAL;
		Position start = Position.start();
		boolean leading = true;

		int ch;
		while ((ch = r.read()) != -1) {
			char c = (char) ch;
			switch (state) {
			case NORMAL -> {
				if (isWS(c)) {
					finalizeAndAddToken(tokens, buff, Token.Kind.TEXT, start);
					state = State.WS;
					buff.append(c);
				} else {
					if (isEOL(c)) {
						finalizeAndAddToken(tokens, buff, Token.Kind.TEXT, start);
						handleEOLToken(tokens, c, r, start);
					} else {
						if (c == '#') {
							finalizeAndAddToken(tokens, buff, Token.Kind.TEXT, start);
							state = State.LINE_COMMENT;
						} else if (c == '"') {
							finalizeAndAddToken(tokens, buff, Token.Kind.TEXT, start);
							handleQuoteToken(tokens, r, buff, start);
						} else if (c == '<') {
							finalizeAndAddToken(tokens, buff, Token.Kind.TEXT, start);
							handleAngleQuoteToken(tokens, r, buff, start);
						} else if (c == '{') {
							finalizeAndAddToken(tokens, buff, Token.Kind.TEXT, start);
							handleMacroToken(tokens, r, buff, start);
						} else {
							buff.append(c);
						}
					}
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
					handleQuoteToken(tokens, r, buff, start);
				} else if (c == '<') {
					handleAngleQuoteToken(tokens, r, buff, start);
				} else if (c == '{') {
					handleMacroToken(tokens, r, buff, start);
				} else if (c != '#') {
					buff.append(c);
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
	private static void handleQuoteToken(List<Token> tokens, CharCursor r, StringBuilder buff, Position start) {
		char prevChar = '"';
		buff.setLength(0);
		int ncount = 0; int npos = 0;

		int ch;
		while ((ch = r.read()) != -1) {
			char c = (char) ch;
			if (prevChar == '"' && c == '"') {
				if (buff.isEmpty()) {
					return;
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
					ncount++;        // newline count
					npos = 0;      // reset chars-since-last-newline
					if (c == '\r' && r.peek() == '\n') {
						r.read(); // consume \n of \r\n pair
					}
				}
				buff.append(c);
			}
			prevChar = c;
			ncount++;
		}

		finalizeAndAddToken(tokens, buff.toString(), Token.Kind.QUOTED, start, ncount, npos, false);
		buff.setLength(0);
	}

	// Note: this assumes that r is currently at the first '<' character
	// Note: we are skipping << and >> from the token text itself, can be deduced from token kind
	private static void handleAngleQuoteToken(List<Token> tokens, CharCursor r, StringBuilder buff, Position start) {
		buff.setLength(0);
		int ncount = 0; int npos = 0;

		int ch = r.read();
		if (ch == -1 || ((char) ch) != '<') {
			if (ch != -1) r.unread((char) ch);
			buff.append('<'); // lone '<'
			return;
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
					ncount++;
					npos = 0;
					if (c == '\r' && r.peek() == '\n') r.read();
				} else {
					npos++;
				}
				buff.append(c);
			}
		}

		finalizeAndAddToken(tokens, buff.toString(), Token.Kind.ANGLE_QUOTED, start, ncount, npos, false);
		buff.setLength(0);
	}

	// Note: this assumes that r is currently at the character '{'
	// Note: we are skipping { and } from the token text itself, can be deduced from token kind
	private static void handleMacroToken(List<Token> tokens, CharCursor r, StringBuilder buff, Position start) {
		buff.setLength(0);
		int ch;
		int nlvl = 0;
		int ncount = 0; int npos = 0;
		boolean hasNested = false;

		while ((ch = r.read()) != -1) {
			char c = (char) ch;
			if (c == '{') {
				nlvl++;
				buff.append(c);
				npos++;
			} else if (c == '}') {
				if (nlvl == 0) {
					break;
				} else {
					nlvl--;
					hasNested = true; // at least one nested matching {} pair
					buff.append(c);
					npos++;
				}
			} else {
				if (isEOL(c)) {
					ncount++;
					npos = 0;
					if (c == '\r' && r.peek() == '\n') r.read();
				} else {
					npos++;
				}
				buff.append(c);
			}
		}

		finalizeAndAddToken(tokens, buff.toString(), Token.Kind.MACRO, start, ncount, npos, hasNested);
		buff.setLength(0);
	}

	private static void handleEOLToken(List<Token> tokens, char c, CharCursor r, Position start) {
		if (c == '\r') {
			int c2 = r.read();
			if (c2 != -1 && ((char) c2) == '\n') {
				finalizeAndAddToken(tokens, "\r\n", Token.Kind.EOL, start, 1, 0, false);
			} else {
				if (c2 != -1) {
					r.unread((char) c2);
				}
				finalizeAndAddToken(tokens, "\r", Token.Kind.EOL, start, 1, 0, false);
			}
		} else {
			finalizeAndAddToken(tokens, String.valueOf(c), Token.Kind.EOL, start, 1, 0, false);
		}
	}

	private static void finalizeAndAddToken(List<Token> tokens, StringBuilder buff, Token.Kind kind, Position start) {
		finalizeAndAddToken(tokens, buff.toString(), kind, start, 0, buff.length(), false);
		buff.setLength(0);
	}

	private static void finalizeAndAddToken(List<Token> tokens, String contents, Token.Kind kind, Position start, int ncount, int npos, boolean hasNested) {
		if (!contents.isEmpty() || kind == Token.Kind.COMMENT) {
			extractData(contents);

			tokens.add(new Token(contents, kind, start.line(), start.col(), hasNested));
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

	// Data Extraction

	public static void enableExtraction(boolean enabled) {
		enableExtraction = enabled;
	}

	public static Set<Path> getBinaryPaths() {
		return binaryPath;
	}

	public static Set<String> getUnitTypes() {
		return unitTypes;
	}

	public static String getMainDefine() {
		return mainDefine;
	}

	private static void extractData(String contents) {
		// define extraction is intentionally always enabled.
		// because it is needed by the preprocessor for [campaign] main define
		// autodetection, and users mostly need that.
		if (!enableExtraction) return;
		if (contents.isEmpty()) return;
		if (contents.charAt(0) != '['
			&& !(extractBinPath || extractTypeID || extractDefine)) return;

		if (contents.equals("[binary_path]")) {
			extractBinPath = true;
		} else if (contents.equals("[unit_type]")) {
			extractTypeID = true;
		} else if (contents.equals("[campaign]")) {
			extractDefine = true;
		} else if (extractBinPath) {
			if(contents.indexOf('/') >= 0) {
				int eqlPos = contents.indexOf('=');
				if (eqlPos >= 0) {
					String path = contents.substring(5);
					if (!path.isEmpty()) {
						binaryPath.add(Path.of(path));
						extractBinPath = false;
					}
				} else if (!contents.isEmpty()) {
					binaryPath.add(Path.of(contents));
					extractBinPath = false;
				}
			}
		} else if (extractTypeID) {
			int eqlPos = contents.indexOf('=');
			if (eqlPos >= 0 && contents.startsWith("id")) {
				String name = contents.substring(3);
				// avoid cases where the unittype id is a variable or empty
				if (!name.isEmpty() && name.charAt(0) != '$') {
					unitTypes.add(name);
					extractTypeID = false;
				} else {
					getNextTok = true;
				}
			}
		} else if (extractDefine) {
			int eqlPos = contents.indexOf('=');
			if (eqlPos >= 0 && contents.startsWith("define")) {
				String define = contents.substring(7);
				if (!define.isEmpty()) {
					mainDefine = define;
					extractDefine = false;
				} else {
					getNextTok = true;
				}
			}
		} else if (getNextTok && !contents.isEmpty()) {
			if (extractTypeID) {
				// avoid cases where the unittype id is a variable or empty
				if (contents.charAt(0) != '$') {
					unitTypes.add(contents);
					extractTypeID = false;
				}
			} else if (extractDefine) {
				mainDefine = contents;
				extractDefine = false;
			}
			getNextTok = false;
		}
	}
}
