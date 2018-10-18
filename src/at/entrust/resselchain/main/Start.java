/*
* This file is part of ResselChain.
* Copyright Center for Secure Energy Informatics 2018
* Fabian Knirsch, Andreas Unterweger, Clemens Brunner
* This code is licensed under a modified 3-Clause BSD License. See LICENSE file for details.
*/

package at.entrust.resselchain.main;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;

import at.entrust.resselchain.chain.Block;
import at.entrust.resselchain.chain.IncomingMessage;
import at.entrust.resselchain.chain.Participant;
import at.entrust.resselchain.communication.Server;
import at.entrust.resselchain.config.GenesisBlock;
import at.entrust.resselchain.config.GlobalConfig;
import at.entrust.resselchain.config.ParticipantConfig;
import at.entrust.resselchain.logging.Logger;
import at.entrust.resselchain.mining.Miner;
import at.entrust.resselchain.state.ChainState;
import at.entrust.resselchain.state.ParticipantsAlreadyExistsException;
import at.entrust.resselchain.statetable.AssetStateTable;
import at.entrust.resselchain.tree.TreeNode;
import at.entrust.resselchain.utils.Base64Converter;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.ParsingException;
import nu.xom.ValidityException;

public class Start {

	public static void main(String[] args) throws NoSuchAlgorithmException, ParticipantsAlreadyExistsException, ValidityException, ParsingException, IOException, InvalidKeySpecException {
		if (args.length == 0) {
			System.out.println("Pass name of node config file as first argument and name of participants config file as second argument.");
			return;
		}
		
		removeCryptographyRestrictions();
		
		Thread.currentThread().setName("Main");
		ChainState.INSTANCE.setStatus("Node started");
		
		// Parse config file
		Builder parser = new Builder();
		Document doc = parser.build(new File(args[0]));
		Element root = doc.getRootElement();
		
		// Initialization of this node
		// Note: When exporting Java generated key with getEncoded,
		// the public key is in X509 format and the private key is in PKCS8 format;
		// therefore we need to import them accordingly
		KeyFactory keyFactory = KeyFactory.getInstance(GlobalConfig.INSTANCE.PKSK_ALGORITHM);
		PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(Base64Converter.decodeToByteArray(root.getChildElements("PublicKey").get(0).getValue())));
		PrivateKey secretKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(Base64Converter.decodeToByteArray(root.getChildElements("SecretKey").get(0).getValue())));
		
		String nodeName = root.getChildElements("Name").get(0).getValue();
		
		// check if special role is defined
		String specialRole = null;
		if (root.getChildElements("SpecialRole").size() != 0)
			specialRole = root.getChildElements("SpecialRole").get(0).getValue();
		
		GlobalConfig.INSTANCE.PARTICIPANT_CONFIG = new ParticipantConfig(secretKey, publicKey, nodeName, specialRole);
	
		File dir = new File("chainstate");
		if (!dir.exists()) {
			if (!dir.mkdir()) {
				Logger.ERR.log("Could not find or create directory " + dir.getAbsolutePath() + ".");
				System.exit(1);
			}
		}
		
		// Add all participants
		Builder parser2 = new Builder();
		Document doc2 = parser2.build(new File(args[1]));
		Element root2 = doc2.getRootElement();

		Elements participants = root2.getChildElements("Participant");
		for(int i = 0; i < participants.size(); i++) {
			String specialParticipantRole = null;
			if (participants.get(i).getChildElements("SpecialRole").size() != 0)
				specialRole = participants.get(i).getChildElements("SpecialRole").get(0).getValue();

			Participant p = new Participant(participants.get(i).getChildElements("Name").get(0).getValue(), Base64Converter.decodeToByteArray(participants.get(i).getChildElements("PublicKey").get(0).getValue()), participants.get(i).getChildElements("Address").get(0).getValue(), Integer.valueOf(participants.get(i).getChildElements("Port").get(0).getValue()), specialParticipantRole);
			ChainState.INSTANCE.addParticipant(p);
		}
		

		ChainState.INSTANCE.setStatus("Bootstrapping");
		Logger.FULL.log("Begin bootstrapping from disc");
		

		Logger.FULL.log("Begin bootstrapping asset state");
		// Bootstrapping from asset state table DB
		HashMap<String, AssetStateTable> tables = AssetStateTable.createAssetStateTablesFromMeta();
		for (String s : tables.keySet())
			ChainState.INSTANCE.addAssetStateTable(s, tables.get(s));
		Logger.STD.log("Successfully bootstrapped from disc with " + tables.size() + " asset tables.");


		Logger.FULL.log("Begin bootstrapping chainstate");
		// Bootstrapping from chainstate
		if (dir.listFiles().length != 0) {
			try {
				long lastBlockNumber = 0;
				long lastNodeNumber = 0;
				ArrayList<TreeNode> lastNodes = new ArrayList<>();
				if (dir.isDirectory()) {
					File[] files = dir.listFiles();
					if (files != null && files.length != 0) {
						for (File file : files) {
							long fileBlockNumber = Long.valueOf(file.getName().split("-")[0]);
							long fileNodeNumber  = Long.valueOf(file.getName().split("-")[1]);
							if (fileBlockNumber > lastBlockNumber)
								lastBlockNumber = fileBlockNumber;
							if (fileNodeNumber > lastNodeNumber)
								lastNodeNumber = fileNodeNumber;
						}
					}
				}
				final long effectiveLastBlockNumber = lastBlockNumber;
				File[] files = dir.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return name.matches(effectiveLastBlockNumber + "-.*");
					}
				});
				
				if (files!= null && files.length != 0) {
					TreeNode.setNodeCount(lastNodeNumber + 1); // will be next node
					for(File file : files) {
						long fileNodeNumber  = Long.valueOf(file.getName().split("-")[1]);
						lastNodes.add(TreeNode.load(effectiveLastBlockNumber, fileNodeNumber));
					}
					ChainState.INSTANCE.getBlockchain().setLastNodes(lastNodes, TreeNode.load(0, 0));
				}
				Logger.STD.log("Successfully bootstrapped from disc with last blocknumber " + effectiveLastBlockNumber + ".");
			} catch (Exception e) {
				Logger.STD.log("Bootstrapping from disc failed.");
				TreeNode.setNodeCount(0);
				ChainState.INSTANCE.getBlockchain().setLastNodes(new ArrayList<>(), null);
			}
		}
				
		// add hard-coded genesis block
		if (ChainState.INSTANCE.getBlockchain().getRootNode() == null) {
			Block genesisBlock = GenesisBlock.getGenesisBlock();
			ChainState.INSTANCE.appendBlock(genesisBlock);
		}
		
		
		ChainState.INSTANCE.setStatus("Node configured");

		// Start server and listen to messages from other peers
		GlobalConfig.INSTANCE.INCOMING_MESSAGE_PORT = Integer.valueOf(root.getChildElements("Port").get(0).getValue());
		Server.startServer(GlobalConfig.INSTANCE.INCOMING_MESSAGE_PORT, new IncomingMessage());
		
		// Start mining thread

		Miner.startMining();

		//ChainState.INSTANCE.startSync(1);
		
		ChainState.INSTANCE.setStatus("Node waits for requests and is ready for mining");

		
	}
	
	/* https://stackoverflow.com/a/44056166/4568958 */
	public static void removeCryptographyRestrictions() {
		if (!isRestrictedCryptography()) {
			return;
		}
		try {
			/*
			 * Do the following, but with reflection to bypass access checks:
			 * 
			 * JceSecurity.isRestricted = false; JceSecurity.defaultPolicy.perms.clear();
			 * JceSecurity.defaultPolicy.add(CryptoAllPermission.INSTANCE);
			 */
			final Class<?> jceSecurity = Class.forName("javax.crypto.JceSecurity");
			final Class<?> cryptoPermissions = Class.forName("javax.crypto.CryptoPermissions");
			final Class<?> cryptoAllPermission = Class.forName("javax.crypto.CryptoAllPermission");

			Field isRestrictedField = jceSecurity.getDeclaredField("isRestricted");
			isRestrictedField.setAccessible(true);
			setFinalStatic(isRestrictedField, true);
			isRestrictedField.set(null, false);

			final Field defaultPolicyField = jceSecurity.getDeclaredField("defaultPolicy");
			defaultPolicyField.setAccessible(true);
			final PermissionCollection defaultPolicy = (PermissionCollection) defaultPolicyField.get(null);

			final Field perms = cryptoPermissions.getDeclaredField("perms");
			perms.setAccessible(true);
			//((Map<?, ?>) perms.get(defaultPolicy)).clear();

			final Field instance = cryptoAllPermission.getDeclaredField("INSTANCE");
			instance.setAccessible(true);
			defaultPolicy.add((Permission) instance.get(null));
		}
		catch (final Exception e) {
			e.printStackTrace();
		}
	}

	static void setFinalStatic(Field field, Object newValue) throws Exception {
		field.setAccessible(true);

		Field modifiersField = Field.class.getDeclaredField("modifiers");
		modifiersField.setAccessible(true);
		modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

		field.set(null, newValue);
	}

	private static boolean isRestrictedCryptography() {
		// This simply matches the Oracle JRE, but not OpenJDK.
		return "Java(TM) SE Runtime Environment".equals(System.getProperty("java.runtime.name"));
	}
	

}
