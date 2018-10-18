/*
* This file is part of ResselChain.
* Copyright Center for Secure Energy Informatics 2018
* Fabian Knirsch, Andreas Unterweger, Clemens Brunner
* This code is licensed under a modified 3-Clause BSD License. See LICENSE file for details.
*/

package at.entrust.resselchain.main;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import at.entrust.resselchain.communication.NewClient;
import at.entrust.resselchain.config.GlobalConfig;
import nu.xom.Builder;
import nu.xom.ParsingException;
import nu.xom.Serializer;

public class XMLSender {

	private static void showUsage() {
		System.out.println("Ressel Chain XML Sender (XMLSender)");
		System.out.println("Sends XML string to a node and displays response (synchronous only).");
		System.out.println("Usage: XMLSender -h | (-xml <Node Address> <Node Port> <XML File>)");
		System.out.println("-h : display help");
		System.out.println("<Node Address> <Node Port> : IP address and port of the node that initially receives this transaction");
		System.out.println("<XML File> : Path to file containing XML string\n");
	}
	
	public static void main(String[] args) {

		Start.removeCryptographyRestrictions();
		
		if (args.length == 1 && args[0].equals("-h")) {
			showUsage();
			System.exit(0);
		}
		else if (args.length == 4 && args[0].equals("-xml")) {
			try {
				// parse arguments
				String nodeAddress = args[1];
				int nodePort = Integer.valueOf(args[2]);
				String xmlFile = args[3];
				
				
				try {
					String xmlFileContent = readFile(xmlFile).replace('\n', ' ').replace('\r', ' ');
					
					try {
						SSLSocket socket = (SSLSocket)SSLSocketFactory.getDefault().createSocket(nodeAddress, nodePort);
						socket.setEnabledCipherSuites(new String[] {GlobalConfig.INSTANCE.SSL_SOCKET_CIPHER_SUITE});
						
						PrintWriter output = new PrintWriter(socket.getOutputStream());
						BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
						
						output.println(xmlFileContent);
						output.flush();
						String response = input.readLine();
						System.out.println(format(response));

						output.println("EOL");
						output.close();
						input.close();
						socket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					
				} catch (Exception e) {
					System.out.println("XMLSender: Invalid file.");
				}

			} catch (Exception e) {
				System.out.println("XMLSender: One or more arguments for sending XML string in invalid format. See usage below.\n");
				showUsage();
				System.exit(1);
			}				
		} else {
			System.out.println("XMLSender: Invalid arguments. See usage below.\n");
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
	
	/* https://stackoverflow.com/questions/4716503/reading-a-plain-text-file-in-java */
	private static String readFile(String file) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));
		try {
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();

			while (line != null) {
				sb.append(line);
				sb.append(System.lineSeparator());
				line = br.readLine();
			}
			return sb.toString();
		} finally {
			br.close();
		}
	}
	
	/* https://stackoverflow.com/questions/139076/how-to-pretty-print-xml-from-java */
	public static String format(String xml) throws ParsingException, IOException {
		//System.out.println(xml);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Serializer serializer = new Serializer(out);
		serializer.setIndent(4);
		serializer.write(new Builder().build(xml, null));
		return out.toString("UTF-8");
	}
}