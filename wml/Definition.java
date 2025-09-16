package wml;

import java.util.HashMap;
import java.util.Vector;

public class Definition {
	private String name;
	private String value;
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

	/** Expand the macro, substituting any given args */
	public String expand(Vector<String> values, HashMap<String, String> keyVals) {
		String unparsed = this.value;
		if (values.size() != args.size()) {
			throw new IllegalArgumentException(
				"Wrong number of arguments supplied to macro '" + this.name + "'. " +
				"Expected " + values.size() + " but got " + args.size() +
				". Supplied args: " + args
			);
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
		return expand(values, new HashMap<String, String>());
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("#define");
		sb.append(name);
		for (var arg : args) {
			sb.append(" " + arg);
		}
		sb.append("\n");
		sb.append(value);
		sb.append("#enddef");
		return sb.toString();
	}

}
