package com.babai.wml.experimental;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static com.babai.wml.utils.Colors.*;
import static com.babai.wml.utils.ANSIFormatter.colorify;
import static com.babai.wml.experimental.Tokenizer.tokenize;
import static com.babai.wml.experimental.LogUtils.*;
import static com.babai.wml.experimental.ParseUtils.skip;

public class Parser {
	private Stack<String> tagStack = new Stack<>();
	private HashMap<String, List<Consumer<String>>> queryLambdas = new LinkedHashMap<>();
	private static final Pattern NOT_TAG_PATTERN = Pattern.compile("[^a-z_\\d]|^\\d");
	
	public void addQuery(String query, Consumer<String> queryLambda) {
		queryLambdas.computeIfAbsent(query, k -> new ArrayList<>()).add(queryLambda);
	}
	
	public void parse(String text) throws IOException {
		var itor = tokenize(new StringReader(text)).listIterator();
		while (itor.hasNext()) {
			Token t = itor.next();
			parseToken(itor, t);
		}
	}

	private void parseToken(ListIterator<Token> itor, Token t) {
		switch (t.kind()) {
		case TEXT -> {
			String line = t.content().strip();
			for (var query : queryLambdas.entrySet()) {
				String[] parts = line.split("=", 2);
				if (queryMatch(query.getKey(), tagStack, parts[0].trim())) {
					String value = parts[1].trim();
					if (value.isEmpty()) {
						t = itor.next();
						if (t.kind() == Token.Kind.WHITESPACE) {
							skip(itor, Token.Kind.WHITESPACE);
						}
						
						itor.previous();
						t = itor.next();
						if (t.kind() != Token.Kind.EOL) {
							value = t.content().strip();
						} else {
							errorPrint("Invalid line: " + line);
							break;
						}
					}
					
					for (var lambda : query.getValue()) {
						lambda.accept(value);
					}
				}
			}
		}
		case TAG -> {
			String tagName = t.content().strip();
			if (tagName.startsWith("+")) {
				// appending tag, like [+units]
				tagName = tagName.substring(1, tagName.length());
				if (!NOT_TAG_PATTERN.matcher(tagName).find()) {
					tagStack.push(tagName);
				}
			} else if (tagName.startsWith("/")) {
				// end tag
				tagName = tagName.substring(1, tagName.length());
				if (tagStack.isEmpty()) {
					errorPrint("End tag without matching start tag.");
				} else if (tagStack.peek().equals(tagName)) {
					debugPrint("Read Tag: " + colorify("[" + tagName + "]", tagColor));
					tagStack.pop();
				} else {
					errorPrint("Wrong end tag " + colorify(tagName, RED)
					+ " found for tag "
					+ colorify("[" + tagStack.peek() + "]", tagColor));
				}
				// needs better handling
			} else if (!NOT_TAG_PATTERN.matcher(tagName).find()) {
				tagStack.push(tagName);
			}
		}
		case WHITESPACE, EOL, COMMENT, QUOTED, ANGLE_QUOTED -> {} //ignore
		case MACRO -> warningPrint("Parser: Unexpanded macro {" + t.content() + "}, skipping");
		default -> warningPrint("Parser: Unexpected token " + t.content() + ", skipping");
		}
	}

	public static boolean queryMatch(String queryStr, Stack<String> tagStack, String key) {
		String[] queryParts = queryStr.split("/");

		if (tagStack.size() < queryParts.length - 1) return false; // not deep enough

		int i;
		for (i = 0; i < queryParts.length; i++) {
			if (i == queryParts.length - 1) {
				// if stack has this level, it's a tag match
				// if stack is one short, it's a key match
				if (tagStack.size() > i) {
					return tagStack.get(i).equals(queryParts[i]);
				} else {
					return key.equals(queryParts[i]);
				}
			}
			if (!tagStack.get(i).equals(queryParts[i])) return false;
		}

		return true; // all tag parts matched, no key part
	}
}
