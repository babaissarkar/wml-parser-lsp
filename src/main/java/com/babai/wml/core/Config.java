package com.babai.wml.core;

import java.util.ArrayList;
import java.util.HashMap;

public class Config {
	private HashMap<String, ConfigAttributeBase> attributes;
	private ArrayList<Config> children;
	private String name;

	public Config(String name) {
		this.name = name;
		attributes = new HashMap<>();
		children = new ArrayList<>();
	}
	public String getName() {
		return this.name;
	}

	// @Nullable
	public ConfigAttributeBase getAttr(String attrName) {
		return attributes.get(attrName);
	}

	// @Nullable
	public Config getChild(int i) {
		if (i < children.size()) {
			return children.get(i);
		} else {
			return null;
		}
	}

	public void add(Config entry) {
		children.add(entry);
	}

	public <T> void add(String key, T value) {
		attributes.put(key, new ConfigAttribute<T>(key, value));
	}
	
	public String getID() {
		String unitTypeId = getAttr("id").stringValue();
		if (unitTypeId.startsWith("\"") && unitTypeId.endsWith("\"")) {
			unitTypeId = unitTypeId.substring(1, unitTypeId.length()-1);
		}
		return unitTypeId;
	}
	
	// TODO WIP error handling, nested tags
	public static Config read(String text) {
		Config cfg = null;
		String[] lines = text.split("\n+");
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i].strip();
			if (i == 0) {
				if (line.startsWith("[") && line.endsWith("]")) {
					line = line.substring(1, line.length()-1);
					if (line.startsWith("+")) {
						line = line.substring(1);
					}
					if (cfg == null) {
						cfg = new Config(line);
					}
				}
			} else if (line.contains("=")) {
				String[] key_val = line.split("=");
				if (key_val.length > 1) {
					cfg.add(key_val[0].strip(), key_val[1].strip());
				}
			}
		}
		return cfg;
	}

	public String write(int indentLevel) {
		var sb = new StringBuilder();
		int i = 0;

		// Start tag
		sb.append("\t".repeat(indentLevel));
		sb.append("[" + name + "]\n");

		// Attributes
		for (var attr : attributes.entrySet()) {
			sb.append(attr.getValue().write(indentLevel + 1));

			if (i < attributes.size()) {
				sb.append('\n');
				i++;
			}
		}

		// Subtags
		for (var child : children) {
			sb.append(child.write(indentLevel + 1));
			sb.append('\n');
		}

		// End tag
		sb.append("\t".repeat(indentLevel));
		sb.append("[/" + name + "]");

		return sb.toString();
	}

	public String toString() {
		return write(0);
	}
}
