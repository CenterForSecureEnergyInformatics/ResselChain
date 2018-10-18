/*
* This file is part of ResselChain.
* Copyright Center for Secure Energy Informatics 2018
* Fabian Knirsch, Andreas Unterweger, Clemens Brunner
* This code is licensed under a modified 3-Clause BSD License. See LICENSE file for details.
*/

package at.entrust.resselchain.state;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import at.entrust.resselchain.chain.Block;
import at.entrust.resselchain.chain.Participant;
import at.entrust.resselchain.chain.Transaction;
import at.entrust.resselchain.chain.UtilityTransaction;
import at.entrust.resselchain.communication.SyncClient;
import at.entrust.resselchain.config.GlobalConfig;
import at.entrust.resselchain.logging.Logger;
import at.entrust.resselchain.statetable.AssetStateTable;
import at.entrust.resselchain.statetable.TxStateTable;
import at.entrust.resselchain.tree.InvalidBlockOrderException;
import at.entrust.resselchain.tree.Tree;
import at.entrust.resselchain.tree.TreeNode;

public enum ChainState {
	INSTANCE;
	
	private Tree blockchain = new Tree();
	private HashMap<String, AssetStateTable> assetStates = new HashMap<>();

	private TxStateTable txStates = new TxStateTable();
	private ConcurrentLinkedQueue<Transaction> unconfirmedTransactions = new ConcurrentLinkedQueue<>();

	private HashMap<String, Participant> participants = new HashMap<>();
	private String status = "";
	private ArrayList<String> lastBlockRequests = new ArrayList<>();

	private long lastResetTime = System.currentTimeMillis();

	
	public AssetStateTable getAssetStateTable(String assetName) {
		return assetStates.get(assetName);
	}

	public TxStateTable getTxStateTable() {
		return txStates;
	}

	public HashMap<String, AssetStateTable> getassetStateTables() {
		return assetStates;
	}


	public ArrayList<String> getLastBlockRequests() {
		return lastBlockRequests;
	}
	
	public void setStatus(String status) {
		synchronized(status) {
			this.status = status;
		}
	}
	
	public String getStatus() {
		synchronized(status) {
			return status;
		}
	}

	// Warning: This method is not thread safe, only used in Viewer
	public Tree getBlockchain() {
		return blockchain;
	}
	
	// only called once for bootstrapping
	public void addTxState(ArrayList<Transaction> tx) {
		txStates.addTx(tx);
	}
	
	public void addUnconfirmedTransaction(Transaction tx) {
		// add new Tx if not already in list
		// TODO: sufficient to synchronize here?
		synchronized(unconfirmedTransactions) {
			if (!unconfirmedTransactions.contains(tx))
				unconfirmedTransactions.add(tx);
		}
	}
	
	public void removeUnconfirmedTransaction(Transaction tx) {
		unconfirmedTransactions.remove(tx);
	}
	
	public int getUnconfirmedTransactionCount() {
		return unconfirmedTransactions.size();
	}
	
	public void addParticipant(Participant p) throws ParticipantsAlreadyExistsException {
		if (participants.containsKey(p.getName()))
			throw new ParticipantsAlreadyExistsException("A participant with this name already exists.");
		
		participants.put(p.getName(), p);
	}
	
	public boolean addAssetStateTable(String assetName, ArrayList<Participant> participants, HashMap<Participant, Integer> participantAmounts) {
		
		ArrayList<String> participantNames = new ArrayList<>();
		for (Participant p : participants)
			participantNames.add(p.getName());
		
		HashMap<String, Integer> participantNameShares = new HashMap<>();
		for (Participant p : participantAmounts.keySet()) {
			participantNameShares.put(p.getName(), participantAmounts.get(p));
		}
		
		AssetStateTable assetState;
		try {
			assetState = new AssetStateTable(assetName, participantNames, participantNameShares);
			assetStates.put(assetName, assetState);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public void addAssetStateTable(String name, AssetStateTable table) {
		assetStates.put(name, table);
	}
	
	public ArrayList<Participant> getAllParticipants() {
		return new ArrayList<Participant>(participants.values());
	}

	public ArrayList<Participant> getAllOtherParticipants() {
		ArrayList<Participant> ps = getAllParticipants();
		String nodeName = GlobalConfig.INSTANCE.PARTICIPANT_CONFIG.getName();
		List<Participant> filteredParticipants = ps.stream()
				.filter(p -> !p.getName().equals(nodeName))
				.collect(Collectors.toList());
		return (ArrayList<Participant>)filteredParticipants;
	}
	
	public Participant getParticipantByName(String name) {
		return participants.get(name);
	}
	
	public ArrayList<Transaction> getUnconfirmedTransactions(int count) {
		// we do not need a deep copy here, since the objects are moved from
		// one list to another and all methods are synchronized
		if (count < 1) throw new IllegalArgumentException("You cannot pop zero elements from the list.");
		
		// copy top count elements (do remove from queue)
		ArrayList<Transaction> tmp = new ArrayList<>();
		for(int i = 0; i < count; i++) {
			Transaction t = unconfirmedTransactions.poll(); // this removes the element from the queue
			if (t != null) {
				tmp.add(t);
			}
		}
		//QUESTION: Why adding again
		for (Transaction t : tmp)
			unconfirmedTransactions.add(t); // add element to queue again
		
		return tmp;
	}
	
	public boolean existsUnconfirmedTransaction(Transaction tx) {
		return unconfirmedTransactions.contains(tx);
	}


	public static Block getParentForBlock(Block b) {
		if (b.getBlockNumber() < 1) {
			System.out.println(Thread.currentThread().getStackTrace().toString());
			System.exit(0);
		};

		ArrayList<TreeNode> nodes = TreeNode.getSiblings(b.getBlockNumber() -1);
		for(TreeNode n  : nodes)
			if (Arrays.equals(n.getBlock().getBlockHash(), b.getPreviousBlockHash()))
				return n.getBlock();

		return null;
	}

	public Block getLastBlock() {
		synchronized(blockchain) {
			ArrayList<TreeNode> children = blockchain.getLastNodes();
			// choose last node with lowest hash

			if (children.size() == 0) // no blocks at all
				return null;

			BigInteger lowestHash = new BigInteger(children.get(0).getBlock().getBlockHash());
			Block block = children.get(0).getBlock();
			for(int i = 1; i < children.size(); i++) {
				BigInteger nodeHash = new BigInteger(children.get(i).getBlock().getBlockHash());
				if (nodeHash.compareTo(lowestHash) == -1) {
					lowestHash = nodeHash;
					block = children.get(i).getBlock();
				}
			}
			return block;
		}
	}

	public boolean appendBlock(Block block){
		synchronized (blockchain) {
			try {
				// get previous last block
				Block lastBlockInPreviousLongestChain = getLastBlock();
				// try append block
				blockchain.appendRevertableBlockByBlockNumber(block);
				// get current last block
				Block lastBlockInCurrentLongestChain = getLastBlock();
				// if block successfully appended, revert transactions in abandoned branch

				if (lastBlockInPreviousLongestChain != null) //null is genesis block
					processTransactions(lastBlockInPreviousLongestChain, lastBlockInCurrentLongestChain);

				Logger.STD.log("Block added: Block# " + block.getBlockNumber() + ", Timestamp " + new SimpleDateFormat("HH:mm:ss").format(block.getTimestamp()));
				return true;
			} catch (InvalidBlockOrderException e) {
				// something went wrong when inserting this block, reverse
				blockchain.revertLastBlockInsertion();
				return false;
			}
		}
	}

	public void startSync(Block b){
		synchronized (blockchain){
			Logger.STD.log("Start syncing with: " + b.getMiner());

			byte[] fromHash = null;

			ArrayList<Block> blocks = new ArrayList<>();
			long from = getLastBlock().getBlockNumber();
			Participant miner = getParticipantByName(b.getMiner());

			do {
				from -= GlobalConfig.INSTANCE.SYNC_BLOCK_REQUEST_OFFSET;
				if (from <= 0) from = 0;
				try {
					fromHash = ChainState.INSTANCE.blockchain.getBlockHash(from);
				} catch (InvalidBlockOrderException e) {
					e.printStackTrace();
					return;
				}
				Logger.STD.log("Send Request to " + miner.getName() + " from: " + from);

				blocks = SyncClient.getBlocks(miner, from, fromHash);
				if (blocks == null){
					Logger.STD.log("Sync failed...");
					break;
				}

				if (blocks.size() == 0) {
					Logger.STD.log("Block sync response is empty try again... ");
				}else {
					if (addSyncBlocks(blocks)) {
						break;
					} else {
						System.out.println("Does not make sense not possible...");
						blocks.clear(); //todo speed up keep blocks and request new with from-towa
						break;
					}
				}
			} while (blocks.size() == 0);
		}
	}

	private boolean addSyncBlocks(ArrayList<Block> blocks){
		Collections.sort(blocks, (o1, o2) -> Long.compare(o1.getBlockNumber(),o2.getBlockNumber()));

		for (Block b : blocks) {
            if (!appendBlock(b))
                return false;
        }
		return true;
	}

	private Block getBlockByBlockNumber(Block block, long blockNumber){
		while (block.getBlockNumber() > 0) {
			if (block.getBlockNumber() == blockNumber)
				return block;
			block = getParentForBlock(block);
		}
		return blockchain.getRootNode().getBlock();
	}

	private Block findCommonParentNode (Block b1, Block b2){
		long smallestBlockNumber = Math.min(b1.getBlockNumber(), b2.getBlockNumber());
		b1 = getBlockByBlockNumber(b1, smallestBlockNumber);
		b2 = getBlockByBlockNumber(b2, smallestBlockNumber);

		while (!b1.equals(b2)){
			b1 = getParentForBlock(b1);
			b2 = getParentForBlock(b2);
		}

		return b1;
	}

	private ArrayList<Transaction> getAllTransactions(Block from, Block to){
		ArrayList<Transaction> txs = new ArrayList<>();

		while (!to.equals(from)){
			ArrayList<Transaction> tmpTxs = (ArrayList<Transaction>) to.getTransactions().clone();
			Collections.reverse(tmpTxs);
			txs.addAll(tmpTxs);
			to = getParentForBlock(to);
		}
		Collections.reverse(txs);
		return txs;
	}


	private void processTransactions(Block lastBlockOld, Block lastBlockNew) throws InvalidBlockOrderException {
		if (lastBlockOld.equals(lastBlockNew)) return;

		backupProcessTransaction();

		System.out.println("ProcessTransaction ("+lastBlockOld.getBlockNumber()+"," + lastBlockNew.getBlockNumber() + ")");

		Block commonParent = findCommonParentNode(lastBlockOld, lastBlockNew);

		ArrayList<Transaction> txsOldBlock = getAllTransactions(commonParent, lastBlockOld);
		Collections.reverse(txsOldBlock);

		ArrayList<Transaction> txsNewBlock = getAllTransactions(commonParent, lastBlockNew);

		Logger.FULL.log("Append operation requires " + txsOldBlock.size() + " Tx to revert and " + txsNewBlock.size() + " Tx to process before appending new block");

		processTxToRevert(txsOldBlock, txsNewBlock);

		if (!processTxToProcess(txsNewBlock)) {
			restoreProcessTransaction();
			throw new InvalidBlockOrderException();
		}

		// remove successfully processed Tx from pool of unconfimred Tx
		unconfirmedTransactions.removeAll(txsNewBlock);

		Logger.FULL.log("Append operation completed.");

	}


	
	private void backupProcessTransaction(){
		AssetStateTable.backupTable();
	}

	private void restoreProcessTransaction(){
		AssetStateTable.restoreBackup();
		HashMap<String, AssetStateTable> tables = AssetStateTable.createStateFromStorage();
		assetStates.clear();
		for (String s : tables.keySet())
			addAssetStateTable(s, tables.get(s));
	}

	public boolean processTxToProcess(ArrayList<Transaction> txToProcess) {
		for(Transaction tx : txToProcess) {
			String assetName = tx.getAssetName();
			if (!(tx instanceof UtilityTransaction)) {
				// check if asset name exists (UtilityTx has already been sent)
				if (!assetStates.containsKey(assetName)) {
					Logger.STD.log("Invalid asset Name: " + assetName + ". Block is discarded.");
					return false;
				} else {
					AssetStateTable table = getAssetStateTable(assetName);
					if (table != null) {
						table.processTransaction(tx);
					}
				}
				// check if Tx already in chain
				if (txStates.containsTx(tx)) {
					Logger.STD.log("Tx already in chain (Sender: " + tx.getSender() + ", Receiver: " + tx.getReceiver() + ", AssetName: " + tx.getAssetName() +  ", Amount: " + tx.getAmount() + ". Block is discarded.");
					return false;
				}else {
					txStates.addTx(tx);
					unconfirmedTransactions.remove(tx);
				}
			} else {
				UtilityTransaction utx = (UtilityTransaction)tx;
				if (addAssetStateTable(utx.getAssetName(), utx.getParticipants(), utx.getParticipantShares())) {
					Logger.STD.log("New asset state table created. Asset name: " + assetName);
				}
				else {
					Logger.STD.log("New asset state table cannot be created: " + assetName + ". Discarding block.");
					return false;
				}
			}
		}
		return true;
	}
	
	public void processTxToRevert(ArrayList<Transaction> txToRevert, ArrayList<Transaction> txToProcess) {
		for(Transaction tx : txToRevert) {

			if (!(tx instanceof UtilityTransaction)) {
				String assetName = tx.getAssetName();
				AssetStateTable table = getAssetStateTable(assetName);
				if (table != null) {
					table.revertTransaction(tx);
				}
			}

			// add Tx that are reverted, but not in he to process list to pool of unconfirmed Tx
			if (!txToProcess.contains(tx))
				unconfirmedTransactions.add(tx);

			txStates.removeTx(tx);
		}

		/*
		 * TODO: drop tables for utility Tx when reverting (possibly outside this function)
		 * */
	}
}
