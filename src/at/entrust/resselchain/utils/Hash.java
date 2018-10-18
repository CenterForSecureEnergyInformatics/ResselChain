/*
* This file is part of ResselChain.
* Copyright Center for Secure Energy Informatics 2018
* Fabian Knirsch, Andreas Unterweger, Clemens Brunner
* This code is licensed under a modified 3-Clause BSD License. See LICENSE file for details.
*/

package at.entrust.resselchain.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import at.entrust.resselchain.config.GlobalConfig;

public class Hash {

	public static byte[] hash(byte[] input) {
		try {
			MessageDigest md = MessageDigest.getInstance(GlobalConfig.INSTANCE.HASH_ALOGITHM);
			md.update(input);
			return md.digest();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static byte[] hash(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance(GlobalConfig.INSTANCE.HASH_ALOGITHM);
			md.update(input.getBytes());
			return md.digest();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}
}
