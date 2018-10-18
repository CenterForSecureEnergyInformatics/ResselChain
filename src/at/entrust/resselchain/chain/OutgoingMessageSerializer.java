/*
* This file is part of ResselChain.
* Copyright Center for Secure Energy Informatics 2018
* Fabian Knirsch, Andreas Unterweger, Clemens Brunner
* This code is licensed under a modified 3-Clause BSD License. See LICENSE file for details.
*/

package at.entrust.resselchain.chain;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import at.entrust.resselchain.config.GlobalConfig;
import at.entrust.resselchain.utils.Base64Converter;
import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;

public class OutgoingMessageSerializer {
		
	public String serializeBlockToString(Block block) {
		return serializeBlockToString(block, false);
	}
	
	public String serializeBlockToString(Block block, boolean isSyncResponse) {
		// serialize block to XML
	
		Element root = serializeBlockToElement(block, isSyncResponse);

		Document doc = new Document(root);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			Serializer serializer = new Serializer(out, "UTF-8");
			serializer.setLineSeparator("\n");
			serializer.write(doc);

			return out.toString().replace('\n', ' ');

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public Element serializeBlockToElement(Block block, boolean isSyncResponse) {
		// serialize block to XML
				
		Element root = new Element((isSyncResponse ? "SyncBlock" : "Block"));
		Element timestamp = new Element("Timestamp");
		timestamp.appendChild(String.valueOf(block.getTimestamp()));
		root.appendChild(timestamp);

		Element blockNumber = new Element("BlockNumber");
		blockNumber.appendChild(String.valueOf(block.getBlockNumber()));
		root.appendChild(blockNumber);

		Element nonce = new Element("Nonce");
		nonce.appendChild(String.valueOf(block.getNonce()));
		root.appendChild(nonce);

		Element miner = new Element("Miner");
		miner.appendChild(block.getMiner());
		root.appendChild(miner);

		Element hash = new Element("Hash");
		hash.appendChild(Base64Converter.encodeFromByteArray(block.getBlockHash()));
		root.appendChild(hash);

		Element difficulty = new Element("Difficulty");
		difficulty.appendChild(String.valueOf(block.getDifficulty()));
		root.appendChild(difficulty);

		Element previousBlockhash = new Element("PreviousBlockHash");
		previousBlockhash.appendChild(Base64Converter.encodeFromByteArray(block.getPreviousBlockHash()));
		root.appendChild(previousBlockhash);

		Element signature = new Element("Signature");
		signature.appendChild(Base64Converter.encodeFromByteArray(block.getSignature()));
		root.appendChild(signature);
		
		Element tag = new Element("Tag");
		tag.appendChild(block.getTag());
		root.appendChild(tag);
		
		// Node that sends this information (this field is just for P2P routing)
		Element sendingNode = new Element("SendingNode");
		sendingNode.appendChild(GlobalConfig.INSTANCE.PARTICIPANT_CONFIG.getName());
		root.appendChild(sendingNode);
		
		
		Element transactions = new Element("Transactions");
		root.appendChild(transactions);
		int order = 0;
		for(Transaction tx : block.getTransactions()) {
			
			String type;
			if (tx instanceof UtilityTransaction)
				type = "UtilityTx";
			else
				type = "Tx";
			
			Element transaction = new Element(type);
			transaction.addAttribute(new Attribute("order", String.valueOf(order++)));
			
			Element timestampTx = new Element("Timestamp");
			timestampTx.appendChild(String.valueOf(tx.getTimestamp()));
			transaction.appendChild(timestampTx);

			Element sender = new Element("Sender");
			sender.appendChild(tx.getSender());
			transaction.appendChild(sender);
			
			Element receiver = new Element("Receiver");
			receiver.appendChild(tx.getReceiver());
			transaction.appendChild(receiver);
			
			Element assetName = new Element("assetName");
			assetName.appendChild(tx.getAssetName());
			transaction.appendChild(assetName);
			
			Element amount = new Element("Amount");
			amount.appendChild(String.valueOf(tx.getAmount()));
			transaction.appendChild(amount);
			
			Element txSignature = new Element("Signature");
			txSignature.appendChild(Base64Converter.encodeFromByteArray(tx.getSignature()));
			transaction.appendChild(txSignature);
			
			Element txTag = new Element("Tag");
			txTag.appendChild(tx.getTag());
			transaction.appendChild(txTag);
			
			// for utility Tx
			if (tx instanceof UtilityTransaction) {
				
				ArrayList<Participant> participants = ((UtilityTransaction)tx).getParticipants();
				HashMap<Participant, Integer> participantAmounts = ((UtilityTransaction)tx).getParticipantShares();
				
				Element pElements = new Element("Participants");
				transaction.appendChild(pElements);
				for (Participant p : participants) {
					Element pElement = new Element("Participant");
					pElements.appendChild(pElement);
					
					Element pName = new Element("Name");
					pName.appendChild(p.getName());
					pElement.appendChild(pName);
					
					Element pPublicKey = new Element("PublicKey");
					pPublicKey.appendChild(Base64Converter.encodeFromByteArray(p.getPublickey()));
					pElement.appendChild(pPublicKey);
					
					Element pAddress = new Element("Address");
					pAddress.appendChild(p.getAddress());
					pElement.appendChild(pAddress);
					
					Element pPort = new Element("Port");
					pPort.appendChild(String.valueOf(p.getPort()));
					pElement.appendChild(pPort);
					
					Element pAssetAmount = new Element("AssetAmount");
					pAssetAmount.appendChild(String.valueOf(participantAmounts.get(p)));
					pElement.appendChild(pAssetAmount);
				}
			}
			
			transactions.appendChild(transaction);
		}

		return root;
	}
	
	public String serializeTxToString(Transaction tx) {
		// serialize transaction to XML
		
		String type;
		if (tx instanceof UtilityTransaction)
			type = (tx.isExternal() ? "ExternalUtilityTx" : "UtilityTx");
		else
			type = (tx.isExternal() ? "ExternalTx" : "Tx");
		
		Element root = new Element(type);
		Element timestamp = new Element("Timestamp");
		timestamp.appendChild(String.valueOf((tx.getTimestamp())));
		root.appendChild(timestamp);

		Element sender = new Element("Sender");
		sender.appendChild(tx.getSender());
		root.appendChild(sender);
		
		Element receiver = new Element("Receiver");
		receiver.appendChild(tx.getReceiver());
		root.appendChild(receiver);
		
		Element assetName = new Element("assetName");
		assetName.appendChild(tx.getAssetName());
		root.appendChild(assetName);
		
		Element amount = new Element("Amount");
		amount.appendChild(String.valueOf(tx.getAmount()));
		root.appendChild(amount);
		
		Element txSignature = new Element("Signature");
		txSignature.appendChild(Base64Converter.encodeFromByteArray(tx.getSignature()));
		root.appendChild(txSignature);
		
		Element txTag = new Element("Tag");
		txTag.appendChild(tx.getTag());
		root.appendChild(txTag);

		
		// Node that sends this information (this field is just for P2P routing)
		Element sendingNode = new Element("SendingNode");
		sendingNode.appendChild(GlobalConfig.INSTANCE.PARTICIPANT_CONFIG.getName());
		root.appendChild(sendingNode);
		
		// for utility Tx
		if (tx instanceof UtilityTransaction) {

			ArrayList<Participant> participants = ((UtilityTransaction)tx).getParticipants();
			HashMap<Participant, Integer> participantAmounts = ((UtilityTransaction)tx).getParticipantShares();

			Element pElements = new Element("Participants");
			root.appendChild(pElements);
			for (Participant p : participants) {
				Element pElement = new Element("Participant");
				pElements.appendChild(pElement);

				Element pName = new Element("Name");
				pName.appendChild(p.getName());
				pElement.appendChild(pName);

				Element pPublicKey = new Element("PublicKey");
				pPublicKey.appendChild(Base64Converter.encodeFromByteArray(p.getPublickey()));
				pElement.appendChild(pPublicKey);

				Element pAddress = new Element("Address");
				pAddress.appendChild(p.getAddress());
				pElement.appendChild(pAddress);

				Element pPort = new Element("Port");
				pPort.appendChild(String.valueOf(p.getPort()));
				pElement.appendChild(pPort);

				Element pAssetAmount = new Element("AssetAmount");
				pAssetAmount.appendChild(String.valueOf(participantAmounts.get(p)));
				pElement.appendChild(pAssetAmount);
			}
		}

		Document doc = new Document(root);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			Serializer serializer = new Serializer(out, "UTF-8");
			serializer.setLineSeparator("\n");
			serializer.write(doc);

			return out.toString().replace('\n', ' ');

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public String serializeBlockNumberToString(long fromBlockNumber, long toBlockNumber, String fromBlockHash) {

		Element root = new Element("Request");
		
		Element fromBlockNum = new Element("FromBlockNumber");
		fromBlockNum.appendChild(String.valueOf(fromBlockNumber));
		root.appendChild(fromBlockNum);
		
		Element toBlockNum = new Element("ToBlockNumber");
		toBlockNum.appendChild(String.valueOf(toBlockNumber));
		root.appendChild(toBlockNum);
		
		Element fromBlockH = new Element("FromBlockHash");
		fromBlockH.appendChild(String.valueOf(fromBlockHash));
		root.appendChild(fromBlockH);
		
		Element sendingNode = new Element("SendingNode");
		sendingNode.appendChild(GlobalConfig.INSTANCE.PARTICIPANT_CONFIG.getName());
		root.appendChild(sendingNode);

		Document doc = new Document(root);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			Serializer serializer = new Serializer(out, "UTF-8");
			serializer.setLineSeparator("\n");
			serializer.write(doc);

			return out.toString().replace('\n', ' ');

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
