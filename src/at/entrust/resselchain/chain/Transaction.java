/*
* This file is part of ResselChain.
* Copyright Center for Secure Energy Informatics 2018
* Fabian Knirsch, Andreas Unterweger, Clemens Brunner
* This code is licensed under a modified 3-Clause BSD License. See LICENSE file for details.
*/

package at.entrust.resselchain.chain;

import java.io.Serializable;
import java.util.Arrays;
import at.entrust.resselchain.utils.Hash;

public class Transaction implements Serializable {

	private static final long serialVersionUID = 4001966383785922506L;
	protected String sender;
	protected String receiver;
	protected String assetName;
	protected long timestamp;
	protected int amount;
	protected byte[] signature;
	protected String tag;
	private boolean isExternal;

	public Transaction(String sender, String receiver, long timestamp, int amount, byte[] signature, String tag, String assetName) {
		this(sender, receiver, timestamp, amount, signature, tag, false, assetName);
	}
	
	public Transaction(String sender, String receiver, long timestamp, int amount, boolean isExternal, String assetName) {
		super();
		this.sender = sender;
		this.receiver = receiver;
		this.timestamp = timestamp;
		this.amount = amount;
		this.tag = "";
		this.isExternal = isExternal;
		this.assetName = assetName;
	}
	
	public Transaction(String sender, String receiver, long timestamp, int amount, byte[] signature, String tag, boolean isExternal, String assetName) {
		super();
		this.sender = sender;
		this.receiver = receiver;
		this.timestamp = timestamp;
		this.amount = amount;
		this.signature = signature;
		this.tag = ((tag == null) ? "" : tag);
		this.isExternal = isExternal;
		this.assetName = assetName;
	}
			
	
	public byte[] getTransactionHash() {
		// append all data fields
		StringBuilder sb = new StringBuilder();
		sb.append(sender);
		sb.append(receiver);
		sb.append(assetName);
		sb.append(timestamp);
		sb.append(amount);
		sb.append(tag);
		
		return Hash.hash(sb.toString());
	}
	
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((assetName == null) ? 0 : assetName.hashCode());
		result = prime * result + (isExternal ? 1231 : 1237);
		result = prime * result + amount;
		result = prime * result + ((receiver == null) ? 0 : receiver.hashCode());
		result = prime * result + ((sender == null) ? 0 : sender.hashCode());
		result = prime * result + Arrays.hashCode(signature);
		result = prime * result + ((tag == null) ? 0 : tag.hashCode());
		result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Transaction))
			return false;
		Transaction other = (Transaction) obj;
		if (assetName == null) {
			if (other.assetName != null)
				return false;
		} else if (!assetName.equals(other.assetName))
			return false;
		if (isExternal != other.isExternal)
			return false;
		if (amount != other.amount)
			return false;
		if (receiver == null) {
			if (other.receiver != null)
				return false;
		} else if (!receiver.equals(other.receiver))
			return false;
		if (sender == null) {
			if (other.sender != null)
				return false;
		} else if (!sender.equals(other.sender))
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
		return true;
	}

	public String getSender() {
		return sender;
	}
	
	public String getReceiver() {
		return receiver;
	}
	
	public long getTimestamp() {
		return timestamp;
	}
	
	public int getAmount() {
		return amount;
	}
	
	public byte[] getSignature() {
		return signature;
	}
	
	public String getTag() {
		return tag;
	}

	public void setSignature(byte[] signature) {
		this.signature = signature;
	}

	public boolean isExternal() {
		return isExternal;
	}
	
	public String getAssetName() {
		return assetName;
	}
	
}
