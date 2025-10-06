package com.babai.wml.utils;

import java.nio.file.Files;
import java.nio.file.Path;

public final class FS {
	private FS() {
	}

	/** Convert Wesnoth path string to NIO Path object */
	public static Path resolve(String pathStr, Path currentPath, Path dataPath, Path userDataPath) {
		Path p;
		if (pathStr.startsWith(".")) {
			if (Files.isDirectory(currentPath)) {
				p = currentPath.resolve(pathStr);
			} else {
				p = currentPath.getParent().resolve(pathStr);
			}
		} else if (pathStr.startsWith("~")) {
			// TODO should throw warning if userdatapath is null
			// Supports both ~add-ons and ~/add-ons
			String relpath = pathStr.substring(1);
			if (relpath.startsWith("/")) {
				relpath = relpath.substring(1);
			}
			p = userDataPath.resolve(relpath);
		} else {
			// TODO should throw warning if datapath is null
			p = dataPath.resolve(pathStr);
		}

		return p;
	}
}
