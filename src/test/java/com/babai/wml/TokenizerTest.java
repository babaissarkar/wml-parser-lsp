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
		String text = "Hello #Comment";
		try {
			List<Token> toks = Tokenizer.tokenize(new BufferedReader(new StringReader(text)));
			assertEquals(toks.size(), 2);
			assertEquals(toks.get(0).getContent(), "Hello ");
			assertEquals(toks.get(1).getContent(), "#Comment");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
