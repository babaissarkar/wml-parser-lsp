package com.babai.wml.utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Vector;

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

	/** Convert Wesnoth path string to NIO Path object */
	public static Path resolve(String pathStr, Vector<Path> binaryPaths, Path currentPath, Path dataPath, Path userDataPath) {
		Path parent = null;

		if (pathStr.startsWith(".")) {
			parent = Files.isDirectory(currentPath) ? currentPath : currentPath.getParent();
		} else {
			if (pathStr.startsWith("~")) {
				// Supports both ~add-ons and ~/add-ons
				pathStr = pathStr.replaceFirst("^~/?", "");
				parent = userDataPath;
			} else {
				// E.g.: scenary/alter.png
				String assetType = getAssetType(pathStr); 
				pathStr = Path.of(assetType, pathStr).toString();
				if (!assetType.isEmpty()) {
					for (var bPath : binaryPaths) {
						parent = userDataPath.resolve(Path.of("../", bPath.toString()));
						if (Files.exists(parent.resolve(pathStr))) {
							return parent.resolve(pathStr).normalize();
						} else {
							parent = dataPath.resolve(Path.of("../", bPath.toString()));
							if (Files.exists(parent.resolve(pathStr))) {
								return parent.resolve(pathStr).normalize();
							}
						}
					}
					pathStr = Path.of("core", pathStr).toString();
				}
				parent = dataPath;
			}
		}

		return parent != null ? parent.resolve(pathStr).normalize() : parent;
	}
}
