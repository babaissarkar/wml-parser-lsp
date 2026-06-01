package com.babai.wml.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import com.babai.wml.query.WMLQuery;
import com.babai.wml.tokenizer.Token;

import static com.babai.wml.utils.Colors.*;
import static com.babai.wml.utils.LogUtils.*;
import static com.babai.wml.cli.ANSIFormatter.colorify;

import static com.babai.wml.tokenizer.ParserTokenizer.addEvent;
import static com.babai.wml.tokenizer.ParserTokenizer.tokenize;

public class Parser {
	private List<String> tagStack = new ArrayList<>();
	private HashMap<WMLQuery, List<Consumer<String>>> queryLambdas = new HashMap<>();

	public void addQuery(String query, Consumer<String> queryLambda) {
		queryLambdas.computeIfAbsent(WMLQuery.of(query), k -> new ArrayList<>()).add(queryLambda);
	}

	public void parse(String text) throws IOException {
		var key = new StringBuilder();
		var val = new StringBuilder();
		addEvent((kind, buff) -> parseToken(buff.toString(), kind, key, val));
		tokenize(text.toCharArray());
	}

	private void parseToken(String contents, Token.Kind kind, StringBuilder key, StringBuilder val) {
		switch (kind) {
		case TEXT -> key.append(contents);
		
		case VAL -> val.append(contents);
		
		case EOL -> {
			if (!key.isEmpty() && !val.isEmpty()) {
				// Check against queries
				final String keyStr = key.toString();
				final String valStr = val.toString();
				queryLambdas.forEach((q, actions) -> {
					if (q.match(tagStack, keyStr)) {
						for (var lambda : actions) {
							lambda.accept(valStr);
						}
					}
				});
			}
			
			key.setLength(0);
			val.setLength(0);
		}
		
		case TAG_START -> {
			if (isTag(contents)) {
				tagStack.add(contents);
			}
		}
		
		case TAG_END -> {
			if (tagStack.isEmpty()) {
				errorPrint(() -> "End tag without matching start tag.");
			} else if (tagStack.getLast().equals(contents)) {
				tagStack.removeLast();
			} else {
				errorPrint(() -> "Wrong end tag " + colorify(contents, RED)
				+ " found for tag "
				+ colorify("[" + tagStack.getLast() + "]", tagColor));
			}
		}
		case WHITESPACE, COMMENT, QUOTED, ANGLE_QUOTED -> {} //ignore
		case MACRO -> warningPrint(() -> "Unexpanded macro during parse, {" + contents + "}, skipping");
		default -> {} //TODO
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
