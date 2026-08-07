package com.sk.skala.shopapi.tools;

// 문자열 관련 공통 유틸리티
public class StringUtil {

	private StringUtil() {
	}

	// 하나라도 null이거나 공백이면 true
	public static boolean isAnyEmpty(String... values) {
		if (values == null || values.length == 0) {
			return true;
		}
		for (String value : values) {
			if (value == null || value.trim().isEmpty()) {
				return true;
			}
		}
		return false;
	}

	// 모두 값이 있으면 true
	public static boolean isNoneEmpty(String... values) {
		return !isAnyEmpty(values);
	}
}
