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
		String text = "Hello #Comment\n";
		try {
			List<Token> toks = Tokenizer.tokenize(new BufferedReader(new StringReader(text)));
			System.out.println("Toks(comment test): " + toks);
			assertEquals(toks.size(), 4);
			assertEquals(toks.get(0).getContent(), "Hello");
			assertEquals(toks.get(1).getContent(), " ");
			assertEquals(toks.get(2).getContent(), "#Comment");
			assertEquals(toks.get(3).getContent(), "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	void testQuotedString() {
		String text = "key=\"value val\"\"ue2\nvalue3\"";
		try {
			List<Token> toks = Tokenizer.tokenize(new BufferedReader(new StringReader(text)));
			System.out.println("Toks(quoted test): " + toks);
			assertEquals(toks.size(), 1);
			// checks 1. "" -> " collapse, preservation of whitespace
			assertEquals(toks.get(0).getContent(), "key=\"value val\"ue2\nvalue3\"");
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
}
