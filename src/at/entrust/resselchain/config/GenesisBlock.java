/*
* This file is part of ResselChain.
* Copyright Center for Secure Energy Informatics 2018
* Fabian Knirsch, Andreas Unterweger, Clemens Brunner
* This code is licensed under a modified 3-Clause BSD License. See LICENSE file for details.
*/

package at.entrust.resselchain.config;


import at.entrust.resselchain.chain.Block;

public class GenesisBlock {

	private final static String signature = "Center for Secure Energy Informatics";
	private final static String parentHash = "0x";
	
	public static Block getGenesisBlock() {
		
		StringBuilder sb = new StringBuilder();
		sb.append(signature);

		return new Block(1539259694,
						0,
						2001681207,
						"Genesis Block",
						0,
						parentHash.getBytes(),
				        signature.toString());
	}
}
