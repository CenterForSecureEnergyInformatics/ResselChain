/*
* This file is part of ResselChain.
* Copyright Center for Secure Energy Informatics 2018
* Fabian Knirsch, Andreas Unterweger, Clemens Brunner
* This code is licensed under a modified 3-Clause BSD License. See LICENSE file for details.
*/

package at.entrust.resselchain.mining;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import at.entrust.resselchain.chain.Block;
import at.entrust.resselchain.chain.OutgoingMessageSerializer;
import at.entrust.resselchain.chain.Participant;
import at.entrust.resselchain.chain.Transaction;
import at.entrust.resselchain.chain.UtilityTransaction;
import at.entrust.resselchain.communication.NewClient;
import at.entrust.resselchain.config.GlobalConfig;
import at.entrust.resselchain.config.NodeNotInitializedException;
import at.entrust.resselchain.logging.Logger;
import at.entrust.resselchain.state.ChainState;
import at.entrust.resselchain.statetable.AtomicTransactionException;
import at.entrust.resselchain.statetable.AssetStateTable;
import at.entrust.resselchain.utils.Sign;

public class Miner implements Runnable {

	private static Thread miningThread = null;
	private static AtomicBoolean isStopped = new AtomicBoolean(true);
	
	public static void startMining() {
		if (isStopped.get() == true) {
			miningThread = new Thread(new Miner());
			miningThread.setName("Miner");
			miningThread.start();
		}
	}
	
	public static void stopMining() {
		isStopped.set(true);
		if (miningThread != null)
			miningThread.interrupt();
	}
	
	@Override
	public void run() {
		isStopped.set(false);
		while(!isStopped.get()) {
			// the mining routine is running all time and waits for new transactions
			try {
				// idle some seconds
				Thread.sleep(GlobalConfig.INSTANCE.MINING_THREAD_SLEEP_MILISECONDS);

				// try to mine a block
				Block block = mineBlock();
				if (block != null) {
					// check if the freshly mined block is still valid as the newest block
					// without this check there will be a lot of branching
					long currentLastBlockNumber = ChainState.INSTANCE.getLastBlock().getBlockNumber();
					if (block.getBlockNumber() == currentLastBlockNumber + 1) {
						// if successfully minded, append to own tree and propagate to network
						Logger.STD.logImportant("Block mined: Block# " + block.getBlockNumber() + ", Timestamp " + new SimpleDateFormat("HH:mm:ss").format(block.getTimestamp()));

						boolean appended = ChainState.INSTANCE.appendBlock(block);

						if (appended == false)
							continue; // do not propagate unappendable blocks

						// send new block to all known participants
						//Client client = null;
						ArrayList<Participant> participants = ChainState.INSTANCE.getAllOtherParticipants();
						String serializedBlock = new OutgoingMessageSerializer().serializeBlockToString(block);
						for (Participant p : participants) {
							Logger.STD.logImportant("Send block to " + p.getName());
							NewClient.INSTANCE.sendMessage(p.getAddress(), p.getPort(), serializedBlock);
						}
					}
				}
			} catch (InterruptedException e) {

			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (InvalidKeyException e) {
				e.printStackTrace();
			} catch (SignatureException e) {
				e.printStackTrace();
			} catch (NodeNotInitializedException e) {
				e.printStackTrace();
			} catch (AtomicTransactionException e) {
				e.printStackTrace();
			}
		}
	}

	public Block mineBlock() throws NodeNotInitializedException, InvalidKeyException, NoSuchAlgorithmException, SignatureException, AtomicTransactionException {

		// check node configuration
		PrivateKey sk = GlobalConfig.INSTANCE.PARTICIPANT_CONFIG.getPrivateKey();
		if (sk == null)
			throw new NodeNotInitializedException("Private key not initialized for this node.");
		PublicKey pk = GlobalConfig.INSTANCE.PARTICIPANT_CONFIG.getPublicKey();
		if (pk == null)
			throw new NodeNotInitializedException("Public key not initialized for this node.");
		String miner = GlobalConfig.INSTANCE.PARTICIPANT_CONFIG.getName();
		if (miner == null)
			throw new NodeNotInitializedException("Name not initialized for this node.");
		
		Block previousBlock = ChainState.INSTANCE.getLastBlock();
		long blockNumber = previousBlock.getBlockNumber() + 1; // genesis block has block number 0
		byte[] previousBlockhash = previousBlock.getBlockHash();
		int difficulty = GlobalConfig.INSTANCE.DIFFICULTY; // TODO: For testing difficulty is set to 2; later we will adapt this value base don the block rate
		
		ArrayList<Transaction> transactions = ChainState.INSTANCE.getUnconfirmedTransactions(GlobalConfig.INSTANCE.TRANSACTIONS_PER_BLOCK);
		
		// only check if empty blocks are not allowed
		if (transactions.size() == 0 && GlobalConfig.INSTANCE.MINE_EMPTY_BLOCKS == false) {
			return null;
			//throw new NothingToMineException("No unconfirmed transactions found. Nothing to mine.");
		}
		
		ArrayList<Transaction> tmpTx = new ArrayList<>();
		
		Logger.FULL.log("Mining thread fetched " + transactions.size() + " Tx for mining");
		// check if transaction is valid: call processMultipleTransactions in AssetStateTable and check return value is successful, if successful, revert transactions and mine block (once mined, transactions are written to the AssetStateTable upon appendBlock call)
		// if a transaction is invalid, remove from list of unconfirmed Tx -> happens when block is appended
		boolean isUtilityTxBlock = false;
		HashMap<String, ArrayList<Transaction>> tableList = new HashMap<>();
		for (Transaction t : transactions) {
			if (t instanceof UtilityTransaction) {
				// append utility transactions immediately
				tmpTx.add(t);
				isUtilityTxBlock = true;
				continue;
			}

			if (isUtilityTxBlock) continue;

			String assetName = t.getAssetName();
			if (!tableList.containsKey(assetName))
				tableList.put(assetName, new ArrayList<>());

			tableList.get(assetName).add(t);
		}

		if (!isUtilityTxBlock) {
			for (String assetName : tableList.keySet()) {
				AssetStateTable stateTable = ChainState.INSTANCE.getAssetStateTable(assetName);
				ArrayList<Transaction> invalidTx = stateTable.processMultipleTransactions(tableList.get(assetName));
				if(invalidTx.size() == 0) {
					for (Transaction tx : tableList.get(assetName)) {
						stateTable.revertTransaction(tx);
						tmpTx.add(tx); // Tx is valid, add for mining
					}
				} else {
					ChainState.INSTANCE.removeUnconfirmedTransaction(invalidTx.get(0)); // remove first invalid Tx from list of unconfirmed Tx
				}
			}
		}
		

		Logger.FULL.log("Mining thread successfully processed " + tmpTx.size() + " Tx for mining");
		
		if (tmpTx.size() == 0 && GlobalConfig.INSTANCE.MINE_EMPTY_BLOCKS == false) {
			return null;
		}
		
		// Start mining
		int miningTrial = 0;
		BigInteger hashLimit = BigInteger.valueOf(2).pow(254 - difficulty);


		long nonce = new Random().nextLong();

		Block b = new Block(System.currentTimeMillis(), blockNumber, nonce, miner, difficulty, previousBlockhash);
		for (Transaction tx : tmpTx) {
			b.addTransaction(tx);
		}
		byte[] hash = null;
		do {
			if (miningTrial % 100 == 0)
				try {
					Thread.sleep(0);
				} catch (InterruptedException e) {}

			nonce++;
			nonce = Math.abs(nonce); // use positive nonces only
			b.setNonce(nonce);
			hash = b.getBlockHash();
			
			if (++miningTrial >= GlobalConfig.INSTANCE.MINING_TRIALS_PER_BLOCK)
				return null;
			if (isStopped.get())
			    return null;
			
		} while (new BigInteger(1, hash).abs().compareTo(hashLimit) == 1);
		
		// Sign block with this user's private key
		b = Sign.signBlock(b, sk);

		return b;
	}
}
