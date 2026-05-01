package com.babai.wml.parser;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import com.babai.wml.tokenizer.Token;
import com.babai.wml.utils.AIGenerated;

import static com.babai.wml.utils.Colors.*;
import static com.babai.wml.utils.LogUtils.*;
import static com.babai.wml.cli.ANSIFormatter.colorify;
import static com.babai.wml.parser.ParseUtils.skip;
import static com.babai.wml.tokenizer.Tokenizer.tokenize;

public class Parser {
	private List<String> tagStack = new ArrayList<>();
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
					tagStack.add(tagName);
				}
			} else if (tagName.startsWith("/")) {
				// end tag
				tagName = tagName.substring(1, tagName.length());
				if (tagStack.isEmpty()) {
					errorPrint("End tag without matching start tag.");
				} else if (tagStack.getLast().equals(tagName)) {
					debugPrint("Read Tag: " + colorify("[" + tagName + "]", tagColor));
					tagStack.removeLast();
				} else {
					errorPrint("Wrong end tag " + colorify(tagName, RED)
					+ " found for tag "
					+ colorify("[" + tagStack.getLast() + "]", tagColor));
				}
				// needs better handling
			} else if (!NOT_TAG_PATTERN.matcher(tagName).find()) {
				tagStack.add(tagName);
			}
		}
		case WHITESPACE, EOL, COMMENT, QUOTED, ANGLE_QUOTED -> {} //ignore
		case MACRO -> warningPrint("Parser: Unexpanded macro {" + t.content() + "}, skipping");
		default -> warningPrint("Parser: Unexpected token " + t.content() + ", skipping");
		}
	}

	@AIGenerated
	public static boolean queryMatch(String queryStr, List<String> tagStack, String key) {
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
