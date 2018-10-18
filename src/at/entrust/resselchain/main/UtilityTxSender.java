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
import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

import at.entrust.resselchain.chain.OutgoingMessageSerializer;
import at.entrust.resselchain.chain.Participant;
import at.entrust.resselchain.chain.UtilityTransaction;
import at.entrust.resselchain.communication.NewClient;
import at.entrust.resselchain.config.GlobalConfig;
import at.entrust.resselchain.config.ParticipantConfig;
import at.entrust.resselchain.utils.Base64Converter;
import at.entrust.resselchain.utils.Sign;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;

public class UtilityTxSender {

	private static void showUsage() {
		System.out.println("Ressel Chain Utility Transaction Sender (UtilityTxSender)");
		System.out.println("Creates a new asset plant and defines initial shares for participants.");
		System.out.println("Usage: UtilityTxSender -h | -t | (-sraw <Node Address> <Node Port> <Sender Name> <Sender Private Key>) | (-sxml <Sender Node XML Config> <Receiver Node XML Config>) <asset Name> <kW Peak> {<Participant XML Config> <Participant Share>}");
		System.out.println("-h : display help");
		System.out.println("-t : print current timestamp");
		System.out.println("-sraw : send raw transaction, all following arguments are mandatory");
		System.out.println("-sxml : send transaction with XML node config files, all following arguments are mandatory");
		System.out.println("<Node Address> <Node Port> : IP address and port of the node that initially receives this transaction");
		System.out.println("<Sender Name> : Name of the participant that sends the transferred asset amount");
		System.out.println("<Sender Private Key> : Base64 encoded private key in PKCS8 encoded key specification of the participant that sends the transferred assets (must match the public key associated with the sender name)");
		System.out.println("<Sender Node XML Config> : Node XML config including sender node name, private key, address and port");
		System.out.println("<Receiver Node XML Config> : Node XML config including receiver node name");
		System.out.println("<Asset Name> : Name of the new asset");
		System.out.println("<Participant XML Config> <Participant Share> : List of one or more tuples of participant (node) XML config file and initial share for the new asset\n");
	}
	
	public static void main(String[] args) {

		Start.removeCryptographyRestrictions();
		
		if (args.length == 1 && args[0].equals("-h")) {
			showUsage();
			System.exit(0);
		}
		else if (args.length == 1 && args[0].equals("-t")) {
			System.out.println("UtilityTxSender: Current timestamp is " + System.currentTimeMillis() +  " (" + new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Timestamp(System.currentTimeMillis())) + ")");
			System.exit(0);
		}
		else if (args.length >= 7 && args[0].equals("-sraw")) {
			try {
				// parse arguments
				String nodeAddress = args[1];
				int nodePort = Integer.valueOf(args[2]);
				String senderName = args[3];
				KeyFactory keyFactory = KeyFactory.getInstance(GlobalConfig.INSTANCE.PKSK_ALGORITHM);
				PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(Base64Converter.decodeToByteArray(args[4])));
				
				String assetName = args[5];

				ArrayList<Participant> participants = new ArrayList<>();
				HashMap<Participant, Integer>  participantAmounts = new HashMap<>();
				
				Builder parser = new Builder();
				for (int i=7; i < args.length; i+=2) {
					String participantXMLConfig = args[i];
					int participantAmount = Integer.valueOf(args[i+1]);

					Document receiverNodeConfig = parser.build(new File(participantXMLConfig));
					Element participantNodeRoot = receiverNodeConfig.getRootElement();
					
					String pNodeAddress = participantNodeRoot.getChildElements("Address").get(0).getValue();
					int pNodePort = Integer.valueOf(participantNodeRoot.getChildElements("Port").get(0).getValue());
					String pNodeName = participantNodeRoot.getChildElements("Name").get(0).getValue();
					byte[] pPublicKeyBytes = Base64Converter.decodeToByteArray(participantNodeRoot.getChildElements("PublicKey").get(0).getValue());
					
					Participant p = new Participant(pNodeName, pPublicKeyBytes, pNodeAddress, pNodePort);
					
					participants.add(p);
					participantAmounts.put(p, participantAmount);
				}
				
				GlobalConfig.INSTANCE.PARTICIPANT_CONFIG = new ParticipantConfig(privateKey, null, "NewUtilityTx");

				sendTx(nodeAddress, nodePort, senderName, privateKey, participants, assetName, participantAmounts);
			} catch (Exception e) {
				System.out.println("UtilityTxSender: One or more arguments for sending transaction in invalid format. See usage below.\n");
				showUsage();
				System.exit(1);
			}			
		} else if (args.length >= 6 && args[0].equals("-sxml")) {
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
				
				
				String assetName = args[3];
				double kWPeak = Double.valueOf(args[4]);
				
				ArrayList<Participant> participants = new ArrayList<>();
				HashMap<Participant, Integer>  participantAmounts = new HashMap<>();
				
				for (int i=5; i < args.length; i+=2) {
					String participantXMLConfig = args[i];
					int participantAmount = Integer.valueOf(args[i+1]);
					
					receiverNodeConfig = parser.build(new File(participantXMLConfig));
					Element participantNodeRoot = receiverNodeConfig.getRootElement();
					
					String pNodeAddress = participantNodeRoot.getChildElements("Address").get(0).getValue();
					int pNodePort = Integer.valueOf(participantNodeRoot.getChildElements("Port").get(0).getValue());
					String pNodeName = participantNodeRoot.getChildElements("Name").get(0).getValue();
					byte[] pPublicKeyBytes = Base64Converter.decodeToByteArray(participantNodeRoot.getChildElements("PublicKey").get(0).getValue());
					
					Participant p = new Participant(pNodeName, pPublicKeyBytes, pNodeAddress, pNodePort);
					
					participants.add(p);
					participantAmounts.put(p, participantAmount);
				}
				
				GlobalConfig.INSTANCE.PARTICIPANT_CONFIG = new ParticipantConfig(privateKey, null, "NewUtilityTx");
				
				sendTx(nodeAddress, nodePort, senderName, privateKey, participants, assetName, participantAmounts);
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("UtilityTxSender: One or more arguments or XML config files for sending transaction in invalid format. See usage below.\n");
				showUsage();
				System.exit(1);
			}		
		}else {
			System.out.println("UtilityTxSender: Invalid arguments. See usage below.\n");
			showUsage();
			System.exit(1);
		}
		
		while(!NewClient.INSTANCE.isEmpty()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		NewClient.INSTANCE.terminateAllConnections();
		System.exit(0);
	}

	private static void sendTx(String nodeAddress, int nodePort, String senderName, PrivateKey privateKey, ArrayList<Participant> participants, String assetName, HashMap<Participant, Integer> participantAmount)
			throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		
		// create and sign transactions
		UtilityTransaction tx = new UtilityTransaction(senderName, "n/a", System.currentTimeMillis(), 0,  assetName, participants, participantAmount, true);

		Sign.signTransaction(tx, privateKey);

		// send transaction
		try {
			NewClient.INSTANCE.sendMessage(nodeAddress, nodePort, new OutgoingMessageSerializer().serializeTxToString(tx));
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("UtilityTxSender: Cannot send transaction. Node " + nodeAddress + ":" + nodePort + " not available.\n");
			System.exit(1);
		}
	}
	
	
}
