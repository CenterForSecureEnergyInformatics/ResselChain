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
import java.util.HashMap;

import at.entrust.resselchain.utils.Hash;

public class UtilityTransaction extends Transaction implements Serializable {
	private static final long serialVersionUID = -9032552391754968009L;
	private ArrayList<Participant> participants = new ArrayList<>(); // list of participants for this asset
	private HashMap<Participant, Integer> participantAmounts = new HashMap<>(); // asset amounts for participants
	
	public UtilityTransaction(String sender, String receiver, long timestamp, int amount, byte[] signature, String tag, String assetName, ArrayList<Participant> participants, HashMap<Participant, Integer> participantAmounts) {
		super(sender, receiver, timestamp, amount, signature, tag, assetName);
		this.participants = participants;
		this.participantAmounts = participantAmounts;
	}

	public UtilityTransaction(String sender, String receiver, long timestamp, int amount, String assetName, ArrayList<Participant> participants, HashMap<Participant, Integer> participantAmounts, boolean isExternal) {
		super(sender, receiver, timestamp, amount, isExternal, assetName);
		this.participants = participants;
		this.participantAmounts = participantAmounts;
	}
	
	@Override
	public byte[] getTransactionHash() {
		// append all data fields
		StringBuilder sb = new StringBuilder();
		sb.append(sender);
		sb.append(receiver);
		sb.append(assetName);
		sb.append(timestamp);
		sb.append(amount);
		sb.append(tag);

		// data fields for utility transaction
		sb.append(assetName);
		
		for (Participant p : participants) {
			sb.append(p.getName());
			sb.append(p.getAddress());
			sb.append(p.getPort());
			sb.append(Arrays.toString(p.getPublickey()));
		}
		
		for (Participant p : participantAmounts.keySet()) {
			sb.append(p.getName());
			sb.append(p.getAddress());
			sb.append(p.getPort());
			sb.append(Arrays.toString(p.getPublickey()));
			sb.append(participantAmounts.get(p));
		}
		
		//if (!Thread.currentThread().getName().equals("Miner"))
		//	Logger.FULL.log("HASHED TX VALUE: " + sb.toString());
		
		return Hash.hash(sb.toString());
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		long temp = 0;
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((participantAmounts == null) ? 0 : participantAmounts.hashCode());
		result = prime * result + ((participants == null) ? 0 : participants.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof UtilityTransaction))
			return false;
		UtilityTransaction other = (UtilityTransaction) obj;
		if (participantAmounts == null) {
			if (other.participantAmounts != null)
				return false;
		} else if (!participantAmounts.equals(other.participantAmounts))
			return false;
		if (participants == null) {
			if (other.participants != null)
				return false;
		} else if (!participants.equals(other.participants))
			return false;
		return true;
	}

	public ArrayList<Participant> getParticipants() {
		return participants;
	}

	public HashMap<Participant, Integer> getParticipantShares() {
		return participantAmounts;
	}

}
