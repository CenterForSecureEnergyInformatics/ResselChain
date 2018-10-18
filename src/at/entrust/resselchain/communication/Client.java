/*
* This file is part of ResselChain.
* Copyright Center for Secure Energy Informatics 2018
* Fabian Knirsch, Andreas Unterweger, Clemens Brunner
* This code is licensed under a modified 3-Clause BSD License. See LICENSE file for details.
*/

package at.entrust.resselchain.communication;

import java.io.IOException;
import java.io.PrintWriter;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import at.entrust.resselchain.config.GlobalConfig;
import at.entrust.resselchain.logging.Logger;

public class Client {

	private PrintWriter output = null;
	private SSLSocket socket = null;
	
	public Client(String server, int port) {
		try {
			socket = (SSLSocket)SSLSocketFactory.getDefault().createSocket(server, port);
			socket.setEnabledCipherSuites(new String[] {GlobalConfig.INSTANCE.SSL_SOCKET_CIPHER_SUITE});
			output = new PrintWriter(socket.getOutputStream());
		} catch (IOException e) {
			Logger.STD.log("Node " + server + ":" +port + " unavailable.");
		}
	}
	
	public void sendMessage(String message) {
		output.write(message);
		output.write("\n"); // make sure to terminate
		output.flush();
	}
	
	public void terminateConnection() {
		try {
			if (output != null) {
				output.write("EOL\n"); 
				output.flush();

				//output.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				output.close();
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

