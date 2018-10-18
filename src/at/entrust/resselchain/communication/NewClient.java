/*
* This file is part of ResselChain.
* Copyright Center for Secure Energy Informatics 2018
* Fabian Knirsch, Andreas Unterweger, Clemens Brunner
* This code is licensed under a modified 3-Clause BSD License. See LICENSE file for details.
*/

package at.entrust.resselchain.communication;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import at.entrust.resselchain.chain.Participant;
import at.entrust.resselchain.config.GlobalConfig;
import at.entrust.resselchain.logging.Logger;
import at.entrust.resselchain.state.ChainState;

public enum NewClient {
	INSTANCE;
			
	private ConcurrentHashMap<String, WorkerThread> workerThreads = new ConcurrentHashMap<>();
	
	public void sendMessage(String server, int port, String message) {
		String id = server + ":" + port;
		
		WorkerThread workerThread = workerThreads.computeIfAbsent(id, (String s) -> new WorkerThread(s));
		if (!workerThread.isStarted)
			workerThread.start();
		
		workerThread.add(message);
		//Logger.FULL.log("Active WorkerThreads: " + workerThreads.size());
	}

	
	public void removeWorkerThread(String server, int port) {
		String id = server + ":" + port;
		Logger.FULL.log("WorkerThread removed for ID " + id);
		workerThreads.remove(id);
	}

	
	// TODO: make it really thread-safe -> see Iterator in getQueueMessages()
	public boolean isEmpty() {
		for(WorkerThread wt : workerThreads.values()) {
			//if (wt.getNumMessagesInQueue() != 0)  return false;
			if (!wt.isQueueEmpty()) return false;
		}
		return true;
	}

	// TODO: make it really thread-safe -> see Iterator in getQueueMessages()
	public int getTotalQueueMessages() {
		int count = 0;
		for(WorkerThread wt : workerThreads.values()) {
			count += wt.getNumMessagesInQueue();
		}
		return count;
	}
	
	public HashMap<String, Integer> getQueueMessages() {
		HashMap<String, Integer> counts = new HashMap<>();
		Iterator<Entry<String, WorkerThread>> it = workerThreads.entrySet().iterator();
		while(it.hasNext()) {
			Entry<String, WorkerThread> entry = it.next();
			counts.put(entry.getKey(), entry.getValue().getNumMessagesInQueue());
		}
		return counts;
	}
	
	public void terminateAllConnections() {
		for(WorkerThread wt : workerThreads.values()) {
			wt.terminateConnection();
		}
	}
}
