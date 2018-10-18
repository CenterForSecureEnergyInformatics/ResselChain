/*
* This file is part of ResselChain.
* Copyright Center for Secure Energy Informatics 2018
* Fabian Knirsch, Andreas Unterweger, Clemens Brunner
* This code is licensed under a modified 3-Clause BSD License. See LICENSE file for details.
*/

package at.entrust.resselchain.chain;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import at.entrust.resselchain.config.GlobalConfig;
import at.entrust.resselchain.logging.Logger;
import at.entrust.resselchain.state.ChainState;
import at.entrust.resselchain.utils.Base64Converter;
import at.entrust.resselchain.utils.Sign;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.ParsingException;
import nu.xom.ValidityException;

public class IncomingMessageSerializer {

	public ArrayList<Block> deserializeBlocksFromElement(Element root) throws ValidityException, ParsingException, IOException {
		OutgoingMessageSerializer outgoingMessageSerializer = new OutgoingMessageSerializer();


		Elements elements = root.getChildElements("SyncBlock");

		ArrayList<Block> blocks = new ArrayList<>();

		for (int i= 0 ; i < elements.size(); i++){
			Element block = elements.get(i);
			blocks.add(deserializeBlockFromElement(block));
		}

		return blocks;
	}

	public Block deserializeBlockFromString(String message) throws ValidityException, ParsingException, IOException {
		Builder parser = new Builder();
		Document doc = parser.build(message, null);

		Element root = doc.getRootElement();
		return deserializeBlockFromElement(root);
	}

	public Block deserializeBlockFromElement(Element root) throws ValidityException, ParsingException, IOException {

		// TODO: proper error handling for parsing the XML document
		long timestamp = Long.valueOf(root.getChildElements("Timestamp").get(0).getValue());
		long blockNumber = Long.valueOf(root.getChildElements("BlockNumber").get(0).getValue());
		long nonce = Long.valueOf(root.getChildElements("Nonce").get(0).getValue());
		String miner = root.getChildElements("Miner").get(0).getValue();
		int difficulty = Integer.valueOf(root.getChildElements("Difficulty").get(0).getValue());
		byte[] previousBlockhash = Base64Converter.decodeToByteArray(root.getChildElements("PreviousBlockHash").get(0).getValue());
		byte[] signature = Base64Converter.decodeToByteArray(root.getChildElements("Signature").get(0).getValue());
		String tag = root.getChildElements("Tag").get(0).getValue();
		
		Block block = new Block(timestamp, blockNumber, nonce, miner, difficulty, previousBlockhash, signature, tag);
		
		
		HashMap<Integer, Transaction> unorderedTransaction = new HashMap<>();
		Elements transactions = root.getChildElements("Transactions").get(0).getChildElements();
		for (int i = 0; i < transactions.size(); i++) {
			Element tx = transactions.get(i);
			int order = Integer.valueOf(tx.getAttribute("order").getValue());
			String sender = tx.getChildElements("Sender").get(0).getValue();
			String receiver = tx.getChildElements("Receiver").get(0).getValue();
			String assetName = tx.getChildElements("assetName").get(0).getValue();
			long txTimestamp = Long.valueOf(tx.getChildElements("Timestamp").get(0).getValue());
			int amount = Integer.valueOf(tx.getChildElements("Amount").get(0).getValue());
			byte[] txSignature = Base64Converter.decodeToByteArray(tx.getChildElements("Signature").get(0).getValue());
			String txTag = tx.getChildElements("Tag").get(0).getValue();
			
			Transaction transaction; // = new Transaction(sender, receiver, txTimestamp, amount, txSignature, txTag);
			
			// check if this is a utility Tx
			if (tx.getLocalName().equals("UtilityTx")) {

				ArrayList<Participant> participantsList = new ArrayList<>();
				HashMap<Participant, Integer> AssetAmountList = new HashMap<>();
				Elements participants = tx.getChildElements("Participants").get(0).getChildElements();
				
				for (int k = 0; k < participants.size(); k++) {
					Participant p = new Participant(participants.get(k).getChildElements("Name").get(0).getValue(), Base64Converter.decodeToByteArray(participants.get(k).getChildElements("PublicKey").get(0).getValue()), participants.get(k).getChildElements("Address").get(0).getValue(), Integer.valueOf(participants.get(k).getChildElements("Port").get(0).getValue()));
					participantsList.add(p);
					
					AssetAmountList.put(p, Integer.valueOf(participants.get(k).getChildElements("AssetAmount").get(0).getValue()));
				}
				
				// utility Tx
				transaction = new UtilityTransaction(sender, receiver, txTimestamp, amount, txSignature, txTag, assetName, participantsList, AssetAmountList);
			} else {
				// normal Tx
				transaction = new Transaction(sender, receiver, txTimestamp, amount, txSignature, txTag, assetName);
			}
			
			
			
			// check signature of transaction
			// if the signature for one transaction is invalid, the entire block is invalid!
			Participant participant = ChainState.INSTANCE.getParticipantByName(sender);			
			if (participant == null) {
				// invalid block
				Logger.STD.log("Participant not known. Transaction invalid. Block will be discarded.");
				return null;
			}
			
			try {
				KeyFactory keyFactory = KeyFactory.getInstance(GlobalConfig.INSTANCE.PKSK_ALGORITHM);
				PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(participant.getPublickey()));
				
				if (!Sign.verifySignature(transaction, publicKey)) {
					// invalid block
					Logger.STD.log("Transaction signature not valid. Block will be discarded.");
					return null;
				}
				
			} catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException | InvalidKeySpecException e) {
				e.printStackTrace();
			}
			
			unorderedTransaction.put(order, transaction);
		}
		
		for(int i = 0; i < transactions.size(); i++)
			block.addTransaction(unorderedTransaction.get(i));
						
		// check block data, signature, validate transactions and try to append block if valid
		// 1) check basic block data
		if (difficulty != GlobalConfig.INSTANCE.DIFFICULTY) { // TODO: in future, adapt to dynamic difficulty
			// invalid block
			Logger.STD.log("Wrong value for difficulty. Block will be discarded.");
		}
		
		// 2) check signature
		Participant participant = ChainState.INSTANCE.getParticipantByName(miner);
		if (participant == null) {
			// invalid block
			Logger.STD.log("Participant not known. Block will be discarded.");
			return null;
		}
		
		try {
			KeyFactory keyFactory = KeyFactory.getInstance(GlobalConfig.INSTANCE.PKSK_ALGORITHM);
			PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(participant.getPublickey()));
			
			if (!Sign.verifySignature(block, publicKey)) {
				// invalid block
				Logger.STD.log("Block signature not valid. Block will be discarded.");
				return null;
			}
			
		} catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException | InvalidKeySpecException e) {
			e.printStackTrace();
		}
		
		return block;		
	}
	
	
	public ArrayList<Transaction> deserializeTxFromString(String message) throws ValidityException, ParsingException, IOException {
		
		ArrayList<Transaction> transactions = new ArrayList<>();
		
		Builder parser = new Builder();
		Document doc = parser.build(message, null);
		
		Element root = doc.getRootElement();

		if (root.getLocalName().equals("ExternalTx") || root.getLocalName().equals("Tx") || root.getLocalName().equals("ExternalUtilityTx") || root.getLocalName().equals("UtilityTx")) {
			transactions.add(deserializeSingleTxFromString(root));
		} else if (root.getValue().equals("Transactions")) {
			for(int i=0; i < root.getChildElements().size(); i++) {
				transactions.add(deserializeSingleTxFromString(root.getChildElements().get(i)));
			}
		} else {
			Logger.STD.log("Invalid Tx type. Tx will be discarded.");
			return null;
		}
		
		return transactions;
	}

	private Transaction deserializeSingleTxFromString(Element root) throws ValidityException, ParsingException, IOException {
		// TODO: proper error handling for parsing the XML document
		String sender = root.getChildElements("Sender").get(0).getValue();
		String receiver = root.getChildElements("Receiver").get(0).getValue();
		String assetName = root.getChildElements("assetName").get(0).getValue();
		long timestamp = Long.valueOf(root.getChildElements("Timestamp").get(0).getValue());
		int amount = Integer.valueOf(root.getChildElements("Amount").get(0).getValue());
		byte[] txSignature = Base64Converter.decodeToByteArray(root.getChildElements("Signature").get(0).getValue());
		String txTag = root.getChildElements("Tag").get(0).getValue();
		
		// check if this is a utility Tx
		if (root.getLocalName().equals("ExternalUtilityTx") || root.getLocalName().equals("UtilityTx")) {
			ArrayList<Participant> participantsList = new ArrayList<>();
			HashMap<Participant, Integer> AssetAmountList = new HashMap<>();
			Elements participants = root.getChildElements("Participants").get(0).getChildElements();
			
			for (int i = 0; i < participants.size(); i++) {
				Participant p = new Participant(participants.get(i).getChildElements("Name").get(0).getValue(), Base64Converter.decodeToByteArray(participants.get(i).getChildElements("PublicKey").get(0).getValue()), participants.get(i).getChildElements("Address").get(0).getValue(), Integer.valueOf(participants.get(i).getChildElements("Port").get(0).getValue()));
				participantsList.add(p);
				
				AssetAmountList.put(p, Integer.valueOf(participants.get(i).getChildElements("AssetAmount").get(0).getValue()));
			}
			
			// utility Tx
			return new UtilityTransaction(sender, receiver, timestamp, amount, txSignature, txTag, assetName, participantsList, AssetAmountList);
		} else {
			// normal Tx
			return new Transaction(sender, receiver, timestamp, amount, txSignature, txTag, assetName);
		}
	}
}
