package com.capstone.simulation.proxy;

import com.capstone.simulation.bloomfilters.BloomFilter;
import com.capstone.simulation.client.Client;
import com.capstone.simulation.data.DataBlock;
import com.capstone.simulation.server.Server;
import com.capstone.simulation.utility.Hash;

/**
 * This is the singleton implementation of proxy that forwards client requests
 * and sometimes delivers data to clients
 * 
 * @author Pavan Kumar
 */
public class Proxy {

	private int numberOfClients;
	private int bloomFilterSize;
	private BloomFilter[] bloomFilters;
	private Client[] clients;
	
	private int hitCount;
	private int missCount;
	
	private Server server = Server.getInstance();
	private static final Proxy proxy = new Proxy();

	private Proxy() {

	}

	public static Proxy getInstance() {
		return proxy;
	}

	/**
	 * Adds data to bloom filter set
	 * 
	 * @param clientId
	 *            whose bloom filter is being updated
	 * @param data
	 *            which is being added to the bloom filter
	 */
	public void addDataToBloomFilter(int clientId, int data) {
		int[] hashes = Hash.getInstance().generateHashValues(data);
		for (int i = 0; i < hashes.length; i++) {
			bloomFilters[clientId - 1].setBit(hashes[i]);
		}
	}

	/**
	 * Receives data request from client and finds it
	 * @param data
	 */
	public void receiveDataRequest(int clientId, int data) {
		int index = lookForData(data);
		
		if (index == Integer.MIN_VALUE) {
//			Retrieve and send data from server
			sendDataFromServer(index, data);
		} else {
//			Forward it to client
			clients[index].forwardDataRequest(clientId, data);
		}
	}
	
	/**
	 * Iterates through all the client bloom filters to see if all the bits are
	 * set at hash value indexes.
	 * 
	 * @param data
	 *            The data which is being searched
	 * @return clientId if data is found. Else returns Integer.MIN_VALUE
	 */
	private int lookForData(int data) {
		int[] hashes = Hash.getInstance().generateHashValues(data);
		boolean foundData = false;
		int bfIndex = 0;

		while (!foundData && (bfIndex != numberOfClients)) {
			for (int j = 0; j < hashes.length; j++) {
				if (bloomFilters[bfIndex].isBitSet(hashes[j])) {
					foundData = true;
				} else {
					foundData = false;
					break;
				}
			}
			bfIndex++;
		}
		if (foundData) {
			return bfIndex;
		}
		return Integer.MIN_VALUE;
	}

	/**
	 * @return the numberOfClients
	 */
	public int getNumberOfClients() {
		return numberOfClients;
	}

	/**
	 * @param numberOfClients
	 *            the numberOfClients to set
	 */
	public void setNumberOfClients(int numberOfClients) {
		this.numberOfClients = numberOfClients;
	}

	/**
	 * @return the bloomFilterSize
	 */
	public int getBloomFilterSize() {
		return bloomFilterSize;
	}

	/**
	 * @param bloomFilterSize
	 *            the bloomFilterSize to set
	 */
	public void setBloomFilterSize(int bloomFilterSize) {
		this.bloomFilterSize = bloomFilterSize;
	}

	/**
	 * @return the bloomFilters
	 */
	public BloomFilter[] getBloomFilters() {
		return bloomFilters;
	}

	/**
	 * Initializes bloom filters for all the clients on proxy
	 */
	public void setBloomFilters() {
		int clientSize = getNumberOfClients();
		this.bloomFilters = new BloomFilter[clientSize];

		for (int i = 0; i < clientSize; i++) {
			bloomFilters[i] = new BloomFilter(getBloomFilterSize());
		}
	}

	/**
	 * @return the clients
	 */
	public Client[] getClients() {
		return clients;
	}

	/**
	 * @param clients the clients to set
	 */
	public void setClients(Client[] clients) {
		this.clients = clients;
	}

	/**
	 * @return the server
	 */
	public Server getServer() {
		return server;
	}

	/**
	 * @param server the server to set
	 */
	public void setServer(Server server) {
		this.server = server;
	}
	
	private void sendDataFromServer(int clientIndex, int data) {
		int retrievedData = server.getData(data);
		DataBlock dataBlock = new DataBlock(retrievedData);
		clients[clientIndex].receiveData(dataBlock);
	}
	
	public int getHitCount() {
		return hitCount;
	}
	
	public synchronized void hitOccured() {
		hitCount++;
	}
	
	public int getMissCount() {
		return missCount;
	}
 	
	public synchronized void missOccured() {
		missCount++;
	}

}
