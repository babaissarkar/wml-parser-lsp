package com.babai.wml.parser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import com.babai.wml.utils.FS;

public record PathContext(Path dataPath, Path userDataPath) {
	public static final PathContext EMPTY_CONTEXT =
		new PathContext(Path.of("."), Path.of("."));
	
	public Path resolve(String pathToResolve, Path currentPath, Set<Path> binaryPaths) {
		if (pathToResolve.charAt(0) == '.' || pathToResolve.charAt(0) == '~')
			return resolveFileInclusion(pathToResolve, currentPath);
		return FS.resolveAsset(pathToResolve, currentPath, binaryPaths, dataPath, userDataPath);
	}

	public Path resolveFileInclusion(String pathStr, Path currentPath) {
		Path parent = null;
		if (pathStr.charAt(0) == '.') {
			parent = Files.isDirectory(currentPath) ? currentPath : currentPath.getParent();
		} else {
			if (pathStr.charAt(0) == '~') {
				// Supports both ~add-ons and ~/add-ons
				pathStr = pathStr.replaceFirst("^~/?", "");
				parent = userDataPath.resolve("data");
			} else {
				parent = dataPath.resolve("data");
			}
		}
		// parent shouldn't be null at this point
		return parent.resolve(pathStr).normalize().toAbsolutePath();
	}
	
	public String relativize(Path target) {
		Path afterData = FS.relativizeIfUnder(dataPath.resolve("data"), target);
		boolean dataRelativized = !afterData.equals(target);

		Path afterUser = FS.relativizeIfUnder(userDataPath.resolve("data"), afterData);
		boolean userRelativized = !afterUser.equals(afterData);

		String out = afterUser.toString();
		if (!dataRelativized && userRelativized) {
			out = "~" + out;
		} else if (dataRelativized) {
			out = afterData.toString();
		}
		return out;
	}

}
