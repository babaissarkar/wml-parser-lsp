package com.babai.wml;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.babai.wml.core.Config;

class ConfigTest {
	@Test
	void testConfigRead() {
		String test = """
				[binary_path]
					path=my/test/path.png
				[/binary_path]
				""";
		Config cfg = Config.read(test);
		assertEquals("binary_path", cfg.getName());
		assertEquals("my/test/path.png", cfg.getAttr("path").stringValue());
	}
}
