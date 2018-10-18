/*
* This file is part of ResselChain.
* Copyright Center for Secure Energy Informatics 2018
* Fabian Knirsch, Andreas Unterweger, Clemens Brunner
* This code is licensed under a modified 3-Clause BSD License. See LICENSE file for details.
*/

package at.entrust.resselchain.chain;

public class ExternalTransaction extends Transaction {
	private static final long serialVersionUID = 651082921751822082L;

	public ExternalTransaction(String sender, String receiver, long timestamp, int amount,
			byte[] signature, String tag, String assetName) {
		super(sender, receiver, timestamp, amount, signature, tag, assetName);
	}
	// inherits all methods from transaction
}
