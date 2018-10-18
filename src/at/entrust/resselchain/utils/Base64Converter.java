/*
* This file is part of ResselChain.
* Copyright Center for Secure Energy Informatics 2018
* Fabian Knirsch, Andreas Unterweger, Clemens Brunner
* This code is licensed under a modified 3-Clause BSD License. See LICENSE file for details.
*/

package at.entrust.resselchain.utils;

import java.util.Base64;

public class Base64Converter {

	public static String encodeFromByteArray(byte[] input) {
		return new String(Base64.getEncoder().encode(input));
	}
	
	public static String encodeFromString(String input) {
		return new String(Base64.getEncoder().encode(input.getBytes()));
	}
	
	public static String decodeToString(String input) {
		return new String(Base64.getDecoder().decode(input));
	}
	
	public static byte[] decodeToByteArray(String input) {
		return Base64.getDecoder().decode(input);
	}
	
}
