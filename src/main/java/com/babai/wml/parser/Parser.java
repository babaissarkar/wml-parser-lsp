package com.babai.wml.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;

import com.babai.wml.query.WMLQuery;
import com.babai.wml.tokenizer.Token;

import static com.babai.wml.utils.Colors.*;
import static com.babai.wml.utils.LogUtils.*;
import static com.babai.wml.cli.ANSIFormatter.colorify;
import static com.babai.wml.tokenizer.Tokenizer.tokenize;
import static com.babai.wml.tokenizer.Token.Kind.*;
import static com.babai.wml.parser.ParseUtils.peek;

public class Parser {
	private List<String> tagStack = new ArrayList<>();
	private HashMap<String, List<Consumer<String>>> queryLambdas = new LinkedHashMap<>();

	public void addQuery(String query, Consumer<String> queryLambda) {
		queryLambdas.computeIfAbsent(query, k -> new ArrayList<>()).add(queryLambda);
	}

	public void parse(String text) throws IOException {
		var line = new StringBuilder();
		var itor = tokenize(text).listIterator();
		while (itor.hasNext()) {
			Token t = itor.next();
			parseToken(itor, t, line);
		}
	}

	private void parseToken(ListIterator<Token> itor, Token t, StringBuilder val) {
		val.setLength(0);
		
		switch (t.kind()) {
		case TEXT -> {
			String key = t.content();
			
			// rest of line is rhs value, as '=' is suppressed in tokenizer
			while (peek(itor).isKind(VAL, WHITESPACE, QUOTED, ANGLE_QUOTED, EQL)) {
				t = itor.next();
				if (t.isNotKind(EQL)) {
					val.append(t.content());
				}
			}
				
			// Check against queries
			for (var query : queryLambdas.entrySet()) {
				if (WMLQuery.match(tagStack, query.getKey(), key)) {
					for (var lambda : query.getValue()) {
						lambda.accept(val.toString());
					}
				}
			}
		}
		case TAG_START -> {
			String tagName = t.content();
			if (isTag(tagName)) {
				tagStack.add(tagName);
			}
		}
		case TAG_END -> {
			String tagName = t.content();
			if (tagStack.isEmpty()) {
				errorPrint(() -> "End tag without matching start tag.");
			} else if (tagStack.getLast().equals(tagName)) {
				tagStack.removeLast();
			} else {
				final String tmpTagName = tagName;
				errorPrint(() -> "Wrong end tag " + colorify(tmpTagName, RED)
				+ " found for tag "
				+ colorify("[" + tagStack.getLast() + "]", tagColor));
			}
		}
		case WHITESPACE, EOL, COMMENT, QUOTED, ANGLE_QUOTED -> {} //ignore
		case MACRO -> {
			final String tmpContent = t.content();
			warningPrint(() -> "Parser: Unexpanded macro {" + tmpContent + "}, skipping");
		}
		default -> {
			final String tmpContent = t.content();
			warningPrint(() -> "Parser: Unexpected token " + tmpContent + ", skipping");
		}
		}
	}
	
	private boolean isTag(String tagName) {
		if (tagName.length() == 0) return false;
		char c;
		for (int i = 0; i < tagName.length(); i++) {
			c = tagName.charAt(i);
			if (i == 0) {
				if (!(Character.isLetter(c))) return false;
			} else {
				if (!(Character.isLetterOrDigit(c) || c == '_')) return false;
			}
		}
		return true;
	}
}
