package com.willvuong.septacular.android;

public class Utils {

	public static int toInt(String number, int defaultValue) {
		try {
			return Integer.valueOf(number);
		}
		catch (NumberFormatException e) {
			return defaultValue;
		}
	}
	
}
