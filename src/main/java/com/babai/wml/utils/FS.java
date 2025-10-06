package com.babai.wml.utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class FS {
	private FS() {
	}
	
	public static String getAssetType(String path) {
		// TODO: add sounds folder if needed
		var extFolders = Map.of(
			List.of(".png", ".jpg", ".webp"),"images",
			List.of(".ogg"), "music",
			List.of(".wav"), "sounds"
		);
		var coreFolders = extFolders.values().stream().collect(Collectors.toSet());
		for (var entry : extFolders.entrySet()) {
			if (entry.getKey().stream().anyMatch(path::endsWith)) {
				return entry.getValue();
			}
		}
		return "";
	}

	/** Convert Wesnoth path string to NIO Path object */
	public static Path resolve(String pathStr, Path currentPath, Path dataPath, Path userDataPath) {
		Path parent = null;

		if (pathStr.startsWith(".")) {
			parent = Files.isDirectory(currentPath) ? currentPath : currentPath.getParent();
		} else {
			String assetType = getAssetType(pathStr); 
			pathStr = Path.of(assetType, pathStr).toString();
			if (pathStr.startsWith("~")) {
				// Supports both ~add-ons and ~/add-ons
				pathStr = pathStr.replaceFirst("^~/?", "");
				parent = userDataPath;
			} else {
				// E.g.: scenary/alter.png
				if (!assetType.isEmpty()) {
					pathStr = Path.of("core", pathStr).toString();
				}
				parent = dataPath;
			}
		}

		return parent != null ? parent.resolve(pathStr).normalize() : parent;
	}
}
