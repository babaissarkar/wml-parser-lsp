package com.babai.wml.query;

import java.util.List;

import com.babai.wml.utils.AIGenerated;

public class Query {
	@AIGenerated
	public static boolean match(String queryStr, List<String> tagStack, String key) {
		if (queryStr.startsWith("//")) {
			return anyMatch(queryStr.substring(2, queryStr.length()), tagStack, key);
		} else {
			return absoluteMatch(queryStr, tagStack, key);
		}
//		return false;
	}

	private static boolean anyMatch(String queryStr, List<String> tagStack, String key) {
		String[] queryParts = queryStr.split("/");
		
		if (tagStack.size() < queryParts.length - 1) return false; // not deep enough
		
		boolean matchStart = false;
		int matchedUpto = 0;
		int lastMatchStart = 0;
		for (int i = 0; i < tagStack.size();) {
			if (!matchStart) {
				matchStart = tagStack.get(i).equals(queryParts[matchedUpto]);
				if (matchStart) {
					matchedUpto = 1;
					lastMatchStart = i;
				}
				i++;
			} else {
				if (tagStack.get(i).equals(queryParts[matchedUpto])) {
					if (matchedUpto < queryParts.length - 1) {
						matchedUpto++;
						i++;
					} else {
						return true;
					}
				} else {
					matchStart = false;
					i = lastMatchStart + 1;
					matchedUpto = 0;
					lastMatchStart = 0;
				}
			}
		}
		
		return matchedUpto == queryParts.length - 1;
	}

	private static boolean absoluteMatch(String queryStr, List<String> tagStack, String key) {
		String[] queryParts = queryStr.split("/");

		if (tagStack.size() < queryParts.length - 1) return false; // not deep enough

		for (int i = 0; i < queryParts.length; i++) {
			if (i == queryParts.length - 1) {
				// if stack has this level, it's a tag match
				// if stack is one short, it's a key match
				if (tagStack.size() > i) {
					return tagStack.get(i).equals(queryParts[i]);
				} else {
					return key.equals(queryParts[i]);
				}
			}
			if (!tagStack.get(i).equals(queryParts[i])) return false;
		}

		return true; // all tag parts matched, no key part
	}
}
