/*
* This file is part of ResselChain.
* Copyright Center for Secure Energy Informatics 2018
* Fabian Knirsch, Andreas Unterweger, Clemens Brunner
* This code is licensed under a modified 3-Clause BSD License. See LICENSE file for details.
*/

package at.entrust.resselchain.statetable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import at.entrust.resselchain.chain.Transaction;
import at.entrust.resselchain.logging.Logger;
import at.entrust.resselchain.state.ChainState;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class AssetStateTable {

	// holds a MySQL database for this table

	private String assetName;
	private ArrayList<String> assetParticipants = new ArrayList<>();

	private Connection connection = null;


	public AssetStateTable(String assetName, ArrayList<String> participantNames, HashMap<String, Integer> participantNameShares) {

	}


	public boolean revertTransaction(Transaction tx) {
		//TODO: implement your own logic to revert a transaction
		// If revert process is valid return true, if not return false
		return true;
	}

	public boolean processTransaction(Transaction tx) {
		//TODO: implement your own logic to process a transaction
		// If transaction is valid return true, if not return false
		return false;
	}

	public static void restoreBackup() {
		//TODO: implement your own backup restore
	}

	public static HashMap<String, AssetStateTable> createStateFromStorage() {
		//TODO: implement your own backup restore
		// The hashmap consists of the asset name and an assetstatetable where the distribution is stored
		return new HashMap<String, AssetStateTable>();
	}

	public static void backupTable() {
		//TODO: implement a backup mechanism to backup the current state
	}

    public HashMap<String, Integer> getAssets(long date) {

		//TODO: return the asset distribution at a given timestamp
		return new HashMap<String, Integer>();
	}


	public ArrayList<Transaction> processMultipleTransactions(ArrayList<Transaction> txs) throws AtomicTransactionException {

		ArrayList<Transaction> invalidTx = new ArrayList<>();
		int i = 0;
		while (i < txs.size() && processTransaction(txs.get(i)))
			i++;
		if (i >= txs.size())
			return invalidTx; // returns empty list if all Tx are valid
		else
		{
			Logger.FULL.log("Failed trying to process " + txs.size() + " Tx, failed at index " + i);
			invalidTx.add(txs.get(i)); // returns the first Tx that is invalid

			i--; // do not revert last transaction since it failed
			while (i >= 0 && revertTransaction(txs.get(i)))
				i--;
			if (i != -1) throw new AtomicTransactionException(); //This should never happen

			return invalidTx;
		}
	}

	public static HashMap<String,AssetStateTable> createAssetStateTablesFromMeta() {
		return new HashMap<>();
	}
}