package com.babai.wml.experimental;

import java.awt.Color;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import com.babai.wml.utils.Colors;

import static com.babai.wml.utils.ANSIFormatter.*;

final class LogUtils {
	private final static Logger pL = Logger.getLogger("preprocessor.parse");
	
	// TODO setters needed
	private static boolean showParseLogs = true;
	private static boolean warnParseLogs = true;
	private static boolean disableErrors = false;
	
	static {
		setLoggingFormat();
	}
	
	private LogUtils() {}
	
	private static void setLoggingFormat() {
		for (var handler : Logger.getLogger("").getHandlers()) {
			handler.setFormatter(new java.util.logging.Formatter() {
				@Override
				public String format(LogRecord r) {
					// Customize Message for separators between Level and Message
					Level l = r.getLevel();
					String lvlStr = "[" + l + "]";
					if (l == Level.SEVERE) {
						lvlStr = colorify(lvlStr, Color.RED);
					} else if (l == Level.WARNING) {
						lvlStr = colorify(lvlStr, Color.ORANGE);
					} else if (l == Level.INFO) {
						lvlStr = colorify(lvlStr, Color.CYAN);
					}

					return lvlStr + " " + r.getMessage() + "\n";
				}
			});
		}
	}
	
	@SuppressWarnings("unused")
	private static void debugPrintTokJ(Token t) {
		var frame = StackWalker.getInstance()
				.walk(s -> s.skip(1).findFirst().get());
		System.out.println("[tok, " + frame.getMethodName() +":L" + frame.getLineNumber() + "]: " + t);
	}

	@SuppressWarnings("unused")
	private static void debugPrintJ(String str) {
		var frame = StackWalker.getInstance()
				.walk(s -> s.skip(1).findFirst().get());
		System.out.println("[" + frame.getMethodName() +":L" + frame.getLineNumber() + "]: " + str);
	}
	
	public static void debugPrint(String s) {
		if (showParseLogs) {
			pL.info(s);
		}
	}

	public static void warningPrint(String s) {
		if (showParseLogs || warnParseLogs) {
			pL.warning(s);
		}
	}
	
	public static void errorPrint(String s) {
		// temporary, to test lsp
		if (!disableErrors) {
			pL.severe(s);
		}
	}

	public static String position(Token tok) {
		return position(tok.beginLine(), tok.beginColumn());
	}

	public static String position(int line, int col) {
		return colorify("(" + line + ":" + col + ")", Colors.lineNumColor);
	}

}
