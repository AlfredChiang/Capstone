package com.capstone.simulation.proxy;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import com.capstone.simulation.bloomfilters.IABloomFilter;
import com.capstone.simulation.client.Client;
import com.capstone.simulation.client.ClientKSequence;
import com.capstone.simulation.data.DataBlock;
import com.capstone.simulation.data.Forward;
import com.capstone.simulation.server.Server;
import com.capstone.simulation.utility.Hash;

/**
 * This is the singleton implementation of proxy that forwards client requests
 * and sometimes delivers data to clients
 * 
 * @author Pavan Kumar
 */
public class ProxyIBF implements Proxy{

	private int numberOfClients;
	private int bloomFilterSize;
	private IABloomFilter[] bloomFilters;
	private Client[] clients;
	private ConcurrentHashMap<Integer, Integer> singletMap;
	private Integer hitCount;
	private Integer missCount;
	private Integer diskAccessCount;
	
	private Server server = Server.getInstance();
	private static final ProxyIBF proxy = new ProxyIBF();

	private ProxyIBF() {

	}

	public static ProxyIBF getInstance() {
		return proxy;
	}

	/**
	 * Adds data to bloom filter set
	 * 
	 * @param clientId
	 *            whose bloom filter is being updated
	 * @param data
	 *            which is being added to the bloom filter
	 * @param importance 
	 */
	public void addDataToBloomFilter(int clientId, int data, int importance) {
		synchronized (clients[clientId - 1]) {
			bloomFilters[clientId - 1].updateBF(data, importance);
		}
	}

	/**
	 * Receives data request from client and finds it
	 * @param data
	 */
	public synchronized void receiveDataRequest(int clientId, int data) {
//		System.out.println("Entered receiveDataRequest for client " + clientId + " and data " + data);
		int fromClientId = 0; // Start looking from first client

		while (fromClientId < numberOfClients) {
//			System.out.println("receiveDataRequest: Entered loop for client "+ clientId);
//			System.out.println("Bloom filter look up: Begin : From " + fromClientId);
			int index = lookForData(clientId - 1, data, fromClientId);
//			System.out.println("Bloom filter look up: End : Data found at " + (index+1));
//			System.out.println("Found data in client " + (index+1));
			
			if (index == Integer.MIN_VALUE) {
//				Retrieve and send data from server
				sendDataFromServer(clientId - 1, data);
//				System.out.println("Disk access occured");
				break;
			} else {
//				Forward it to client
//				System.out.println("Found data " + data + " in client " + (index+1));
				if (clients[index].forwardDataRequest(clientId - 1, data)) {
					hitOccured();
//					System.out.println("Hit occured");
					break;
				} else {
					
					fromClientId = index + 1;
					missCount++;
//					System.out.println("Miss occured: Incremented clientId to " + fromClientId);
				}
				
			}
		}
		
		if (fromClientId >= numberOfClients) {
//			Traversed through all the Bloom filters and data is not found in any client.
//			So, disk access
			sendDataFromServer(clientId - 1, data);
//			System.out.println("Disk access occured");
		}
//		System.out.println("Exited receiveDataRequest for client " + clientId + " and data " + data);
	}
	
	/**
	 * Iterates through all the client bloom filters to see if all the bits are
	 * set at hash value indexes.
	 * 
	 * @param data
	 *            The data which is being searched
	 * @return clientId if data is found. Else returns Integer.MIN_VALUE
	 */
	public int lookForData(int clientId, int data, int fromClientId) {
//		System.out.println("Entered lookforData");
//		System.out.println("Calculated hashes");
		boolean foundData = false;
		int[] hashes = Hash.getInstance().generateHashValues(data);
		int bfIndex = fromClientId;

		while (!foundData && (bfIndex < numberOfClients)) {
//			System.out.println("In the loop for client " + clientId + " numberOfClients " + numberOfClients);
			if (bfIndex == clientId) {
//				Skipping the client that requested data
				bfIndex++;
				continue;
			}
			
			for (int i = 0; i < hashes.length; i++) {
//				System.out.println("Looking for data: " + data + " in client: " + bfIndex + " at position: " + hashes[i]);
				if (bloomFilters[bfIndex].isIndexSet(hashes[i])) {
					foundData = true;
				} else {
					foundData = false;
					break;
				}
			}
			
			if (foundData) {
//				System.out.println("Found data in client " + (clientId + 1));
				return bfIndex;
			} else {
				bfIndex++;
			}
			
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
	public IABloomFilter[] getBloomFilters() {
		return bloomFilters;
	}

	/**
	 * Initializes bloom filters for all the clients on proxy
	 */
	public void setBloomFilters() {
		int clientSize = getNumberOfClients();
//		System.out.println("Initializing bloomfilters on proxy");
		this.bloomFilters = new IABloomFilter[clientSize];

		for (int i = 0; i < clientSize; i++) {
//			System.out.println("Setting bloomfilter size to " + getBloomFilterSize());
			bloomFilters[i] = new IABloomFilter(getBloomFilterSize());
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
	 * @return the singletMap
	 */
	public ConcurrentHashMap<Integer, Integer> getSingletMap() {
		return singletMap;
	}

	/**
	 * @param singletMap the singletMap to set
	 */
	public void setSingletMap() {
//		Initial capacity = total number of data blocks cached in all the clients altogether
		int initialCapacity = numberOfClients * clients[0].getCacheSize();
		float loadFactor = 0.75f;
		this.singletMap = new ConcurrentHashMap<Integer, Integer>(initialCapacity, loadFactor);
	}
	
	/**
	 * Updates singletMap: If data was not present in the map, it is added to it
	 * and it's value is set to 1. If it is already present, its value is
	 * incremented by 1.
	 * @param data
	 * @param increment 
	 */
	public void updateSingletMap(int data, boolean increment) {
		synchronized (singletMap) {
			if (increment) {
				if (singletMap.containsKey(data)) {
					int count = singletMap.get(data);
					singletMap.put(data, count++);
				} else {
//					Add entry for data to Singlet map
					singletMap.put(data, 1);
				}
			} else {
				if (singletMap.containsKey(data)) {
					int count = singletMap.get(data);
					singletMap.put(data, count--);
				}
			}
			
		}
//		System.out.println("Singlet Map size: " + singletMap.size());
	}
	
	/**
	 * Returns occurrence count of data
	 * @param data
	 * @return
	 */
	public int getDataOccurrenceCount(int data) {
		synchronized (singletMap) {
			if (singletMap.containsKey(data)) {
				return singletMap.get(data);
			}
		}
		return 0;
	}
	
	/**
	 * Checks if data is a Singlet 
	 * @param data
	 * @return true iff the data is a singlet
	 */
	public boolean isDataSinglet(int clientId, int data) {
		synchronized (singletMap) {
			try {
				return (singletMap.get(data) == 1);
			} catch (NullPointerException e) {
//				SingletMap throws null pointer when data is not present
				return true;
			}
		}
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
//		System.out.println("Server sending client: " + clients[clientIndex].getId() + " data: " + retrievedData);
		clients[clientIndex].receiveData(dataBlock);
		diskAccessOccured();
	}
	
	public int getHitCount() {
		return hitCount;
	}
	
	public void setHitCount(int value) {
		hitCount = value;
	}
	
	public void hitOccured() {
		synchronized (hitCount) {
			hitCount++;
		}
	}
	
	public int getMissCount() {
		return missCount;
	}
	
	public void setMissCount(int value) {
		missCount = value;
	}
 	
	public void missOccured(int missedClientId, int requestedClientId, int data) {
		synchronized (missCount) {
			missCount++;
		}
		
		int fromClientId = missedClientId + 1; // Start looking from after the previously missed client
		int index = lookForData(requestedClientId, data, fromClientId);
//		System.out.println("Found data in client " + (index+1));
		
		if (index == Integer.MIN_VALUE) {
//			Retrieve and send data from server
			sendDataFromServer(requestedClientId, data);
		} else {
//			Forward it to client
//			System.out.println("Found data " + data + " in client " + (index+1));
//			System.out.println("Calling forwardDataRequest from missOccured");
			clients[index].forwardDataRequest(requestedClientId, data);
		}
	}

	/**
	 * @return the diskAccessCount
	 */
	public int getDiskAccessCount() {
		return diskAccessCount;
	}

	/**
	 * @param diskAccessCount the diskAccessCount to set
	 */
	public void setDiskAccessCount(int diskAccessCount) {
		this.diskAccessCount = diskAccessCount;
	}
	
	private void diskAccessOccured() {
		synchronized(diskAccessCount) {
			diskAccessCount++;
		}
	}
	
	/**
	 * Iterates through the singlet map to find the datablock that is cached in the most clients
	 * @param clientId Is the id of requesting client
	 * @return
	 */
	public Forward getVictim(int clientId) {

		synchronized(singletMap) {
			int maxOccurance = 2; // Making sure no Singlet is returned
			Integer maxOccuringData = null;

			Iterator<Integer> itr = singletMap.keySet().iterator();

			while (itr.hasNext()) {
				int currentData = itr.next();
				if (singletMap.get(currentData) >= maxOccurance) {
					maxOccurance = singletMap.get(currentData);
					maxOccuringData = currentData;
				}
			}

			// Get client id for max occuring data
			int resultClientId = Integer.MIN_VALUE;

			// Decrease count for maxOccuringData in singletMap
			if (maxOccuringData != null) {
				resultClientId = lookForData(clientId - 1, maxOccuringData, 0); // Start looking from the first bloom filter
//				singletMap.put(maxOccuringData, singletMap.get(maxOccuringData) - 1);
			}

			if (resultClientId == Integer.MIN_VALUE) {
				return new Forward(resultClientId, null);
			} else {
				return new Forward(resultClientId, new DataBlock(maxOccuringData));
			}
		}

	}

	@Override
	public int getSequenceOccurrence(int firstBlock) {
		Client[] clients = getClients();

		int counter = 0;
		for (int i = 0; i < clients.length; i++) {
			if (((ClientKSequence) clients[i]).isSequenceCached(firstBlock)) {
				counter++;
			}
		}

		return counter;
	}
	
	@Override
	public void printBloomFilters() {
		IABloomFilter[] bfs = getBloomFilters();
		for (int i = 0; i < bfs.length; i++) {
			int[] content = bfs[i].getArray();
//			System.out.println("********* Client " + (i+1) + "*********");
			for (int value : content) {
				System.out.print(value + " ");
			}
			System.out.println();
		}
	}

	@Override
	public int getAvgZeroCells() {
		int sum = 0;
		for (int i = 0; i < bloomFilters.length; i++) {
			sum += bloomFilters[i].getZeroCells();
		}
		return sum / bloomFilters.length;
	}

}
