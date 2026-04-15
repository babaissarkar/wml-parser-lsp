package com.babai.wml.experimental;

import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static com.babai.wml.utils.Colors.*;
import static com.babai.wml.utils.ANSIFormatter.*;

public final class LogUtils {
	private final static Logger pL = Logger.getLogger("preprocessor.parse");
	
	static {
		setupLogger();
	}
	
	private LogUtils() {}
	
	public static void setLogLevel(Level lvl) {
		pL.setLevel(lvl);
		for (var handler : pL.getHandlers()) {
			handler.setLevel(lvl);
		}
	}
	
	private static void setupLogger() {
		pL.setUseParentHandlers(false);
		ConsoleHandler handler = new ConsoleHandler();
		handler.setFormatter(new Formatter() {
			@Override
			public String format(LogRecord record) {
				// Customize Message for separators between Level and Message
				Level l = record.getLevel();
				String lvlStr = "[" + l + "]";
				if (l == Level.SEVERE) {
					lvlStr = colorify(lvlStr, RED);
				} else if (l == Level.WARNING) {
					lvlStr = colorify(lvlStr, ORANGE);
				} else if (l == Level.INFO) {
					lvlStr = colorify(lvlStr, GREEN);
				} else if (l == Level.FINER) {
					lvlStr = colorify("[DEBUG]", CYAN);
				}
				
				return lvlStr + " " + record.getMessage() + "\n";
			}
		});
		pL.addHandler(handler);
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
	
	public static void infoPrint(String s) {
		pL.info(s);
	}
	
	public static void debugPrint(String s) {
		pL.finer(s);
	}

	public static void warningPrint(String s) {
		pL.warning(s);
	}
	
	public static void errorPrint(String s) {
		pL.severe(s);
	}

	public static String position(Token tok, String path) {
		return colorify(path + ":" + "(" + tok.beginLine() + "," + tok.beginColumn() + ")", lineNumColor);
	}

	public static String position(int line, int col) {
		return colorify("(" + line + "," + col + ")", lineNumColor);
	}

}
