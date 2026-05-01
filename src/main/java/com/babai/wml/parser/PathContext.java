package com.babai.wml.parser;

import java.nio.file.Path;
import java.util.HashSet;
import com.babai.wml.utils.FS;

public record PathContext(Path dataPath, Path userDataPath, HashSet<Path> binaryPaths) {
	public static final PathContext EMPTY_CONTEXT =
		new PathContext(Path.of("."), Path.of("."), new HashSet<Path>());
	
	public Path resolve(String pathToResolve, Path currentPath) {
		return FS.resolve(pathToResolve, currentPath, binaryPaths(), dataPath(), userDataPath());
	}
}
