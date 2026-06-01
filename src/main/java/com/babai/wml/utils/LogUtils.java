package com.babai.wml.utils;

import java.util.function.Supplier;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.babai.wml.tokenizer.Token;

import static com.babai.wml.cli.ANSIFormatter.*;
import static com.babai.wml.utils.Colors.*;

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
				String lvlStr;
				if (l == Level.SEVERE) {
					lvlStr = colorify("[ERROR]", RED);
				} else if (l == Level.WARNING) {
					lvlStr = colorify("[" + l + "]", ORANGE);
				} else if (l == Level.INFO) {
					lvlStr = colorify("[" + l + "]", GREEN);
				} else if (l == Level.FINER) {
					lvlStr = colorify("[DEBUG]", CYAN);
				} else {
					lvlStr = "[" + l + "]";
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
	
	public static void infoPrint(Supplier<String> s) {
		if (pL.isLoggable(Level.INFO)) {
			pL.info(s.get());
		}
	}
	
	public static void debugPrint(Supplier<String> s) {
		if (pL.isLoggable(Level.FINER)) {
			pL.finer(s.get());
		}
	}

	public static void warningPrint(Supplier<String> s) {
		if (pL.isLoggable(Level.WARNING)) {
			pL.warning(s.get());
		}
	}
	
	public static void errorPrint(Supplier<String> s) {
		if (pL.isLoggable(Level.SEVERE)) {
			pL.severe(s.get());
		}
	}

	public static String position(int line, int col, String path) {
		return colorify(path + ":" + "(" + line + "," + col + ")", lineNumColor);
	}

	public static String position(int line, int col) {
		return colorify("(" + line + "," + col + ")", lineNumColor);
	}

}
