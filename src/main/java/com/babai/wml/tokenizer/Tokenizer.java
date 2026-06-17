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
	private static StringBuilder lineBuff = new StringBuilder();

	public static List<Token> tokenize(String content) throws IOException {
		return tokenize(content.toCharArray());
	}

	public static List<Token> tokenize(char[] input) throws IOException {
		lineBuff.setLength(0);

		CharCursor r = new CharCursor(input);
		List<Token> tokens = new ArrayList<>();
		StringBuilder buff = new StringBuilder(256);
		State state = State.NORMAL;
		Position start = Position.start();

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
							buff.append(c);
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
						buff.append(c);
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
	private static void handleQuoteToken(List<Token> tokens, CharCursor r, StringBuilder buff, Position start) {
		char prevChar = 0;
		buff.setLength(0);
		int ncount = 0;

		buff.append('"');
		int npos = 1;
		
		int ch;
		while ((ch = r.read()) != -1) {
			char c = (char) ch;
			if (prevChar == '"' && c == '"') {
				if (buff.length() == 1) {
					buff.append(c);
					npos++;
					break;
				}
				// else case: consecutive "" in middle of string: only 1 is added
			} else if (prevChar != '"' && c == '"') {
				buff.append(c);
				npos++;
				
				int c2 = r.peek();
				if (c2 == -1 || (char) c2 != '"') break;
			} else {
				if (isEOL(c)) {
					ncount++;
					npos = 0;
					if (c == '\r' && r.peek() == '\n') {
						r.read(); // consume \n of \r\n pair
					}
				} else {
					npos++;
				}
				buff.append(c);
			}
			prevChar = c;
		}

		finalizeAndAddToken(tokens, buff.toString(), Token.Kind.QUOTED, start, ncount, npos, false);
		buff.setLength(0);
	}

	// Note: this assumes that r is currently at the first '<' character
	private static void handleAngleQuoteToken(List<Token> tokens, CharCursor r, StringBuilder buff, Position start) {
		buff.setLength(0);
		int ncount = 0; int npos = 0;
		
		buff.append('<');
		int ch = r.read();
		if (ch == -1 || ((char) ch) != '<') {
			if (ch != -1) r.unread((char) ch);
			return; // lone '<'
		}

		buff.append('<');
		while ((ch = r.read()) != -1) {
			char c = (char) ch;
			if (c == '>') {
				ch = r.read();
				if (ch != -1 && ((char) ch) == '>') {
					buff.append(c).append((char) ch);
					break;
				} else if (ch != -1) {
					r.unread((char) ch);
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
			if (kind != Token.Kind.WHITESPACE) {
				if (kind != Token.Kind.EOL) {
					extractData(contents);
				} else {
					commitBuff();
				}
			}

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

	private static void commitBuff() {
		if (enableExtraction && extractTypeID && !lineBuff.isEmpty()) {
			String unitType = "";
			if (lineBuff.charAt(0) == '"' && lineBuff.charAt(lineBuff.length() - 1) == '"') {
				unitType = lineBuff.substring(1, lineBuff.length() - 1);
			} else {
				unitType = lineBuff.toString();
			}
			
			// extra condition: unittype must start with alphabetic
			if (!unitType.isEmpty() && Character.isAlphabetic(unitType.charAt(0))) {
				unitTypes.add(unitType);
			}
			extractTypeID = false;
			lineBuff.setLength(0);
		}
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
			if (contents.indexOf('/') >= 0) {
				int eqlPos = contents.indexOf('=');
				if (eqlPos >= 0) {
					String path = contents.substring(5).strip();
					if (path.charAt(0) == '"' && path.charAt(path.length() - 1) == '"') {
						path = path.substring(1, path.length() - 1);
					}
					if (!path.isEmpty()) {
						binaryPath.add(Path.of(path));
						extractBinPath = false;
					}
				} else if (!contents.isEmpty()) {
					String path = contents.strip();
					if (path.charAt(0) == '"' && path.charAt(path.length() - 1) == '"') {
						path = path.substring(1, path.length() - 1);
					}
					binaryPath.add(Path.of(path));
					extractBinPath = false;
				}
			}
		} else if (extractTypeID) {
			if (contents.startsWith("id") && lineBuff.isEmpty()) {
				int eqlPos = contents.indexOf('=');

				if (eqlPos >= 0) {
					String name = contents.substring(3);
					// avoid cases where the unittype id is a variable or empty
					if (!name.isEmpty() && name.charAt(0) != '$') {
						lineBuff.append(name);
					} else {
						getNextTok = true;
					}
				} else {
					getNextTok = true;
				}
			} else {
				if (!lineBuff.isEmpty()) {
					lineBuff.append(" ");
				}
				lineBuff.append(contents);
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
		} else if (getNextTok) {
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

			// separate '=' token, keep checking
			getNextTok = contents.charAt(0) == '=';
		}
	}

	public static void clearUnitTypes() {
		unitTypes.clear();
	}
}
