package com.babai.wml.experimental;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.*;

import com.babai.wml.query.Query;
import com.babai.wml.utils.AIGenerated;

import static org.junit.jupiter.api.Assertions.*;

@AIGenerated
class WMLQueryTest {

	List<String> stack(String... tags) {
		List<String> s = new ArrayList<>();
		// push outermost first so get(0) = innermost
		for (int i = tags.length - 1; i >= 0; i--) s.add(tags[i]);
		return s;
	}

	// --- full tag path match (no key) ---

	@Test
	void exactPathMatch() {
		// query: top/mid/bot, stack innermost-first: [bot, mid, top]
		assertTrue(Query.match("top/mid/bot", stack("bot", "mid", "top"), ""));
	}

	@Test
	void partialPathNoMatch() {
		// stack is only mid/top, bot not entered yet
		assertFalse(Query.match("top/mid/bot", stack("mid", "top"), ""));
	}

	@Test
	void wrongTagNoMatch() {
		assertFalse(Query.match("top/mid/bot", stack("wrong", "mid", "top"), ""));
	}

	// --- key match ---

	@Test
	void keyMatch() {
		// query: top/mid/id — path matches, key line matches
		assertTrue(Query.match("top/mid/id", stack("mid", "top"), "id"));
	}

	@Test
	void keyMismatch() {
		assertFalse(Query.match("top/mid/id", stack("mid", "top"), "name=foo"));
	}

	@Test
	void keyMatchWrongPath() {
		assertFalse(Query.match("top/mid/id", stack("wrong", "top"), "id=foo"));
	}
	
	// --- anywhere match ---
	
	@Test
	void anyWhereMatch() {
		assertTrue(Query.match("//a/b", stack("b", "a", "c"), ""));
	}
	
	@Test
	void anyWhereMatchFalseRecovery() {
		assertTrue(Query.match("//a/b", stack("d", "b", "a", "a", "e", "c"), ""));
	}

	// --- edge cases ---

	@Test
	void stackTooShallow() {
		assertFalse(Query.match("top/mid/bot", stack("top"), ""));
	}

	@Test
	void singleTagQuery() {
		assertTrue(Query.match("top", stack("top"), ""));
	}

	@Test
	void singleKeyQuery() {
		assertTrue(Query.match("top/id", stack("top"), "id"));
	}

	@Test
	void emptyKeyLine() {
		assertFalse(Query.match("top/mid/id", stack("mid", "top"), ""));
	}
}
