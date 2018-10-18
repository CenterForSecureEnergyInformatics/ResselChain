/*
* This file is part of ResselChain.
* Copyright Center for Secure Energy Informatics 2018
* Fabian Knirsch, Andreas Unterweger, Clemens Brunner
* This code is licensed under a modified 3-Clause BSD License. See LICENSE file for details.
*/

package at.entrust.resselchain.communication;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketTimeoutException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import at.entrust.resselchain.config.GlobalConfig;
import at.entrust.resselchain.logging.Logger;

public class WorkerThread extends Thread {
	
	ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
	
	private PrintWriter output;
	private SSLSocket socket;
	String server;
	int port;
	boolean isStarted = false;

		
	public void sendMessage(String message) {
		output.write(message);
		output.write("\n"); // make sure to terminate
		output.flush();
	}
	
	public void terminateConnection() {
		output.write("EOL\n");
		output.flush();
		
		// Remark: it might be necessary to sleep here before closing the stream to make sure that all data is sent
		
		output.close();
	}
	
	@Override
	public synchronized void start() {
		super.start();
		isStarted = true;
	}
	
	public void run() {
			
		try {
			socket = (SSLSocket)SSLSocketFactory.getDefault().createSocket(server, port);
			socket.setEnabledCipherSuites(new String[] {GlobalConfig.INSTANCE.SSL_SOCKET_CIPHER_SUITE});
			socket.startHandshake();
			
			output = new PrintWriter(socket.getOutputStream());
			Logger.FULL.log("Starting new WorkerThread for ID " + server + ":" + port);
		} catch (IOException e) {
			System.out.println("Worker Thread (" + this.server + ":" + this.port + "): Node " + server + ":" + port + " unavailable.");
			NewClient.INSTANCE.removeWorkerThread(server, port);
			return;
		}

		while(true) {

			try {
				if (Thread.currentThread().isInterrupted()) {
					terminateConnection();
					socket.close();
					NewClient.INSTANCE.removeWorkerThread(server, port);
					return;
				}

				if (!queue.isEmpty()) {
					String message = queue.poll();
					socket.setSoTimeout(10); // setTimeout if connection is closed --> procece error
					if (message != null && output != null)
						sendMessage(message);
				}
			} catch (IOException | NullPointerException e) {
				e.printStackTrace();
				
				// this means the socket has been closed,
				// exit thread and delete object
				NewClient.INSTANCE.removeWorkerThread(server, port);
				return;
			}

			if (queue.isEmpty()) {
				try {
					Thread.sleep(GlobalConfig.INSTANCE.COMMUNICATION_THREAD_SLEEP_MILISECONDS);

				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	}
	
	public WorkerThread(String id) {
		this(id.split(":")[0], Integer.valueOf(id.split(":")[1]).intValue());
	}
	
	public WorkerThread(String server, int port) {
		super();
		this.server = server;
		this.port = port;
		this.setName("Worker Thread " + server + ":" + port);
	}
	
	public void add(String message) {
		queue.add(message);
	}
	
	public boolean isQueueEmpty() {
		return queue.isEmpty();
	}
	
	public int getNumMessagesInQueue() {
		return queue.size();
	}
	
}