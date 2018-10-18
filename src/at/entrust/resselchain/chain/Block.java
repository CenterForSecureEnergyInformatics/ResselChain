/*
* This file is part of ResselChain.
* Copyright Center for Secure Energy Informatics 2018
* Fabian Knirsch, Andreas Unterweger, Clemens Brunner
* This code is licensed under a modified 3-Clause BSD License. See LICENSE file for details.
*/

package at.entrust.resselchain.chain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import at.entrust.resselchain.utils.Base64Converter;
import at.entrust.resselchain.utils.Hash;

public class Block implements Serializable {
	private static final long serialVersionUID = 4873236738290079944L;
	private long timestamp;
	private long blockNumber;
	private long nonce;
	private String miner;
	private int difficulty;
	private byte[] previousBlockHash;
	private byte[] signature;
	private ArrayList<Transaction> transactions = new ArrayList<>();
	private String tag;
	private String hashWithoutNonce = null;
	private byte[] hash;

	public Block(long timestamp, long blockNumber, long nonce, String miner, int difficulty, byte[] previousBlockhash, byte[] signature, String tag) {
		super();
		this.timestamp = timestamp;
		this.blockNumber = blockNumber;
		this.nonce = nonce;
		this.miner = miner;
		this.difficulty = difficulty;
		this.previousBlockHash = previousBlockhash;
		this.signature = signature;
		this.tag = ((tag == null) ? "" : tag);
		this.hash = getBlockHash();
	}
	
	public Block(long timestamp, long blockNumber, long nonce, String miner, int difficulty, byte[] previousBlockhash, String tag) {
		super();
		this.timestamp = timestamp;
		this.blockNumber = blockNumber;
		this.nonce = nonce;
		this.miner = miner;
		this.difficulty = difficulty;
		this.previousBlockHash = previousBlockhash;
		this.tag = ((tag == null) ? "" : tag);
		this.hash = getBlockHash();
	}
	
	public Block(long timestamp, long blockNumber, long nonce, String miner, int difficulty, byte[] previousBlockhash, byte[] signature) {
		super();
		this.timestamp = timestamp;
		this.blockNumber = blockNumber;
		this.nonce = nonce;
		this.miner = miner;
		this.difficulty = difficulty;
		this.previousBlockHash = previousBlockhash;
		this.signature = signature;
		this.tag = "";
		this.hash = getBlockHash();
	}
	
	public Block(long timestamp, long blockNumber, long nonce, String miner, int difficulty, byte[] previousBlockhash) {
		super();
		this.timestamp = timestamp;
		this.blockNumber = blockNumber;
		this.nonce = nonce;
		this.miner = miner;
		this.difficulty = difficulty;
		this.previousBlockHash = previousBlockhash;
		this.tag = "";
		this.hash = getBlockHash();
	}

	public void setNonce(long nonce){
		this.nonce = nonce;
	}
	
	public ArrayList<Transaction> getTransactions() {
		return transactions;
	}
	
	public void setSignature(byte[] signature) {
		this.signature = signature;
	}
	
	public void addTransaction(Transaction value) {
		transactions.add(value);
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (blockNumber ^ (blockNumber >>> 32));
		result = prime * result + difficulty;
		result = prime * result + ((miner == null) ? 0 : miner.hashCode());
		result = prime * result + (int) (nonce ^ (nonce >>> 32));
		result = prime * result + Arrays.hashCode(previousBlockHash);
		result = prime * result + Arrays.hashCode(signature);
		result = prime * result + ((tag == null) ? 0 : tag.hashCode());
		result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
		result = prime * result + ((transactions == null) ? 0 : transactions.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Block))
			return false;
		Block other = (Block) obj;
		if (blockNumber != other.blockNumber)
			return false;
		if (difficulty != other.difficulty)
			return false;
		if (miner == null) {
			if (other.miner != null)
				return false;
		} else if (!miner.equals(other.miner))
			return false;
		if (nonce != other.nonce)
			return false;
		if (!Arrays.equals(previousBlockHash, other.previousBlockHash))
			return false;
		if (!Arrays.equals(signature, other.signature))
			return false;
		if (tag == null) {
			if (other.tag != null)
				return false;
		} else if (!tag.equals(other.tag))
			return false;
		if (timestamp != other.timestamp)
			return false;
		if (transactions == null) {
			if (other.transactions != null)
				return false;
		} else if (!transactions.equals(other.transactions))
			return false;
		return true;
	}

	public byte[] getBlockHash() {
		if (hashWithoutNonce == null) {
			// append all data fields and transaction data fields
			StringBuilder sb = new StringBuilder();
			sb.append(timestamp);
			sb.append(blockNumber);
			sb.append(miner); // only set name of miner in block header
			sb.append(difficulty);
			sb.append(Base64Converter.encodeFromByteArray(previousBlockHash));
			sb.append(tag);
			// sb.append(Base64Converter.encodeFromByteArray(signature)); // do not include signature in hash, otherwise we cannot sign the block hash

			for (Transaction t : transactions) {
				sb.append(Arrays.toString(t.getTransactionHash()));
			}
			hashWithoutNonce = Arrays.toString(Hash.hash(sb.toString()));
		}
		return Hash.hash(hashWithoutNonce + nonce);
	}

	public long getTimestamp() {
		return timestamp;
	}

	public long getBlockNumber() {
		return blockNumber;
	}

	public long getNonce() {
		return nonce;
	}

	public String getMiner() {
		return miner;
	}

	public int getDifficulty() {
		return difficulty;
	}

	public byte[] getPreviousBlockHash() {
		return previousBlockHash;
	}

	public byte[] getSignature() {
		return signature;
	}
	
	public String getTag() {
		return tag;
	}
	
}
