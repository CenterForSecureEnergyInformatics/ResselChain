/*
* This file is part of ResselChain.
* Copyright Center for Secure Energy Informatics 2018
* Fabian Knirsch, Andreas Unterweger, Clemens Brunner
* This code is licensed under a modified 3-Clause BSD License. See LICENSE file for details.
*/

package at.entrust.resselchain.communication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import at.entrust.resselchain.config.GlobalConfig;
import at.entrust.resselchain.logging.Logger;

public class Server implements Runnable {
	private ArrayList<ClientThread> clientThreads = new ArrayList<ClientThread>();;
	private int port;
	private IncomingMessageHandler messageHandler;
	
	private static Thread serverThread = null;
	private static Server serverInstance = null;
	
	public static void startServer(int port, IncomingMessageHandler messageHandler) {
		serverInstance = new Server(port, messageHandler);
		serverThread = new Thread(serverInstance);
		serverThread.setName("Server");
		serverThread.start();
	}
	
	public static void stopServer() {
		serverThread.interrupt();
	}

	Server(int port, IncomingMessageHandler messageHandler) {
		this.port = port;
		this.messageHandler = messageHandler;
	}
	
	public static ArrayList<String> getThreadStates() {
		ArrayList<String> states = new ArrayList<>();
		if (serverInstance == null)
			return states;
		
		for (ClientThread t : serverInstance.clientThreads) {
			states.add(t.getState().toString());
		}
		return states;
	}
	
	
	@Override
	public void run() {
		try {
			SSLServerSocket serverSocket = (SSLServerSocket)SSLServerSocketFactory.getDefault().createServerSocket(port);
			serverSocket.setEnabledCipherSuites(new String[] {GlobalConfig.INSTANCE.SSL_SOCKET_CIPHER_SUITE});
			
			while(true) {
				if (Thread.currentThread().isInterrupted()) {
					Logger.FULL.log("Interrupting all client threads due to the termination of the server thread");
					// close all client threads and exit
					for(ClientThread t : clientThreads)
						t.interrupt();
					serverSocket.close();
					return;
				}
				
				SSLSocket socket = (SSLSocket) serverSocket.accept(); // wait for connection
				//socket.setSoTimeout(GlobalConfig.INSTANCE.SOCKET_READ_TIMEOUT_MILISECONDS);
				ClientThread t = new ClientThread(socket, messageHandler);
				t.setName("Server Thread "+ socket.getRemoteSocketAddress());
				clientThreads.add(t);
				t.start();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	class ClientThread extends Thread {
		Socket socket;
		IncomingMessageHandler messageHandler;
		BufferedReader input;
		PrintWriter output;
		int id;

		ClientThread(Socket socket, IncomingMessageHandler messageHandler) {
			this.socket = socket;
			this.messageHandler = messageHandler;
			
			try
			{
				// create output first
				input  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				output = new PrintWriter(socket.getOutputStream());
			}
			catch (IOException e) {
				return;
			}
		}
		
		private boolean isConnectionTerminated() {
			return socket.isClosed(); // TODO: this does not return true if client terminates, find better solution!
		}

		public void run() {
			while(true) {
				if (Thread.currentThread().isInterrupted() == true || isConnectionTerminated()) {
					Logger.FULL.log("Interrupting client thread due to the connection being terminated");
					try {
						input.close();
						output.close();
						socket.close();
						//clientThreads.remove(this);
					} catch (IOException e) {
						e.printStackTrace();
					} finally {
						clientThreads.remove(this);
					}
					return;
				}
				
				try {
					String message = input.readLine();
					//Logger.FULL.log("DEBUG/SERVER: " +  ((message == null) ? "null" : message));
					if (message != null && !message.equals("EOL")) {
						messageHandler.processMessage(message, output);
					} else {
						//Logger.FULL.log("Interrupting client thread due to possible EOL command");
						//TODO: spams the output because of status request ^
						//Thread.currentThread().interrupt();
						//System.gc();
						clientThreads.remove(this);
						return;
					}
					
					try {
						Thread.sleep(GlobalConfig.INSTANCE.COMMUNICATION_THREAD_SLEEP_MILISECONDS);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
				} catch (Exception e) {
					// remove from client list (also in case of read timeout)
					e.printStackTrace();
					Thread.currentThread().interrupt();
					clientThreads.remove(this);
					//System.gc();
					return;
				}
				/*} catch (IOException e) {
					e.printStackTrace();
				}*/
			}
		}
	}
}


