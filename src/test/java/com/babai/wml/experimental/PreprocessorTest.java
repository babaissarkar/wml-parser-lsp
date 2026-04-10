package com.babai.wml.experimental;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;

import com.babai.wml.core.Definition;

class PreprocessorTest {

	@Test
	void testDefineExpand() throws IOException {
		String defString = """
			#define MYMACRO2
			# This is some docs
			Something#enddef
			{MYMACRO2}""";
		var preproc = new Preprocessor(PathContext.EMPTY_CONTEXT);
		String str = preproc.preprocessContent(new StringReader(defString));
		assertFalse(str.isEmpty());
		assertEquals("Something", str);
	}

	@Test
	void testDefineArgsMetadata() throws IOException {
		String defString = """
			#define MYMACRO ARG1 ARG2
			# This is doc
			#
			# Doc para 2
			#arg DARG1
			default#endarg

			#arg DARG2
			default2#endarg

			#arg DARG3
			default3#endarg
			Something#enddef""";
		var preproc = new Preprocessor(PathContext.EMPTY_CONTEXT);
		preproc.preprocessContent(new StringReader(defString));
		var defines = preproc.getDefines();
		assertEquals(1, defines.rowCount());
		var rows = defines.getRows("Name", "MYMACRO");
		assertEquals(1, rows.size());
		var macroDefinition = (Definition) rows.get(0).getColumn("Definition").getValue();
		assertEquals(2, macroDefinition.getArgCount());
		assertEquals(3, macroDefinition.getDefArgCount());
		assertEquals("This is doc\n\nDoc para 2", macroDefinition.getDocs());
	}

	@Test
	void testDefineArgsExpansionDefaultAndOverride() throws IOException {
		String defString = """
			#define GREET WHO
			#arg PUNC
			!#endarg
			Hello {WHO}{PUNC}#enddef
			{GREET "World"}
			{GREET "Friend" PUNC="?"}""";
		var preproc = new Preprocessor(PathContext.EMPTY_CONTEXT);
		String str = preproc.preprocessContent(new StringReader(defString));
		assertEquals("Hello \"World\"!\nHello \"Friend\"?", str);
	}

	@Test
	void testDefineArgsKeywordQuotedValueWithWhitespace() throws IOException {
		String defString = """
			#define SAY WHO
			#arg NOTE
			none#endarg
			{WHO}:{NOTE}#enddef
			{SAY "Unit" NOTE="very good"}""";
		var preproc = new Preprocessor(PathContext.EMPTY_CONTEXT);
		String str = preproc.preprocessContent(new StringReader(defString));
		assertEquals("\"Unit\":very good", str);
	}

	@Test
	void testIfdefTrueBranch() throws IOException {
		String defString = """
			#define MYMACRO2
			Something#enddef
			#ifdef MYMACRO2
			{MYMACRO2}
			#else
			"Nodef"
			#endif""";
		var preproc = new Preprocessor(PathContext.EMPTY_CONTEXT);
		String str = preproc.preprocessContent(new StringReader(defString));
		assertFalse(str.isEmpty());
		assertEquals("Something\n", str);
	}

	@Test
	void testIfdefFalseBranchUsesElse() throws IOException {
		String defString = """
			#ifdef MISSING_MACRO
			bad
			#else
			good
			#endif""";
		var preproc = new Preprocessor(PathContext.EMPTY_CONTEXT);
		String str = preproc.preprocessContent(new StringReader(defString));
		assertEquals("good\n", str);
	}

	@Test
	void testIfndefTrueBranch() throws IOException {
		String defString = """
			#ifndef MISSING_MACRO
			ok
			#else
			nope
			#endif""";
		var preproc = new Preprocessor(PathContext.EMPTY_CONTEXT);
		String str = preproc.preprocessContent(new StringReader(defString));
		assertEquals("ok\n", str);
	}

	@Test
	void testUndefinedMacroFallsBackWithoutExpansion() throws IOException {
		String defString = """
			{DOES_NOT_EXIST}
			text""";
		var preproc = new Preprocessor(PathContext.EMPTY_CONTEXT);
		String str = preproc.preprocessContent(new StringReader(defString));
		assertTrue(str.startsWith("{DOES_NOT_EXIST}"));
		assertTrue(str.contains("text"));
	}

	@Test
	void testDeprecatedDirectiveMetadataParsed() throws IOException {
		String defString = """
			#define OLD_MACRO
			#deprecated 2 1.19.0 Use NEW_MACRO instead
			abc#enddef""";
		var preproc = new Preprocessor(PathContext.EMPTY_CONTEXT);
		preproc.preprocessContent(new StringReader(defString));
		var rows = preproc.getDefines().getRows("Name", "OLD_MACRO");
		assertEquals(1, rows.size());
		var macroDefinition = (Definition) rows.get(0).getColumn("Definition").getValue();
		assertTrue(macroDefinition.isDeprecated());
		assertEquals(2, macroDefinition.getDeprecationLevel());
		assertEquals("1.19.0", macroDefinition.getDeprecationRemovalVersion());
		assertEquals("Use NEW_MACRO instead", macroDefinition.getDeprecationMessage());
	}
}
