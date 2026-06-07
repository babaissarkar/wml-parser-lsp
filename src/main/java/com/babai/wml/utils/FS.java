package com.babai.wml.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FS {
	private FS() {
	}

	public static String getAssetType(String path) {
		// TODO: add sounds folder if needed
		var extFolders = Map.of(
			List.of(".png", ".jpg", ".webp"),"images",
			List.of(".ogg"), "music",
			List.of(".wav"), "sounds",
			List.of(".map"), "maps"
		);
		for (var entry : extFolders.entrySet()) {
			if (entry.getKey().stream().anyMatch(path::endsWith)) {
				return entry.getValue();
			}
		}
		return "";
	}
	
	public static Path resolve(String pathStr, Path currentPath, Set<Path> binaryPaths, Path dataPath, Path userDataPath) {
		Path parent = null;

		if (pathStr.charAt(0) == '.') {
			parent = Files.isDirectory(currentPath) ? currentPath : currentPath.getParent();
		} else {
			if (pathStr.charAt(0) == '~') {
				// Supports both ~add-ons and ~/add-ons
				pathStr = pathStr.replaceFirst("^~/?", "");
				parent = userDataPath;
			} else {
				// E.g.: scenary/alter.png
				String assetType = getAssetType(pathStr);
				
				pathStr = Path.of(assetType, pathStr).toString();
				if (!assetType.isEmpty() && binaryPaths != null) {
					Path resolved;
					for (var bPath : binaryPaths) {
						parent = userDataPath.getParent().resolve(bPath).normalize();
						resolved = parent.resolve(pathStr).normalize();
						if (Files.exists(resolved)) {
							return resolved;
						} else {
							parent = dataPath.getParent().resolve(bPath).normalize();
							resolved = parent.resolve(pathStr).normalize();
							if (Files.exists(resolved)) {
								return resolved;
							}
						}
					}
					pathStr = Path.of("core", pathStr).toString();
				}
				parent = dataPath;
			}
		}

		return parent != null ? parent.resolve(pathStr).normalize() : null;
	}
	
	@AIGenerated
	public static Path relativizeIfUnder(Path b, Path a) {
		try {
			Path absA = a.toRealPath().normalize();
			Path absB = b.toRealPath().normalize();
			return absA.startsWith(absB) ? absB.relativize(absA) : a;
		} catch (IOException e) {
			// fallback: use absolute normalized paths without resolving symlinks
			Path absA = a.toAbsolutePath().normalize();
			Path absB = b.toAbsolutePath().normalize();
			return absA.startsWith(absB) ? absB.relativize(absA) : a;
		}
	}
}
