package com.babai.wml.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Collectors;

public class Definition {
	private String name, value, docs;
	private Vector<String> args = new Vector<>();
	private HashMap<String, String> defArgs = new HashMap<>();

	public Definition(String name, String value) {
		this.name = name;
		this.value = value;
	}

	public Definition(String name, String value, Vector<String> args) {
		this.name = name;
		this.value = value;
		this.args = args;
	}

	public Definition(String name, String value, Vector<String> args, HashMap<String, String> defArgs) {
		this.name = name;
		this.value = value;
		this.args = args;
		this.defArgs = defArgs;
	}

	public void addArg(String arg) {
		args.add(arg);
	}

	public void addDefArg(String key, String val) {
		defArgs.put(key, val);
	}

	public String getValue() {
		return this.value;
	}

	public int getParamCount() {
		return args.size();
	}

	public String getDocs() {
		return this.docs;
	}

	public void setDocs(String docs) {
		this.docs = docs;
	}

	/** Expand the macro, substituting any given args */
	public String expand(Vector<String> values, HashMap<String, String> keyVals) {
		String unparsed = this.value;
		if (values.size() != args.size()) {
			throw new IllegalArgumentException("Wrong number of arguments supplied to macro '" + name() + "'. "
					+ "Expected " + args.size() + " but got " + values.size() + ".");
		}

		int i = 0;
		for (var arg : args) {
			unparsed = unparsed.replace("{" + arg + "}", values.get(i));
			i++;
		}

		for (var entry : defArgs.entrySet()) {
			String val = keyVals.get(entry.getKey());
			if (val == null) {
				val = entry.getValue();
			}
			unparsed = unparsed.replace("{" + entry.getKey() + "}", val);
		}

		return unparsed;
	}

	public String expand(Vector<String> values) {
		return expand(values, new HashMap<>());
	}

	public String expand() {
		return expand(new Vector<>(), new HashMap<>());
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("#define ");
		sb.append(name);
		for (var arg : args) {
			sb.append(" " + arg);
		}
		sb.append("\n");
		sb.append(value);
		sb.append("#enddef");
		return sb.toString();
	}

	public String name() {
		String argsAsString = argsAsString(args, defArgs);
		return name + (!argsAsString.isEmpty() ? "[" + argsAsString + "]" : "");
	}

	public static String argsAsString(Vector<String> args, Map<String, String> defArgs) {
		var keyValsStrings = defArgs.entrySet().stream().map(Map.Entry::toString).collect(Collectors.toList());

		return String.join(", ", args) + (args.size() > 0 && !keyValsStrings.isEmpty() ? ", " : "")
				+ String.join(", ", keyValsStrings);
	}

}
