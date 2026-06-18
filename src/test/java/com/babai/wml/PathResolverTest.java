package com.babai.wml;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.babai.wml.parser.PathContext;
import com.babai.wml.utils.AIGenerated;
import com.babai.wml.utils.FS;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashSet;

@AIGenerated
class PathResolverTest {
	// note: does not check for actual file existance!
	// ideas: fake filesystem library to mock file existance.
	
	private static Path tempRoot;
	private static Path currentPath;
	private static Path dataPath;
	private static Path userDataPath;
	private static HashSet<Path> binaryPaths = new HashSet<>();
	private static PathContext ctxt;

	@BeforeAll
	static void setupDirectories() throws IOException {
		// Create a temporary root directory
		tempRoot = Files.createTempDirectory("wml_test");

		// Create subdirectories for testing
		currentPath = Files.createDirectories(tempRoot.resolve("game/assets"));
		dataPath = tempRoot;
		userDataPath = Files.createDirectories(tempRoot.resolve("user"));
		
		binaryPaths.add(Path.of("data/add-ons/myaddon"));
		
		// Create fake files
		Files.createFile(currentPath.resolve("map1.cfg"));
		Files.createDirectories(dataPath.resolve("images"));
		Files.createDirectories(dataPath.resolve("images/scenary"));
		Files.createFile(dataPath.resolve("images/scenary/altar.png"));
		Files.createDirectories(dataPath.resolve("music"));
		Files.createFile(dataPath.resolve("music/battle.ogg"));
		
		ctxt = new PathContext(dataPath, userDataPath);

		// Optional: mark for deletion on JVM exit
		tempRoot.toFile().deleteOnExit();
	}

	@Test
	void testResolveWithDotPrefix_CurrentIsDirectory() {
		Path result = ctxt.resolve("./scenary/altar.png", currentPath, binaryPaths);
		assertEquals(tempRoot.resolve("game/assets/scenary/altar.png"), result);
	}

	@Test
	void testResolveWithDotPrefix_CurrentIsFile() {
		Path result = ctxt.resolve("./scenary/altar.png", currentPath, binaryPaths);
		assertEquals(tempRoot.resolve("game/assets/scenary/altar.png"), result);
	}

	@Test
	void testResolveWithTildePrefix() {
		Path result = ctxt.resolve("~/add-ons/pack1", currentPath, binaryPaths);
		assertEquals(tempRoot.resolve("user/data/add-ons/pack1"), result);
	}
	
	@Test
	void testResolveWithFullPath() {
		Path result = FS.resolveAsset("images/scenary/altar.png", currentPath, binaryPaths, dataPath, userDataPath);
		assertEquals(tempRoot.resolve("images/scenary/altar.png"), result);
	}

	@Test
	void testResolveImageExtension() {
		Path result = FS.resolveAsset("scenary/altar.png", currentPath, binaryPaths, dataPath, userDataPath);
		assertEquals(tempRoot.resolve("images/scenary/altar.png"), result);
	}

	@Test
	void testResolveAudioExtension_ogg() {
		Path result = FS.resolveAsset("battle.ogg", currentPath, binaryPaths, dataPath, userDataPath);
		assertEquals(tempRoot.resolve("music/battle.ogg"), result);
	}
	
	@AfterAll
	static void cleanup() throws IOException {
		// Clean up temporary files (optional)
		Files.walk(tempRoot)
			.map(Path::toFile)
			.forEach(f -> f.delete());
	}
}

