/*
* This file is part of ResselChain.
* Copyright Center for Secure Energy Informatics 2018
* Fabian Knirsch, Andreas Unterweger, Clemens Brunner
* This code is licensed under a modified 3-Clause BSD License. See LICENSE file for details.
*/

package at.entrust.resselchain.statetable;

import java.sql.*;
import java.util.ArrayList;
import java.util.TreeSet;

import at.entrust.resselchain.chain.Transaction;
import at.entrust.resselchain.logging.Logger;
import at.entrust.resselchain.utils.Base64Converter;

public class TxStateTable {
	
//	private TreeSet<String> txList = new TreeSet<>();
	private TreeSet<String> txListBackup = new TreeSet<>();

	private Connection connection = null;

	public TxStateTable(){
		reinitConnection();
		if(!createTableIfNotExists())
			System.exit(1);
	}



	public void addTx(ArrayList<Transaction> tx) {
		for (Transaction t : tx)
			addTx(t);
	}
	
	public void removeTx(ArrayList<Transaction> tx) {
		for (Transaction t : tx)
			removeTx(t);
	}
	
	public void addTx(Transaction tx) {
		String hash = Base64Converter.encodeFromByteArray(tx.getTransactionHash());;
		if (!containsTxHash(hash)) {
			addTxHash(hash);
		}
	}
	
	public boolean containsTx(Transaction tx) {
		String hash = Base64Converter.encodeFromByteArray(tx.getTransactionHash());
		return containsTxHash(hash);
	}
	
	public void removeTx(Transaction tx) {
		String hash = Base64Converter.encodeFromByteArray(tx.getTransactionHash());
		removeTxHash(hash);
	}

	//SQL Queries
	synchronized private String createTableSQLString(){
		String sql = "CREATE TABLE IF NOT EXISTS 'txstates' " +
				"(hash TEXT PRIMARY KEY)";
		return sql;
	}

	synchronized private String addTxHashSQLString(String hash){
		String sql = "INSERT INTO 'txstates' (hash) VALUES ('" + hash + "')";
		return sql;
	}

	synchronized private String removeTxHashSQLString(String hash){
		String sql = "DELETE FROM 'txstates' WHERE hash = '" + hash + "'";
		return sql;
	}

	synchronized private String getTxHashSQLString(String hash){
		String sql = "SELECT * FROM 'txstates' WHERE hash = '" + hash  + "'";
		return sql;
	}
	//END SQL Queries


	synchronized private boolean createTableIfNotExists() {
		int ret = executeUpdateQuery(createTableSQLString());
		if (ret == -1) {
			Logger.FULL.log("Table for txstates could not be created");
			return false;
		}
		Logger.FULL.log("Table for txstates created successfully");
		return true;
	}

	synchronized private boolean addTxHash(String hash){
		int ret = executeUpdateQuery(addTxHashSQLString(hash));
		if (ret == -1) {
			Logger.FULL.log("Hash could not be added to txstates");
			return false;
		}
		Logger.FULL.log("Hash successfully added to txstates");
		return true;
	}

	synchronized private boolean removeTxHash(String hash){
		int ret = executeUpdateQuery(removeTxHashSQLString(hash));
		if (ret == -1) {
			Logger.FULL.log("Hash could not be deleted from txstates");
			return false;
		}
		Logger.FULL.log("Hash successfully added to txstates");
		return true;
	}

	synchronized private boolean containsTxHash(String hash){
		ResultSet result = executeSelectQuery(getTxHashSQLString(hash));
		boolean contains = false;
		try {
			contains = result.next();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		try {
			result.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return contains;
	}

	synchronized private void reinitConnection() {
		try
		{
			if (connection != null && !connection.isClosed())
				close();
		}
		catch ( Exception e ) {
			connection = null;
			Logger.ERR.log("Error determining status of SQL connection for tx state table: " + e.getMessage());
		}
		try
		{
			Class.forName("org.sqlite.JDBC");
			connection = DriverManager.getConnection("jdbc:sqlite:transactionstates.db");
		} catch ( Exception e ) {
			connection = null;
			Logger.ERR.log("Error opening SQL connection for tx state table: " + e.getMessage());
			Logger.ERR.log("Exiting after failure to connect to SQL data base.");
			System.exit(1);
		}
	}

	synchronized public void close() {
		try {
			if (connection != null)
				connection.close();
		} catch (SQLException e) {
			Logger.ERR.log("Error closing SQL connection for Tx State tables " + e.getMessage());
		}
	}

	synchronized private ResultSet executeSelectQuery(String query) //Warning: caller must call ResultSet.close()!
	{
		ResultSet ret = null;
		try
		{
			if (!connection.isValid(5))
				reinitConnection();
			Statement stmt = connection.createStatement();
			ret = stmt.executeQuery(query);
			stmt.closeOnCompletion();
			//Logger.FULL.log("Executed SQL query %" + query + "% for asset " + assetName);
			return ret;
		} catch ( Exception e ) {
			Logger.ERR.log("Error executing SQL query " + query + ":" + e.getMessage());
			return null;
		}
	}

	synchronized private int executeUpdateQuery(String query)
	{
		int ret = -1;
		try
		{
			if (!connection.isValid(5)) //5 second timeout
				reinitConnection();
			Statement stmt = connection.createStatement();
			ret = stmt.executeUpdate(query);
			stmt.close();
			//Logger.FULL.log("Executed SQL update %" + query + "% for asset " + assetName);
		} catch ( Exception e ) {
			ret = -1;
			Logger.ERR.log("Error executing SQL update %" + query + ": " + e.getMessage());
		}
		return ret;
	}
}
