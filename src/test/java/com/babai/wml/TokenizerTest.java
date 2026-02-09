package com.babai.wml;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.babai.wml.experimental.Token;
import com.babai.wml.experimental.Tokenizer;

class TokenizerTest {

	@Test
	void testCommentSplit() {
		String text = "Hello #Comment\nLine2";
		try {
			List<Token> toks = Tokenizer.tokenize(new BufferedReader(new StringReader(text)));
			System.out.println("Toks(comment test): " + toks);
			assertEquals(5, toks.size());
			assertEquals("Hello", toks.get(0).getContent());
			assertEquals(" ", toks.get(1).getContent());
			assertEquals("Comment", toks.get(2).getContent());
			assertEquals("\n", toks.get(3).getContent());
			assertEquals("Line2", toks.get(4).getContent());
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
			assertEquals("key=", toks.get(0).getContent());
			assertEquals("value val\"ue2\nvalue3", toks.get(1).getContent());
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
			assertEquals("key=", toks.get(0).getContent());
			assertEquals("value val\"ue2\nvalue3", toks.get(1).getContent());
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
}
