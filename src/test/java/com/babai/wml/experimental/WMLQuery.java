package com.babai.wml.experimental;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Stack;

class WMLQueryTest {

    Stack<String> stack(String... tags) {
        Stack<String> s = new Stack<>();
        // push outermost first so get(0) = innermost
        for (int i = tags.length - 1; i >= 0; i--) s.push(tags[i]);
        return s;
    }

    // --- full tag path match (no key) ---

    @Test
    void exactPathMatch() {
        // query: top/mid/bot, stack innermost-first: [bot, mid, top]
        assertTrue(Parser.queryMatch("top/mid/bot", stack("bot", "mid", "top"), ""));
    }

    @Test
    void partialPathNoMatch() {
        // stack is only mid/top, bot not entered yet
        assertFalse(Parser.queryMatch("top/mid/bot", stack("mid", "top"), ""));
    }

    @Test
    void wrongTagNoMatch() {
        assertFalse(Parser.queryMatch("top/mid/bot", stack("wrong", "mid", "top"), ""));
    }

    // --- key match ---

    @Test
    void keyMatch() {
        // query: top/mid/id — path matches, key line matches
        assertTrue(Parser.queryMatch("top/mid/id", stack("mid", "top"), "id=elvish-fighter"));
    }

    @Test
    void keyMatchWrongValue_stillMatches() {
        // queryMatch only checks key name, not value
        assertTrue(Parser.queryMatch("top/mid/id", stack("mid", "top"), "id=something-else"));
    }

    @Test
    void keyMismatch() {
        assertFalse(Parser.queryMatch("top/mid/id", stack("mid", "top"), "name=foo"));
    }

    @Test
    void keyMatchWrongPath() {
        assertFalse(Parser.queryMatch("top/mid/id", stack("wrong", "top"), "id=foo"));
    }

    // --- edge cases ---

    @Test
    void stackTooShallow() {
        assertFalse(Parser.queryMatch("top/mid/bot", stack("top"), ""));
    }

    @Test
    void singleTagQuery() {
        assertTrue(Parser.queryMatch("top", stack("top"), ""));
    }

    @Test
    void singleKeyQuery() {
        assertTrue(Parser.queryMatch("top/id", stack("top"), "id=foo"));
    }

    @Test
    void emptyKeyLine() {
        assertFalse(Parser.queryMatch("top/mid/id", stack("mid", "top"), ""));
    }
}
