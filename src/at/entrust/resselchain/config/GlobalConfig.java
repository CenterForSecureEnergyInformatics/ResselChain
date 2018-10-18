/*
* This file is part of ResselChain.
* Copyright Center for Secure Energy Informatics 2018
* Fabian Knirsch, Andreas Unterweger, Clemens Brunner
* This code is licensed under a modified 3-Clause BSD License. See LICENSE file for details.
*/

package at.entrust.resselchain.config;

import java.io.File;

import at.entrust.resselchain.logging.Logger;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;

public enum GlobalConfig {
	INSTANCE;
	
	private GlobalConfig() {
		// load config from properties.xml if available
		try {
			File f = new File("properties.xml");
			if (f.exists()) {
				Builder parser = new Builder();
				Document doc = parser.build(f);
				
				Element root = doc.getRootElement();
				TRANSACTIONS_PER_BLOCK = Integer.valueOf(root.getChildElements("TransactionsPerBlock").get(0).getValue());
				MINING_TRIALS_PER_BLOCK = Integer.valueOf(root.getChildElements("MiningTrialsPerBlock").get(0).getValue());
				REQUEST_LIMIT = Integer.valueOf(root.getChildElements("RequestLimit").get(0).getValue().toLowerCase());
				MINING_TRIALS_PER_BLOCK = Integer.valueOf(root.getChildElements("MiningTrialsPerBlock").get(0).getValue());
				DIFFICULTY = Integer.valueOf(root.getChildElements("Difficulty").get(0).getValue());
				MINING_THREAD_SLEEP_MILISECONDS = Integer.valueOf(root.getChildElements("MiningThreadSleepMiliseconds").get(0).getValue());
				COMMUNICATION_THREAD_SLEEP_MILISECONDS = Integer.valueOf(root.getChildElements("CommunicationThreadSleepMiliseconds").get(0).getValue());
				NUM_LAST_BLOCK_REQUESTS_LOGGED = Integer.valueOf(root.getChildElements("NumLastBlockRequestsLogged").get(0).getValue());
				HASH_ALOGITHM = root.getChildElements("HashAlgorithm").get(0).getValue();
				SIGNATURE_ALGORITHM = root.getChildElements("SignatureAlgorithm").get(0).getValue();
				SSL_SOCKET_CIPHER_SUITE = root.getChildElements("SSLSocketCipherSuite").get(0).getValue();
				PKSK_ALGORITHM = root.getChildElements("PKSKAlgorithm").get(0).getValue();
				PKSK_KEY_SIZE = Integer.valueOf(root.getChildElements("PKSKKeySize").get(0).getValue());
				LONGEST_CHAIN_STATE_OFFSET = Long.parseLong(root.getChildElements("LongestChainStateOffset").get(0).getValue());
				LONGEST_CHAIN_STATE_WINDOW = Long.parseLong(root.getChildElements("LongestChainStateWindow").get(0).getValue());
				SOCKET_READ_TIMEOUT_MILISECONDS = Integer.valueOf(root.getChildElements("SocketReadTimeoutMiliseconds").get(0).getValue());
				MINE_EMPTY_BLOCKS = Boolean.valueOf(root.getChildElements("MineEmptyBlocks").get(0).getValue());
				SYNC_BLOCK_REQUEST_OFFSET=Long.parseLong(root.getChildElements("SyncBlockRequestOffeset").get(0).getValue());
				DEBUG=Boolean.valueOf(root.getChildElements("Debug").get(0).getValue());

			} else {
				Logger.FULL.log("No properties.xml file found. Using default properties.");
			}
		} catch (Exception e) {
			Logger.ERR.log("Error while reading properties.xml: ");e.printStackTrace();
			System.exit(1);
		}
	}
	
	// Maximum number of transactions per block
	public int TRANSACTIONS_PER_BLOCK = 300; //10;
	
	// Total trials for mining a new block
	public int MINING_TRIALS_PER_BLOCK = 0xFFFFFFF;
	
	// Maximum number of unappendable blocks before requesting missing blocks from peer
	public int REQUEST_LIMIT = 3; //5;
	
	// Difficulty for mining
	// set this to 20 for approx. 5 minutes/block on Raspberry Pi 3
	public int DIFFICULTY = 22;//22;
	
	// Port this node listens for incoming messages (is set by config.xml at startup)
	public int INCOMING_MESSAGE_PORT = -1; //22222;
	
	// Seconds for the mining thread to sleep before attempting to mine a new block
	public int MINING_THREAD_SLEEP_MILISECONDS = 10;
	
	// Miliseconds for the communication threads to sleep before attempting to read or send messages
	public int COMMUNICATION_THREAD_SLEEP_MILISECONDS = 1;
	
	// Number of last block requests (ranges from - to) to be logged for status info 
	public int NUM_LAST_BLOCK_REQUESTS_LOGGED = 3;
	
	// Name of the hash algorithm for the block hash / mining
	public String HASH_ALOGITHM = "SHA-256";
	
	// Name of the signature algorithm
	public String SIGNATURE_ALGORITHM = "SHA256withRSA";
	
	// Name of the SSL socket cypher suite
	public String SSL_SOCKET_CIPHER_SUITE = "TLS_ECDH_anon_WITH_AES_256_CBC_SHA"; // "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384";
	
	// Name of the algorithm for the private/public key encryption scheme
	// Note: Must match the algorithm specified in SIGNATURE_ALGORITHM
	public String PKSK_ALGORITHM = "RSA";
	
	// Key size for the private/public key encryption scheme
	public int PKSK_KEY_SIZE = 1024;
	
	// build and version
	// TODO: use Ant TStamp
	public String BUILD = "19.10.2017 3";
	
	// Configuration of this node as Participant object; is set at startup by main method
	public ParticipantConfig PARTICIPANT_CONFIG = null;
	
	// Number of last blocks in longest chain not reported in longest chain state
	public long LONGEST_CHAIN_STATE_OFFSET = 0;
	
	// Number of blocks in longest chain reported starting from offset
	public long LONGEST_CHAIN_STATE_WINDOW = 25;
	
	// Socket read timeout
	public int SOCKET_READ_TIMEOUT_MILISECONDS = 5000;
	
	// Allow mining of blocks without Tx
	public boolean MINE_EMPTY_BLOCKS = true;

	// Number of tree levels requested in addition to sync offset 
	public long SYNC_BLOCK_REQUEST_OFFSET =  10;

	//Print log to std out
	public boolean DEBUG = true;
	
}
