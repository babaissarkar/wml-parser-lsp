package com.babai.wml.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WMLQuery {	
	private List<String> qparts;
	private boolean absolute;
	
	private WMLQuery(List<String> parts, boolean absolute) {
		this.qparts = parts;
		this.absolute = absolute;
	}
	
	public boolean match(List<String> tagStack, String key) {
		List<String> stack = new ArrayList<>(tagStack);
		if (!key.isEmpty()) {
			stack.add(key);
		}
		
		if (!absolute) {
			return Collections.indexOfSubList(stack, qparts) != -1;
		} else {
			return Collections.indexOfSubList(stack, qparts) == 0;
		}
//		return false;
	}
	
	public static WMLQuery of(String queryStr) {
		int start = 0;
		boolean absolute = true;

		if (queryStr.length() >= 2 && queryStr.charAt(0) == '/' && queryStr.charAt(1) == '/') {
			absolute = false;
			start = 2;
		}

		// trim leading whitespace
		while (start < queryStr.length() && Character.isWhitespace(queryStr.charAt(start))) start++;

		// trim trailing whitespace
		int end = queryStr.length();
		while (end > start && Character.isWhitespace(queryStr.charAt(end - 1))) end--;

		var parts = new ArrayList<String>();
		int partStart = start;
		for (int i = start; i <= end; i++) {
			if (i == end || queryStr.charAt(i) == '/') {
				if (i > partStart) parts.add(queryStr.substring(partStart, i));
				partStart = i + 1;
			}
		}

		return new WMLQuery(parts, absolute);
	}
}
