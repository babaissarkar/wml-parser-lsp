package com.babai.wml.experimental;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.babai.wml.parser.ParseUtils;
import com.babai.wml.tokenizer.Token;
import com.babai.wml.tokenizer.Tokenizer;

class TokenizerTest {
	@Test
	void testParenQuotedSplit() {
		String text = "Hello\n    (How\n are) you \"Konrad the Second\"";
		var parts = ParseUtils.splitQuoted(text);
		System.out.println("Toks(quoted split): " + parts);
		assertEquals(4, parts.size());
		assertEquals("Hello", parts.get(0));
		assertEquals("How\n are", parts.get(1));
		assertEquals("you", parts.get(2));
		assertEquals("\"Konrad the Second\"", parts.get(3));
	}

	@Test
	void testCommentSplit() {
		String text = "Hello #Comment\nLine2";
		try {
			List<Token> toks = Tokenizer.tokenize(new BufferedReader(new StringReader(text)));
			System.out.println("Toks(comment test): " + toks);
			assertEquals(5, toks.size());
			assertEquals("Hello", toks.get(0).content());
			assertEquals(" ", toks.get(1).content());
			assertEquals("Comment", toks.get(2).content());
			assertEquals("\n", toks.get(3).content());
			assertEquals("Line2", toks.get(4).content());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	void testQuotedString() {
		String text = "key=\"value val\"\"ue2\nvalue3\"";
		try {
			List<Token> toks = Tokenizer.tokenize(new StringReader(text));
			System.out.println("Toks(quoted test): " + toks);
			assertEquals(2, toks.size());
			// checks "" -> " collapse, preservation of whitespace
			assertEquals("key=", toks.get(0).content());
			assertEquals("value val\"ue2\nvalue3", toks.get(1).content());
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	@Test
	void testAngledQuotedString() {
		String text = "key=<<value val\"ue2\nvalue3>>";
		try {
			List<Token> toks = Tokenizer.tokenize(new StringReader(text));
			System.out.println("Toks(angle quote test): " + toks);
			assertEquals(2, toks.size());
			// checks 1. "" -> " collapse, preservation of whitespace
			assertEquals("key=", toks.get(0).content());
			assertEquals("value val\"ue2\nvalue3", toks.get(1).content());
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	@Test
	void testMacroString() {
		String text = "key={MYMACRO ARG1 ARG2 ARG3=\"def\"}";
		try {
			List<Token> toks = Tokenizer.tokenize(new StringReader(text));
			System.out.println("Toks(angle quote test): " + toks);
			assertEquals(2, toks.size());
			// checks 1. "" -> " collapse, preservation of whitespace
			assertEquals("key=", toks.get(0).content());
			assertEquals("MYMACRO ARG1 ARG2 ARG3=\"def\"", toks.get(1).content());
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	@Test
	void testQuotedConcatenation() {
		String text = "\"Hello \" + \"Hello\"";
		try {
			List<Token> toks = Tokenizer.tokenize(new StringReader(text));
			System.out.println("Toks(quoted concat): " + toks);

			assertEquals(1, toks.size());
			assertEquals("Hello Hello", toks.get(0).content());
			assertEquals(Token.Kind.QUOTED, toks.get(0).kind());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	void testUnquotedConcatenation() {
		String text = "foo + bar";
		try {
			List<Token> toks = Tokenizer.tokenize(new StringReader(text));
			System.out.println("Toks(unquoted concat): " + toks);

			assertEquals(1, toks.size());
			assertEquals("foo bar", toks.get(0).content());
			assertEquals(Token.Kind.TEXT, toks.get(0).kind());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	void testMixedConcatenation() {
		String text = "\"Hello\" + world";
		try {
			List<Token> toks = Tokenizer.tokenize(new StringReader(text));
			System.out.println("Toks(mixed concat): " + toks);

			assertEquals(1, toks.size());
			assertEquals("Helloworld", toks.get(0).content());
			assertEquals(Token.Kind.QUOTED, toks.get(0).kind());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	void testChainedConcatenation() {
		String text = "\"Journey\" + of + a + \"Frost Mage\"";
		try {
			List<Token> toks = Tokenizer.tokenize(new StringReader(text));
			System.out.println("Toks(chained concat): " + toks);

			assertEquals(1, toks.size());
			assertEquals("Journeyof aFrost Mage", toks.get(0).content());
			assertEquals(Token.Kind.QUOTED, toks.get(0).kind());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
