package com.babai.wml.experimental;

import java.util.logging.Logger;
import com.babai.wml.utils.Colors;

import static com.babai.wml.utils.ANSIFormatter.*;

final class LogUtils {
	private final static Logger pL = Logger.getLogger("preprocessor.parse");
	private static boolean showParseLogs;
	private static boolean warnParseLogs;
	private static boolean disableErrors;
	
	private LogUtils() {}
	
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
