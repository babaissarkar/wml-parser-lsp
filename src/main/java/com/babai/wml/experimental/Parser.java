package com.babai.wml.experimental;

import java.awt.Color;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.ListIterator;
import java.util.Stack;
import java.util.regex.Pattern;

import static com.babai.wml.utils.Colors.*;
import static com.babai.wml.utils.ANSIFormatter.colorify;
import static com.babai.wml.experimental.Tokenizer.tokenize;
import static com.babai.wml.experimental.LogUtils.*;
import static com.babai.wml.experimental.ParseUtils.skip;

public class Parser {
	private PathContext context = null;
	private Stack<String> tagStack = new Stack<>();
	private static final Pattern NOT_TAG_PATTERN = Pattern.compile("[^a-z_\\d]|^\\d");
	
	public Parser(PathContext context) {
		this.context = context;
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
			if (!tagStack.isEmpty() && tagStack.peek().equals("binary_path")) {
				if (line.startsWith("path=")) {
					String value = line.split("=", 2)[1];
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
						}
					}
					
					Path bpath = Path.of(value);
					context.binaryPaths().add(bpath);
					debugPrint("Binary Path found: " + bpath);
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
					errorPrint("Wrong end tag " + colorify(tagName, Color.RED)
					+ " found for tag "
					+ colorify("[" + tagStack.peek() + "]", tagColor));
				}
				// needs better handling
			} else if (!NOT_TAG_PATTERN.matcher(tagName).find()) {
				tagStack.push(tagName);
			}
		}
		case WHITESPACE, EOL, COMMENT, QUOTED, ANGLE_QUOTED -> {} //ignore
		case MACRO -> throw new IllegalArgumentException("Unexpected macro during parse: {" + t.content() + "}");
		default -> throw new IllegalArgumentException("Unexpected value: " + t.content());
		}
	}	
}
