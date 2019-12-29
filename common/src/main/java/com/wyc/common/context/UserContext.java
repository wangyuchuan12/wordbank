package com.wyc.common.context;


import com.auth0.jwt.algorithms.Algorithm;

import java.io.UnsupportedEncodingException;

public abstract class UserContext {


	public static void clear() {

	}

	public static Algorithm getDefaultAlgorithm() {
		try {
			return Algorithm.HMAC256("adfa1479akpqo0Il1Xm");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public static final String HEADER_TOKEN = "x-token";

}
