package com.babai.wml;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.babai.wml.utils.AIGenerated;
import com.babai.wml.utils.FS;

import java.io.IOException;
import java.nio.file.*;
import java.util.Vector;

@AIGenerated
class PathResolverTest {
	// note: does not check for actual file existance!
	// ideas: fake filesystem library to mock file existance.
	
	private static Path tempRoot;
	private static Path currentPath;
	private static Path dataPath;
	private static Path userDataPath;
	private static Vector<Path> binaryPaths = new Vector<>();

	@BeforeAll
	static void setupDirectories() throws IOException {
		// Create a temporary root directory
		tempRoot = Files.createTempDirectory("wml_test");

		// Create subdirectories for testing
		currentPath = Files.createDirectories(tempRoot.resolve("game/maps"));
		dataPath = Files.createDirectories(tempRoot.resolve("data"));
		userDataPath = Files.createDirectories(tempRoot.resolve("user/data"));
		
		// Create fake files
		Files.createFile(currentPath.resolve("map1.cfg"));
		Files.createDirectories(dataPath.resolve("units"));
		Files.createFile(dataPath.resolve("units/elf.cfg"));
		Files.createDirectories(dataPath.resolve("images"));
		Files.createDirectories(dataPath.resolve("images/scenary"));
		Files.createFile(dataPath.resolve("images/scenary/altar.png"));
		Files.createDirectories(dataPath.resolve("music"));
		Files.createFile(dataPath.resolve("music/battle.ogg"));

		// Optional: mark for deletion on JVM exit
		tempRoot.toFile().deleteOnExit();
	}

	@Test
	void testResolveWithDotPrefix_CurrentIsDirectory() {
		Path result = FS.resolve("./scenary/altar.png", binaryPaths, currentPath, dataPath, userDataPath);
		assertEquals(tempRoot.resolve("game/maps/scenary/altar.png"), result);
	}

	@Test
	void testResolveWithDotPrefix_CurrentIsFile() {
		Path result = FS.resolve("./scenary/altar.png", binaryPaths, currentPath, dataPath, userDataPath);
		assertEquals(tempRoot.resolve("game/maps/scenary/altar.png"), result);
	}

	@Test
	void testResolveWithTildePrefix() {
		Path result = FS.resolve("~/add-ons/pack1", binaryPaths, currentPath, dataPath, userDataPath);
		assertEquals(tempRoot.resolve("user/data/add-ons/pack1"), result);
	}

	@Test
	void testResolveWithDataPath() {
		Path result = FS.resolve("units/elf.cfg", binaryPaths, currentPath, dataPath, userDataPath);
		assertEquals(tempRoot.resolve("data/units/elf.cfg"), result);
	}

	@Test
	void testResolveImageExtension() {
		Path result = FS.resolve("scenary/altar.png", binaryPaths, currentPath, dataPath, userDataPath);
		assertEquals(tempRoot.resolve("data/core/images/scenary/altar.png"), result);
	}

	@Test
	void testResolveAudioExtension_ogg() {
		Path result = FS.resolve("battle.ogg", binaryPaths, currentPath, dataPath, userDataPath);
		assertEquals(tempRoot.resolve("data/core/music/battle.ogg"), result);
	}
	
	@AfterAll
	static void cleanup() throws IOException {
		// Clean up temporary files (optional)
		Files.walk(tempRoot)
			.map(Path::toFile)
			.forEach(f -> f.delete());
	}
}

