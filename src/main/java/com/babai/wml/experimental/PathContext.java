package com.babai.wml.experimental;

import java.nio.file.Path;
import java.util.List;

import com.babai.wml.utils.FS;

public record PathContext(Path dataPath, Path userDataPath, List<Path> binaryPaths) {
	public static final PathContext EMPTY_CONTEXT =
		new PathContext(Path.of("."), Path.of("."), List.of());
	
	public Path resolve(String pathToResolve, Path currentPath) {
		return FS.resolve(pathToResolve, binaryPaths, currentPath, dataPath, userDataPath);
	}
}
