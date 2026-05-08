package com.babai.wml.experimental;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.babai.wml.parser.ParseUtils;
import com.babai.wml.tokenizer.Token;
import com.babai.wml.tokenizer.Tokenizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

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
	
	@ParameterizedTest
	@MethodSource("substituteCases")
	void testSubstitute(String template, Map<String, String> subst, String expected) {
		assertEquals(expected, ParseUtils.substitute(template, subst));
	}

	static Stream<Arguments> substituteCases() {
		return Stream.of(
			// no braces - early return
			arguments("no braces", Map.of(), "no braces"),
			// single substitution
			arguments("{A}", Map.of("A", "val"), "val"),
			// multiple substitutions
			arguments("{A} and {B}", Map.of("A", "x", "B", "y"), "x and y"),
			// unknown key - emitted verbatim
			arguments("{UNKNOWN}", Map.of(), "{UNKNOWN}"),
			// mixed known and unknown
			arguments("{A} {UNKNOWN}", Map.of("A", "val"), "val {UNKNOWN}"),
			// text around macros
			arguments("prefix {A} suffix", Map.of("A", "mid"), "prefix mid suffix"),
			// quoted string passthrough + comment preservation
			arguments("\"hello\" {A} # comment", Map.of("A", "val"), "\"hello\" val # comment"),
			// empty subst map, no macros
			arguments("plain text", Map.of(), "plain text")
			);
	}

	@Test
	void testCommentSplit() {
		String text = "Hello #Comment\nLine2";
		try {
			List<Token> toks = Tokenizer.tokenize(text);
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
			List<Token> toks = Tokenizer.tokenize(text);
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
			List<Token> toks = Tokenizer.tokenize(text);
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
			List<Token> toks = Tokenizer.tokenize(text);
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
	void testSingleAngle() {
		String text = "<hello>";
		try {
			List<Token> toks = Tokenizer.tokenize(text);
			assertEquals(1, toks.size());
			assertEquals(Token.Kind.TEXT, toks.get(0).kind());
			assertEquals("<hello>", toks.get(0).content());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	void testUnbalancedAngle() {
		String text = "<hello";
		try {
			List<Token> toks = Tokenizer.tokenize(text);
			assertEquals(1, toks.size());
			assertEquals(Token.Kind.TEXT, toks.get(0).kind());
			assertEquals("<hello", toks.get(0).content());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	void testQuotedConcatenation() {
		String text = "\"Hello \" + \"Hello\"";
		try {
			List<Token> toks = Tokenizer.tokenize(text);
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
			List<Token> toks = Tokenizer.tokenize(text);
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
			List<Token> toks = Tokenizer.tokenize(text);
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
			List<Token> toks = Tokenizer.tokenize(text);
			System.out.println("Toks(chained concat): " + toks);

			assertEquals(1, toks.size());
			assertEquals("Journeyof aFrost Mage", toks.get(0).content());
			assertEquals(Token.Kind.QUOTED, toks.get(0).kind());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
