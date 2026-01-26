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
			System.out.println("Toks: " + toks);
			assertEquals(toks.size(), 4);
			assertEquals(toks.get(0).getContent(), "Hello");
			assertEquals(toks.get(1).getContent(), " ");
			assertEquals(toks.get(2).getContent(), "#Comment");
			assertEquals(toks.get(3).getContent(), "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
