package com.babai.wml.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.babai.wml.preprocessor.Definition;

public class MacroTable {
	Map<String, Definition> nameValMap;
	Map<String, Integer> nameLineNumMap;
	Map<String, String> nameUriMap;
	Map<String, Set<String>> uriNamelistMap;
	
	public MacroTable() {
		nameValMap = new HashMap<>();
		nameLineNumMap = new HashMap<>();
		nameUriMap = new HashMap<>();
		uriNamelistMap = new HashMap<>();
	}
	
	public MacroTable(MacroTable defines) {
		this();
		nameValMap.putAll(defines.nameValMap);
		nameLineNumMap.putAll(defines.nameLineNumMap);
		nameUriMap.putAll(defines.nameUriMap);
		uriNamelistMap.putAll(defines.uriNamelistMap);
	}

	public void addMacro(String name, Definition def, Integer linenum, String uri) {
		//TODO duplicate warning
		nameValMap.put(name, def);
		nameLineNumMap.put(name, linenum);
		nameUriMap.put(name, uri);
		uriNamelistMap.computeIfAbsent(uri, k -> new HashSet<>()).add(name);
	}
	
	public boolean hasMacro(String name) {
		return nameValMap.containsKey(name);
	}
	
	public Definition getMacro(String name) {
		return nameValMap.get(name);
	}
	
	public void removeMacro(String name) {
		nameValMap.remove(name);
		nameLineNumMap.remove(name);
		nameUriMap.remove(name);
	}
	
	public String getUri(String name) {
		return nameUriMap.get(name);
	}
	
	public int getLineNum(String name) {
		return nameLineNumMap.get(name);
	}
	
	public Map<String, Definition> macros() {
		return nameValMap;
	}
	
	public Set<String> macrosByUri(String uri) {
		return uriNamelistMap.get(uri);
	}

	public Map<String, Set<String>> uriMap() {
		return uriNamelistMap;
	}
	
	public int size() {
		return nameValMap.size();
	}
	
	@Override
	public String toString() {
		return macros().toString().replaceAll(", ", "\n");
	}
}
