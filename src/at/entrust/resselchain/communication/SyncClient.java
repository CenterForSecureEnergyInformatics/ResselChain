/*
* This file is part of ResselChain.
* Copyright Center for Secure Energy Informatics 2018
* Fabian Knirsch, Andreas Unterweger, Clemens Brunner
* This code is licensed under a modified 3-Clause BSD License. See LICENSE file for details.
*/

package at.entrust.resselchain.communication;

import at.entrust.resselchain.chain.Block;
import at.entrust.resselchain.chain.IncomingMessageSerializer;
import at.entrust.resselchain.chain.Participant;
import at.entrust.resselchain.config.GlobalConfig;
import at.entrust.resselchain.logging.Logger;
import at.entrust.resselchain.utils.Base64Converter;
import nu.xom.*;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.InetSocketAddress;
import java.util.ArrayList;

public class SyncClient {

	public static long getLastBlockNumber(Participant p) {
		Element root = new Element("Status");
		Element senderName = new Element("SenderName");
		senderName.appendChild("n/a");
		root.appendChild(senderName);

		Element senderSignature = new Element("SenderSignature");
		senderSignature.appendChild("n/a");
		root.appendChild(senderSignature);

		Element replyToAddress = new Element("ReplyToAddress");
		replyToAddress.appendChild("n/a");
		root.appendChild(replyToAddress);

		Element replyToPort = new Element("ReplyToPort");
		replyToPort.appendChild(String.valueOf(0));
		root.appendChild(replyToPort);

		Element response = request(p, root, 2000);

		if (response == null)
			return -1;
		return Long.valueOf(response.getChildElements("LastBlockNumber").get(0).getValue());
	}

	public static ArrayList<Block> getBlocks(Participant p, long from, byte[] fromHash) {
		Element root = new Element("Request");

		Element fromBlockNum = new Element("FromBlockNumber");
		fromBlockNum.appendChild(String.valueOf(from));
		root.appendChild(fromBlockNum);

		Element toBlockNum = new Element("ToBlockNumber");
		toBlockNum.appendChild(String.valueOf(-1));
		root.appendChild(toBlockNum);

		Element fromBlockH = new Element("FromBlockHash");
		fromBlockH.appendChild(Base64Converter.encodeFromByteArray(fromHash));
		root.appendChild(fromBlockH);

		Element sendingNode = new Element("SendingNode");
		sendingNode.appendChild(""); // empty sendingnode then sync response
		root.appendChild(sendingNode);

		Element response = request(p, root, 120000);

		IncomingMessageSerializer ims = new IncomingMessageSerializer();
		ArrayList<Block> blocks = new ArrayList<>();

		if (response == null) {
			return null;
		}

		if (response.getValue().equals(0)) {
			return blocks;
		}

		try {
			blocks = ims.deserializeBlocksFromElement(response);
		} catch (ParsingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return blocks;

	}

	private static Element request(Participant p, Element root, int timeout) {
		Document doc = new Document(root);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Element returnValue = null;
		try {
			Serializer serializer = new Serializer(out, "UTF-8");
			serializer.setLineSeparator("\n");
			serializer.write(doc);

			String statusRequest = out.toString().replace('\n', ' ');

			// send transaction
			/*
			 * Client client = null; try { client = new Client(nodeAddress,
			 * nodePort); client.sendMessage(statusRequest); } catch (Exception
			 * e) { System.out.println("Status: Cannot send transaction. Node "
			 * + nodeAddress + ":" + nodePort + " not available.\n");
			 * System.exit(1); } finally { if (client != null)
			 * client.terminateConnection(); }
			 */
			SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket();
			socket.connect(new InetSocketAddress(p.getAddress(), p.getPort()), timeout);
			socket.setEnabledCipherSuites(new String[] { GlobalConfig.INSTANCE.SSL_SOCKET_CIPHER_SUITE });
			socket.setSoTimeout(timeout);
			

			PrintWriter output = new PrintWriter(socket.getOutputStream());
			BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			output.println(statusRequest);
			output.flush();
			String response = input.readLine();
			try {
				returnValue = parseXML(response);
			} catch (ParsingException e) {
				Logger.FULL.log("XML parsing error in response from " +  p.getName());
				returnValue = null;
			}
			// output.println("EOL");
			output.close();
			input.close();
			socket.close();
		} catch (IOException e) {
			Logger.ERR.log("Error on request for syncing with "+ p.getName() +  ": " + e.getMessage());
		}

		return returnValue;

	}

	public static Element parseXML(String message) throws ParsingException, IOException {
		Builder parser = new Builder();
		Document doc = parser.build(message, null);

		return doc.getRootElement();
	}
}
