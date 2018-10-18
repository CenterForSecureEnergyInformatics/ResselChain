/*
* This file is part of ResselChain.
* Copyright Center for Secure Energy Informatics 2018
* Fabian Knirsch, Andreas Unterweger, Clemens Brunner
* This code is licensed under a modified 3-Clause BSD License. See LICENSE file for details.
*/

package at.entrust.resselchain.main;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import at.entrust.resselchain.config.GlobalConfig;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;
import nu.xom.Serializer;

public class Status {

	private static void showUsage() {
		System.out.println("Ressel Chain Node Status (Status)");
		System.out.println("Read status from a specified node.");
		System.out.println("Usage: Status -h | (<Node Address> <Node Port>)");
		System.out.println("-h : display help");
		System.out.println("<Node Address> <Node Port> : IP address and port of the node to request status from");
	}
	
	/* https://stackoverflow.com/questions/139076/how-to-pretty-print-xml-from-java */
	public static String format(String xml) throws ParsingException, IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Serializer serializer = new Serializer(out);
		serializer.setIndent(4);
		serializer.write(new Builder().build(xml, null));
		return out.toString("UTF-8");
	}
	
	public static void main(String[] args) {

		Start.removeCryptographyRestrictions();

		if (args.length == 1 && args[0].equals("-h")) {
			showUsage();
			System.exit(0);
		}
		else if (args.length == 2 || args.length == 5) {
			
			String ipAddress = "";
			if (args.length == 2) {
				try {
					ipAddress = InetAddress.getLocalHost().getHostAddress();
				} catch (UnknownHostException e1) {
					System.out.println("IP response address could not be determined. Specify with -p <Response Address> <Response Port>.");
					System.exit(1);
				}
			}
			int port = 7828;
			if (args.length == 5 && args[2].equals("-p")) {
				ipAddress = args[3];
				port = Integer.valueOf(args[4]);
			}
			
			// open server for response
			/*Server.startServer(port, new IncomingMessageHandler() {
				@Override
				public void processMessage(String message) {
					try {
						System.out.println(format(message));
					} catch (ParsingException | IOException e) {
						System.out.println("Invalid response.");
						Server.stopServer();
						System.exit(1);
					} finally {
						Server.stopServer();
						System.exit(0);
					}
				}
			});*/
			
			try {
				// parse arguments
				String nodeAddress = args[0];
				int nodePort = Integer.valueOf(args[1]);

				Element root = new Element("Status");
				Element senderName = new Element("SenderName");
				senderName.appendChild("n/a");
				root.appendChild(senderName);

				Element senderSignature = new Element("SenderSignature");
				senderSignature.appendChild("n/a");
				root.appendChild(senderSignature);

				Element replyToAddress = new Element("ReplyToAddress");
				replyToAddress.appendChild(ipAddress);
				root.appendChild(replyToAddress);

				Element replyToPort = new Element("ReplyToPort");
				replyToPort.appendChild(String.valueOf(port));
				root.appendChild(replyToPort);

				Document doc = new Document(root);
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				try {
					Serializer serializer = new Serializer(out, "UTF-8");
					serializer.setLineSeparator("\n");
					serializer.write(doc);
					
					String statusRequest = out.toString().replace('\n', ' ');
					
					// send transaction
					/*
					Client client = null;
					try {
						client = new Client(nodeAddress, nodePort);
						client.sendMessage(statusRequest);
					} catch (Exception e) {
						System.out.println("Status: Cannot send transaction. Node " + nodeAddress + ":" + nodePort + " not available.\n");
						System.exit(1);
					} finally {
						if (client !=  null)
							client.terminateConnection();
					}*/
					SSLSocket socket = (SSLSocket)SSLSocketFactory.getDefault().createSocket(nodeAddress, nodePort);
					socket.setEnabledCipherSuites(new String[] {GlobalConfig.INSTANCE.SSL_SOCKET_CIPHER_SUITE});
					
					PrintWriter output = new PrintWriter(socket.getOutputStream());
					BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					
					output.println(statusRequest);
					output.flush();
					String response = input.readLine();
					System.out.println(format(response));

					//output.println("EOL");
					output.close();
					input.close();
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} catch (Exception e) {
				System.out.println("Status: One or more arguments are in invalid format. See usage below.\n");
				showUsage();
				System.exit(1);
			}
			
		}else {
			System.out.println("Status: Invalid arguments. See usage below.\n");
			showUsage();
			System.exit(1);
		}
	}

}
