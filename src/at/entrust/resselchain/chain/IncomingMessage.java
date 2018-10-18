/*
* This file is part of ResselChain.
* Copyright Center for Secure Energy Informatics 2018
* Fabian Knirsch, Andreas Unterweger, Clemens Brunner
* This code is licensed under a modified 3-Clause BSD License. See LICENSE file for details.
*/

package at.entrust.resselchain.chain;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

import at.entrust.resselchain.communication.IncomingMessageHandler;
import at.entrust.resselchain.communication.NewClient;
import at.entrust.resselchain.communication.Server;
import at.entrust.resselchain.config.GlobalConfig;
import at.entrust.resselchain.logging.Logger;
import at.entrust.resselchain.mining.Miner;
import at.entrust.resselchain.state.ChainState;
import at.entrust.resselchain.statetable.AssetStateTable;
import at.entrust.resselchain.tree.TreeNode;
import at.entrust.resselchain.utils.Base64Converter;
import at.entrust.resselchain.utils.Sign;
import at.entrust.resselchain.utils.TimeSlots;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;
import nu.xom.Serializer;

public class IncomingMessage implements IncomingMessageHandler {

	private void sendResponse (String message, PrintWriter output){
		output.println(message.replace('\n', ' '));
		output.flush();
	}

	//returns the current state of the node
	private void processStatus(Element root, PrintWriter output){
		// TODO: verify signature

		// parse request
		String senderName = root.getChildElements("SenderName").get(0).getValue();
		String senderSignature = root.getChildElements("SenderSignature").get(0).getValue();

		String receiverAddress = root.getChildElements("ReplyToAddress").get(0).getValue();
		int receiverPort = Integer.valueOf(root.getChildElements("ReplyToPort").get(0).getValue());


		// build response
		Element replyRoot = new Element("Status");

		Element nodeName = new Element("NodeName");
		nodeName.appendChild(GlobalConfig.INSTANCE.PARTICIPANT_CONFIG.getName());
		replyRoot.appendChild(nodeName);

		Element version = new Element("Build");
		version.appendChild(GlobalConfig.INSTANCE.BUILD);
		replyRoot.appendChild(version);

		Element statusMessage = new Element("StatusMessage");
		statusMessage.appendChild(ChainState.INSTANCE.getStatus());
		replyRoot.appendChild(statusMessage);

		Element timestamp = new Element("Timestamp");
		timestamp.appendChild(String.valueOf(System.currentTimeMillis()));
		replyRoot.appendChild(timestamp);

		Element syncIteration = new Element("SyncIteration");
		syncIteration.appendChild(String.valueOf(-1));
		replyRoot.appendChild(syncIteration);

		Element lastBlockNumber = new Element("LastBlockNumber");
		lastBlockNumber.appendChild(String.valueOf(ChainState.INSTANCE.getLastBlock().getBlockNumber()));
		replyRoot.appendChild(lastBlockNumber);

		Element lastBlockRequests = new Element("LastBlockRequests");
		lastBlockRequests.appendChild(String.join(", ", ChainState.INSTANCE.getLastBlockRequests()));
		replyRoot.appendChild(lastBlockRequests);

		synchronized (ChainState.INSTANCE.getBlockchain()) {
			Element numLastBlocks = new Element("NumLastBlocks");
			numLastBlocks.appendChild(String.valueOf(ChainState.INSTANCE.getBlockchain().getLastNodes().size()));
			replyRoot.appendChild(numLastBlocks);

			Element numUnappendableBlocks = new Element("NumUnappendableBlocks");
			numUnappendableBlocks.appendChild(String.valueOf(-1));
			replyRoot.appendChild(numUnappendableBlocks);

			Element numUnconfirmedTransactions = new Element("NumUnconfirmedTransactions");
			numUnconfirmedTransactions.appendChild(String.valueOf(ChainState.INSTANCE.getUnconfirmedTransactionCount()));
			replyRoot.appendChild(numUnconfirmedTransactions);
		}

		Element numMessagesInQueue = new Element("NumMessagesInQueue");
		numMessagesInQueue.appendChild(String.valueOf(NewClient.INSTANCE.getTotalQueueMessages()));
		replyRoot.appendChild(numMessagesInQueue);

		ArrayList<String> tStates = Server.getThreadStates();
		int numTStatesBlocked = 0;
		int numTStatesWaiting = 0;
		int numTStatesOther = 0;
		for (String s : tStates) {
			switch(s) {
				case "BLOCKED": numTStatesBlocked++; break;
				case "WAITING": numTStatesWaiting++; break;
				default: numTStatesOther++;
			}
		}

		Element threadStates = new Element("ThreadStates");
		threadStates.appendChild(numTStatesBlocked + "B " + numTStatesWaiting + "W " + numTStatesOther + "O");
		replyRoot.appendChild(threadStates);


		StringBuilder strLongestChain = new StringBuilder();

		synchronized (ChainState.INSTANCE.getBlockchain()) {

			// in order to have the same "hash" for the chainstate on every node, sort by nonce
			ArrayList<TreeNode> lastNodes = ChainState.INSTANCE.getBlockchain().getLastNodes();
			Collections.sort(lastNodes, new Comparator<TreeNode>() {
				@Override
				public int compare(TreeNode node1, TreeNode node2)
				{
					long diff = node1.getBlock().getNonce() - node2.getBlock().getNonce();
					return (diff > 0) ? 1 : (diff < 0 ? -1 : 0);
				}
			});

			TreeNode parent = lastNodes.get(0);

			long fromBlockNumber = parent.getBlock().getBlockNumber() - GlobalConfig.INSTANCE.LONGEST_CHAIN_STATE_OFFSET - GlobalConfig.INSTANCE.LONGEST_CHAIN_STATE_WINDOW;
			long toBlockNumber = parent.getBlock().getBlockNumber() - GlobalConfig.INSTANCE.LONGEST_CHAIN_STATE_OFFSET;

			if (fromBlockNumber < 0) fromBlockNumber = GlobalConfig.INSTANCE.LONGEST_CHAIN_STATE_OFFSET;
			if (toBlockNumber < 0) toBlockNumber = GlobalConfig.INSTANCE.LONGEST_CHAIN_STATE_OFFSET + GlobalConfig.INSTANCE.LONGEST_CHAIN_STATE_WINDOW;

			for (long i = parent.getBlock().getBlockNumber(); i >=fromBlockNumber; i--) {
				if (parent.getBlock().getBlockNumber() <= toBlockNumber) {
					strLongestChain.append(parent.getBlock().getBlockNumber());
					strLongestChain.append(":");
					strLongestChain.append(parent.getBlock().getMiner());
					strLongestChain.append(",");
					strLongestChain.append(parent.getBlock().getNonce());
					strLongestChain.append(";");
				}
				parent = parent.getParent();
			}

		}

		Element longestChain = new Element("LongestChain");
		longestChain.appendChild(strLongestChain.toString());
		replyRoot.appendChild(longestChain);

		Document reply = new Document(replyRoot);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			Serializer serializer = new Serializer(out, "UTF-8");
			serializer.setLineSeparator("\n");
			serializer.write(reply);
		} catch (IOException e) {
			e.printStackTrace();
		}

        // Status is now sent synchronously
		sendResponse(out.toString(), output);
	}

	// used by viewer
	private void processBlockList(Element root, PrintWriter output) {
		// TODO: verify signature

		// parse request
		String senderName = root.getChildElements("SenderName").get(0).getValue();
		String senderSignature = root.getChildElements("SenderSignature").get(0).getValue();

		long fromBlock = Long.valueOf(root.getChildElements("FromBlock").get(0).getValue());
		long toBlock = Long.valueOf(root.getChildElements("ToBlock").get(0).getValue());

		// build response
		Element replyRoot = new Element("BlockList");

		if (fromBlock < 0 || fromBlock > toBlock)
			return; // ignore invalid request


		synchronized (ChainState.INSTANCE.getBlockchain()) {

			ArrayList<TreeNode> siblings;
			for (long i = fromBlock; i <= toBlock; i++) {
				siblings = TreeNode.getSiblings(i);
				if (siblings == null) break;
				for (TreeNode node : siblings) {
					replyRoot.appendChild(new OutgoingMessageSerializer().serializeBlockToElement(node.getBlock(), false));
				}
			}

		}

		Document reply = new Document(replyRoot);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			Serializer serializer = new Serializer(out, "UTF-8");
			serializer.setLineSeparator("\n");
			serializer.write(reply);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Block list is now sent synchronously
		sendResponse(out.toString(), output);
	}

	private void processBlock(String message){
		try {
			Block block = new IncomingMessageSerializer().deserializeBlockFromString(message);
		    if (block == null) return;

            Logger.STD.logImportant("Block received: Block# " + block.getBlockNumber() +  ", Miner " + block.getMiner());

            // Process block if it is greater than current block
            if (ChainState.INSTANCE.getLastBlock().getBlockNumber() < block.getBlockNumber()) {
				if (!ChainState.INSTANCE.appendBlock(block)) {
					Miner.stopMining();
					ChainState.INSTANCE.startSync(block);
				}
    			Miner.startMining();
            } else{
				Logger.STD.logImportant(String.format("Block[%d] discarded because not longer than current chain[%d]",  block.getBlockNumber(), ChainState.INSTANCE.getLastBlock().getBlockNumber()));
			}

		} catch (ParsingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void processTransaction(String message, boolean external, PrintWriter output){
		// we allow to receive a single external Tx or a list of external Tx
		String stx = external ? "ExternalTx" : "Tx";
		ArrayList<Transaction> transactions = null;
		try {
			transactions = new IncomingMessageSerializer().deserializeTxFromString(message);

		if (transactions == null) return;

		for(Transaction tx : transactions) {
			Logger.STD.log(stx + " received: Sender " + tx.getSender() + ", Receiver " + tx.getReceiver() + ", Asset Name " + tx.getAssetName() +  ", Amount " + tx.getAmount());
			//Logger.FULL.log(message);

			// check signature of transaction
			// if the signature is invalid then discard transaction
			Participant participant = ChainState.INSTANCE.getParticipantByName(tx.getSender());
			if (participant == null) {
				// invalid transaction
				Logger.STD.log("Participant not known. Transaction invalid.");
				return;
			}

			try {
				KeyFactory keyFactory = KeyFactory.getInstance(GlobalConfig.INSTANCE.PKSK_ALGORITHM);
				PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(participant.getPublickey()));

				if (!Sign.verifySignature(tx, publicKey)) {
					// invalid transaction
					Logger.STD.log("Transaction signature not valid.");
					return;
				}

			} catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException | InvalidKeySpecException e) {
				e.printStackTrace();
			}

			// send to local pool of unconfirmed transactions if not already known
			if (ChainState.INSTANCE.existsUnconfirmedTransaction(tx) == false) {
				ChainState.INSTANCE.addUnconfirmedTransaction(tx);
			}

			if (external) {
				// TODO: sign response
				String response = "\"<?xml version=\\\"1.0\\\"?><ExternalResponse Type=\\\"TxAck\\\"><Signature></Signature></ExternalResponse>\"";
				sendResponse(response, output);
				forwardExternalTransaction(tx);
			}


		}
		} catch (ParsingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void forwardExternalTransaction(Transaction tx){
		ChainState.INSTANCE.addUnconfirmedTransaction(tx);
		// broadcast to network
		// who sent this information?
		//String sendingNode = root.getChildElements("SendingNode").get(0).getValue();
		// propagate new transaction to all other nodes, except the sending node and myself to prevent bouncing
		//Client client = null;
		ArrayList<Participant> participants = ChainState.INSTANCE.getAllOtherParticipants();
		for (Participant p : participants) {
			NewClient.INSTANCE.sendMessage(p.getAddress(), p.getPort(), new OutgoingMessageSerializer().serializeTxToString(tx));
		}
	}

	private void processRequest(Element root, PrintWriter output){
		long fromBlockNumber = Long.valueOf(root.getChildElements("FromBlockNumber").get(0).getValue());
		long toBlockNumber = Long.valueOf(root.getChildElements("ToBlockNumber").get(0).getValue());

		if (toBlockNumber == -1)
			toBlockNumber = ChainState.INSTANCE.getLastBlock().getBlockNumber();


		if (fromBlockNumber < 0 || toBlockNumber < fromBlockNumber) {
			Logger.STD.log("Invalid block ranges. Request rejected.");
			return;
		}


		String fromBlockHash = null; // for compatibility with requests from the app this is optional
		if (root.getChildElements("FromBlockHash").size() != 0)
			fromBlockHash = String.valueOf(root.getChildElements("FromBlockHash").get(0).getValue());

		String sendingNode = root.getChildElements("SendingNode").get(0).getValue();
		Participant p = ChainState.INSTANCE.getParticipantByName(sendingNode);

		Logger.STD.log("Request received: Sender " + sendingNode + " for blocks " + fromBlockNumber + " to " + toBlockNumber);
		//Logger.FULL.log(message);

		ArrayList<Block> syncResponse = new ArrayList<>();

		synchronized (ChainState.INSTANCE.getBlockchain()) {

			ArrayList<TreeNode> lastNodes = ChainState.INSTANCE.getBlockchain().getLastNodes();
			if (fromBlockNumber > lastNodes.get(0).getBlock().getBlockNumber()) {
				// we cannot answer this request, since we do not have these blocks
				Logger.FULL.log("Cannot handle request for block " + fromBlockNumber);
				return;
			}


			// changed: submit last block // old: submit all last nodes (if requested)
			//Client client = null;
			if (toBlockNumber >= ChainState.INSTANCE.getLastBlock().getBlockNumber()) {
				syncResponse.add(ChainState.INSTANCE.getLastBlock());
			}


			// submit longest branch (requested nodes) starting from parent node of last nodes
			TreeNode parent = ChainState.INSTANCE.getBlockchain().getBlockOfLongestChain(toBlockNumber);

			for (long i = parent.getBlock().getBlockNumber(); i >= fromBlockNumber; i--) {
				if (parent.getBlock().getBlockNumber() <= toBlockNumber) {
					syncResponse.add(parent.getBlock());
				}

				// Check if we can serve the request if a hash is given
				if (fromBlockHash != null && i == fromBlockNumber) {
					if (!Arrays.equals(parent.getBlock().getBlockHash(), Base64Converter.decodeToByteArray(fromBlockHash))) {
						// return that no matching block has been found
						String notFoundResponse = "<?xml version=\"1.0\"?><SyncResponse>0</SyncResponse>";
						sendResponse(notFoundResponse, output);
						return;
					}
				}

				parent = parent.getParent();
			}

		}


		Collections.reverse(syncResponse);

		if (sendingNode == null || sendingNode.equals("")) {
			// This request has been sent by the client and the response is sent synchronously

			// build response
			Element replyRoot = new Element("SyncResponse");

			for(Block b : syncResponse) {
				if (b.getBlockNumber() != 0)
					replyRoot.appendChild(new OutgoingMessageSerializer().serializeBlockToElement(b, true));
			}

			Document reply = new Document(replyRoot);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			try {
				Serializer serializer = new Serializer(out, "UTF-8");
				serializer.setLineSeparator("\n");
				serializer.write(reply);
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				// Block list is now sent synchronously
				sendResponse(out.toString(), output);
			} catch (Exception e) {
				e.printStackTrace();
				// Receiving node not found / offline
				// ignore silently
			}

		} else {
			for(Block b : syncResponse) {
				try {
					//client = new Client(p.getAddress(), p.getPort());
					//client.sendMessage(new OutgoingMessageSerializer().serializeBlockToString(parent.getBlock()));
					//client.terminateConnection();
					Logger.FULL.log("SyncResponse for blocks " + fromBlockNumber + " to " + toBlockNumber);
					//Logger.FULL.log("SyncResponse " + new OutgoingMessageSerializer().serializeBlockToString(parent.getBlock()));

					// This request is sent by another node
					NewClient.INSTANCE.sendMessage(p.getAddress(), p.getPort(), new OutgoingMessageSerializer().serializeBlockToString(b, true));

				} catch (Exception e) {
					// Receiving node not found / offline
					// ignore silently
				}
			}
		}
	}

	private void processGetAmount(Element root, PrintWriter output) {
		String assetName = root.getChildElements("assetName").get(0).getValue();
		long date = Long.valueOf(root.getChildElements("Date").get(0).getValue());

		AssetStateTable table = ChainState.INSTANCE.getAssetStateTable(assetName);
		Element replyRoot;
		if (table == null) {
			replyRoot = new Element("Error");
		}else {

			Set<String> participants = ((HashMap<String, Integer>) table.getAssets(date)).keySet();

			replyRoot = new Element("AmountResponse");

			for (String s : participants) {
				//TODO: Return the distribution of assets at a given time
			}
		}


		Document replyDoc = new Document(replyRoot);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			Serializer serializer = new Serializer(out, "UTF-8");
			serializer.setLineSeparator("\n");
			serializer.write(replyDoc);

			sendResponse(out.toString(), output);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void processMessage(String message, PrintWriter output) {
		//Logger.FULL.log("DEBUG: " + message);
		if (message == null || message.equals("")) {
			Logger.FULL.logImportant("Discarded empty message");
			return;
		}
		try {
			Builder parser = new Builder();
			Document doc = parser.build(message, null);

			Element root = doc.getRootElement();
			String type = root.getLocalName();

			Logger.FULL.logImportant("Received message of type: " + type);

			if (type.equals("Status"))
				processStatus(root, output);
			else if (type.equals("BlockList"))
				processBlockList(root, output);
			else if (type.equals("Block"))
				processBlock(message);
			 else if (type.equals("ExternalTx") || type.equals("ExternalUtilityTx"))
				processTransaction(message, true, output);
			 else if (type.equals("Tx") || type.equals("UtilityTx"))
				processTransaction(message, false, output);
			 else if (type.equals("Request"))
				processRequest(root, output);
			 else if (type.equals("GetAmount"))
				processGetAmount(root, output);

		} catch (ParsingException | IOException e) {
			e.printStackTrace();
		}
	}

}