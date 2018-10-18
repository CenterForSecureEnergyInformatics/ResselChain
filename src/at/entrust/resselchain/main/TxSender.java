/*
* This file is part of ResselChain.
* Copyright Center for Secure Energy Informatics 2018
* Fabian Knirsch, Andreas Unterweger, Clemens Brunner
* This code is licensed under a modified 3-Clause BSD License. See LICENSE file for details.
*/

package at.entrust.resselchain.main;

import java.io.File;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;

import at.entrust.resselchain.chain.OutgoingMessageSerializer;
import at.entrust.resselchain.chain.Transaction;
import at.entrust.resselchain.communication.Client;
import at.entrust.resselchain.communication.NewClient;
import at.entrust.resselchain.config.GlobalConfig;
import at.entrust.resselchain.config.ParticipantConfig;
import at.entrust.resselchain.logging.Logger;
import at.entrust.resselchain.utils.Base64Converter;
import at.entrust.resselchain.utils.Sign;
import at.entrust.resselchain.utils.TimeSlots;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;

public class TxSender {

	private static void showUsage() {
		System.out.println("Ressel Chain Transaction Sender (TxSender)");
		System.out.println("Transfers a asset amount from a sender to a receipient");
		System.out.println("Usage: TxSender -h | -t | (-sraw <Node Address> <Node Port> <Sender Name> <Sender Private Key> <Receipient Name>) | (-sxml <Sender Node XML Config> <Receiver Node XML Config>) <asset Name> <Date> <Time Slot> <Amount> [<Number of Slots>=1]");
		System.out.println("-h : display help");
		System.out.println("-t : print current timestamp");
		System.out.println("-sraw : send raw transaction, all following arguments are mandatory");
		System.out.println("-sxml : send transaction with XML node config files, all following arguments are mandatory");
		System.out.println("<Node Address> <Node Port> : IP address and port of the node that initially receives this transaction");
		System.out.println("<Sender Name> : Name of the participant that sends the transferred asset amount");
		System.out.println("<Sender Private Key> : Base64 encoded private key in PKCS8 encoded key specification of the participant that sends the transferred asset amount (must match the public key associated with the sender name)");
		System.out.println("<Receipient Name> : Name of the participant that receives the transferred asset amount");
		System.out.println("<Asset Name> : Name of the asset of the sender");
		System.out.println("<Sender Node XML Config> : Node XML config including sender node name, private key, address and port");
		System.out.println("<Receiver Node XML Config> : Node XML config including receiver node name");
		System.out.println("<Amount> : Asset amount to be transferred");
	}
	
	public static void main(String[] args) {

		Start.removeCryptographyRestrictions();
		
		if (args.length == 1 && args[0].equals("-h")) {
			showUsage();
			System.exit(0);
		}
		else if (args.length == 1 && args[0].equals("-t")) {
			System.out.println("TxSender: Current timestamp is " + System.currentTimeMillis() +  " (" + new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Timestamp(System.currentTimeMillis())) + ")");
			System.exit(0);
		}
		else if ((args.length == 10 || args.length == 11) && args[0].equals("-sraw")) {
			try {
				// parse arguments
				String nodeAddress = args[1];
				int nodePort = Integer.valueOf(args[2]);
				String senderName = args[3];
				KeyFactory keyFactory = KeyFactory.getInstance(GlobalConfig.INSTANCE.PKSK_ALGORITHM);
				PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(Base64Converter.decodeToByteArray(args[4])));
				String receipientName = args[5];
				String assetName = args[6];
				int amount = Integer.valueOf(args[7]);

				
				GlobalConfig.INSTANCE.PARTICIPANT_CONFIG = new ParticipantConfig(privateKey, null, "NewTx");

				sendTx(nodeAddress, nodePort, senderName, privateKey, receipientName, amount, assetName);
			} catch (Exception e) {
				System.out.println("TxSender: One or more arguments for sending transaction in invalid format. See usage below.\n");
				showUsage();
				System.exit(1);
			}			
		} else if ((args.length == 7 || args.length == 8) && args[0].equals("-sxml")) {
			try {
				Builder parser = new Builder();
				Document senderNodeConfig = parser.build(new File(args[1]));
				Element senderNodeRoot = senderNodeConfig.getRootElement();

				Document receiverNodeConfig = parser.build(new File(args[2]));
				Element receiverNodeRoot = receiverNodeConfig.getRootElement();
				
				// parse arguments
				String nodeAddress = receiverNodeRoot.getChildElements("Address").get(0).getValue();
				int nodePort = Integer.valueOf(receiverNodeRoot.getChildElements("Port").get(0).getValue());
				String senderName = senderNodeRoot.getChildElements("Name").get(0).getValue();
				KeyFactory keyFactory = KeyFactory.getInstance(GlobalConfig.INSTANCE.PKSK_ALGORITHM);
				PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(Base64Converter.decodeToByteArray(senderNodeRoot.getChildElements("SecretKey").get(0).getValue())));
				String receipientName = receiverNodeRoot.getChildElements("Name").get(0).getValue();
				String assetName = args[3];
				int amount = Integer.valueOf(args[4]);
				
				GlobalConfig.INSTANCE.PARTICIPANT_CONFIG = new ParticipantConfig(privateKey, null, "NewTx");

				sendTx(nodeAddress, nodePort, senderName, privateKey, receipientName, amount, assetName);
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("TxSender: One or more arguments or XML config files for sending transaction in invalid format. See usage below.\n");
				showUsage();
				System.exit(1);
			}		
		}else {
			System.out.println("TxSender: Invalid arguments. See usage below.\n");
			showUsage();
			System.exit(1);
		}
		
		while(!NewClient.INSTANCE.isEmpty()) {
			try {
				Logger.FULL.log("Sleeping 1s waiting for message queue to be processed");
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
//		try {
//			System.out.println(NewClient.INSTANCE.getTotalQueueMessages());
//			Thread.sleep(4000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		// TODO: send synchronously here
		
		
		NewClient.INSTANCE.terminateAllConnections();
		System.exit(0);
	}

	private static void sendTx(String nodeAddress, int nodePort, String senderName, PrivateKey privateKey,
			String receipientName,  int amount, String assetName)
			throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {

		// create and sign transactions

			
			Transaction tx = new Transaction(senderName, receipientName, System.currentTimeMillis(), amount, true, assetName);
			Sign.signTransaction(tx, privateKey);

			// send transaction
			try {
				Client client = new Client(nodeAddress, nodePort);
				client.sendMessage(new OutgoingMessageSerializer().serializeTxToString(tx));
				client.terminateConnection();
				//NewClient.INSTANCE.sendMessage(nodeAddress, nodePort, new OutgoingMessageSerializer().serializeTxToString(tx));
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("TxSender: Cannot send transaction. Node " + nodeAddress + ":" + nodePort + " not available.\n");
				System.exit(1);
			}
		}
	
	
}
