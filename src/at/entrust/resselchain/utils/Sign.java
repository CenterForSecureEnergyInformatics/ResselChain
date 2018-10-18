/*
* This file is part of ResselChain.
* Copyright Center for Secure Energy Informatics 2018
* Fabian Knirsch, Andreas Unterweger, Clemens Brunner
* This code is licensed under a modified 3-Clause BSD License. See LICENSE file for details.
*/

package at.entrust.resselchain.utils;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Arrays;

import at.entrust.resselchain.chain.Block;
import at.entrust.resselchain.chain.Transaction;
import at.entrust.resselchain.config.GlobalConfig;
import at.entrust.resselchain.logging.Logger;

/* Adapted from https://stackoverflow.com/questions/7224626/how-to-sign-string-with-private-key */
public class Sign {

	public static KeyPair getKeyPair() throws NoSuchAlgorithmException {
		KeyPairGenerator kpg = KeyPairGenerator.getInstance(GlobalConfig.INSTANCE.PKSK_ALGORITHM);
		kpg.initialize(GlobalConfig.INSTANCE.PKSK_KEY_SIZE);
		return kpg.genKeyPair();
	}

	public static Block signBlock(Block block, PrivateKey sk) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		byte[] hash = block.getBlockHash();

		Signature sig = Signature.getInstance(GlobalConfig.INSTANCE.SIGNATURE_ALGORITHM);
		sig.initSign(sk);
		sig.update(hash);
		byte[] signature = sig.sign();

		//Logger.FULL.log("sign block: " + Arrays.toString(signature));

		block.setSignature(signature);
		return block;
	}

	public static Transaction signTransaction(Transaction tx, PrivateKey sk) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		byte[] hash = tx.getTransactionHash();

		Signature sig = Signature.getInstance(GlobalConfig.INSTANCE.SIGNATURE_ALGORITHM);
		sig.initSign(sk);
		sig.update(hash);
		byte[] signature = sig.sign();
		

		//Logger.FULL.log("sign tx: " + Arrays.toString(signature));

		tx.setSignature(signature);
		return tx;
	}
	
	public static boolean verifySignature(Block block, PublicKey pk) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
		byte[] signature = block.getSignature();
		byte[] hash = block.getBlockHash();
		
		//Logger.FULL.log("Verify Block Signature: Hash: " + Arrays.toString(hash) + " Signature: " + Arrays.toString(signature) + " PK: " + Arrays.toString(pk.getEncoded()) + " PK Base64: " + Base64Converter.encodeFromByteArray(pk.getEncoded()));
		
		Signature sig = Signature.getInstance(GlobalConfig.INSTANCE.SIGNATURE_ALGORITHM);
		sig.initVerify(pk);
		sig.update(hash);

		//Logger.FULL.log("verify block signature: " + Arrays.toString(signature));
		
		return sig.verify(signature);
	}

	public static boolean verifySignature(Transaction tx, PublicKey pk) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
		byte[] signature = tx.getSignature();
		byte[] hash = tx.getTransactionHash();

		//Logger.FULL.log("Verify Tx Signature: Hash: " + Arrays.toString(hash) + " Signature: " + Arrays.toString(signature) + " PK: " + Arrays.toString(pk.getEncoded()) + " PK Base64: " + Base64Converter.encodeFromByteArray(pk.getEncoded()));
		
		Signature sig = Signature.getInstance(GlobalConfig.INSTANCE.SIGNATURE_ALGORITHM);
		sig.initVerify(pk);
		sig.update(hash);

		//Logger.FULL.log("verify tx signature: " + Arrays.toString(signature));
		
		return sig.verify(signature);
	}
}
