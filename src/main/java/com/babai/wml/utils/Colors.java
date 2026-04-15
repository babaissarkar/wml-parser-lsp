package com.babai.wml.utils;

public final class Colors {
	private Colors() {}
	
	public record Color(int r, int g, int b) {};

	public static final Color RED = new Color(255, 0, 0);
	public static final Color GREEN = new Color(0, 255, 0);
	public static final Color CYAN = new Color(0, 255, 255);
	public static final Color ORANGE = new Color(255, 153, 51);
	public static final Color directiveColor = new Color(255, 153, 51);
	public static final Color tdColor = new Color(255, 221, 0);
	public static final Color macroNameColor = new Color(0, 255, 128);
	public static final Color macroArgColor = new Color(255, 0, 255);
	public static final Color lineNumColor = new Color(0, 153, 255);
	public static final Color filePathColor = new Color(128, 192, 255);
	public static final Color tagColor = new Color(0, 206, 209);
}
