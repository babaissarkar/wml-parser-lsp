package com.babai.wml.experimental;

import java.nio.file.Path;
import java.util.List;

import com.babai.wml.utils.FS;

public record PathContext(Path currentPath, Path dataPath, Path userDataPath, List<Path> binaryPaths) {
	public static final PathContext EMPTY_CONTEXT =
		new PathContext(Path.of("."), Path.of("."), Path.of("."), List.of());

	public PathContext withCurrentPath(Path newCurrentPath) {
		return new PathContext(newCurrentPath, dataPath, userDataPath, binaryPaths);
	}
	
	public Path resolve(String path) {
		return FS.resolve(path, binaryPaths, currentPath, dataPath, userDataPath);
	}
}
