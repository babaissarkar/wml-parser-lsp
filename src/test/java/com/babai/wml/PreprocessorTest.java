package com.babai.wml;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringReader;

import com.babai.wml.core.Definition;
import com.babai.wml.experimental.PathContext;
import com.babai.wml.experimental.Preprocessor;

class PreprocessorTest {
	
	@Test
	void testDefineExpand() {
		String defString = """
			#define MYMACRO2
			Something#enddef
			{MYMACRO2}""";
		try {
			var preproc = new Preprocessor(PathContext.EMPTY_CONTEXT);
			String str = preproc.preprocess(new StringReader(defString));
			System.out.println(str);
			assertEquals(true, !str.isEmpty());
			assertEquals("\nSomething", str);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	void testDefineArgs() {
		String defString = """
			#define MYMACRO ARG1 ARG2
			#arg DARG1
			default#endarg
			
			#arg DARG2
			default2#endarg
			
			#arg DARG3
			default3#endarg
			Something#enddef""";
		try {
			var preproc = new Preprocessor(PathContext.EMPTY_CONTEXT);
			preproc.preprocess(new StringReader(defString));
			var defines = preproc.getDefines();
			assertEquals(1, defines.rowCount()); // Only 1 macro defined
			var rows = defines.getRows("Name", "MYMACRO");
			assertEquals(1, rows.size()); // Can be retrieved
			var macroDefinition = (Definition) rows.get(0).getColumn("Definition").getValue();
			assertEquals(2, macroDefinition.getArgCount()); // Has 2 positional args
			assertEquals(3, macroDefinition.getDefArgCount()); // Has 3 default args
			System.out.println(macroDefinition);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
