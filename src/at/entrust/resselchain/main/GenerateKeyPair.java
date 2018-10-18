/*
* This file is part of ResselChain.
* Copyright Center for Secure Energy Informatics 2018
* Fabian Knirsch, Andreas Unterweger, Clemens Brunner
* This code is licensed under a modified 3-Clause BSD License. See LICENSE file for details.
*/

package at.entrust.resselchain.main;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import at.entrust.resselchain.utils.Base64Converter;
import at.entrust.resselchain.utils.Sign;

public class GenerateKeyPair {

	public static void main(String[] args) throws NoSuchAlgorithmException {
		KeyPair kp = Sign.getKeyPair();
		PrivateKey sk = kp.getPrivate();
		PublicKey pk = kp.getPublic();

		String skBase64 = Base64Converter.encodeFromByteArray(sk.getEncoded());
		String pkBase64 = Base64Converter.encodeFromByteArray(pk.getEncoded());
		
		System.out.println("<SecretKey>" + skBase64 + "</SecretKey>");
		System.out.println("<PublicKey>" + pkBase64 + "</PublicKey>");
	}

}
