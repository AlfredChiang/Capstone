package com.capstone.simulation.client;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentLinkedDeque;

import com.capstone.simulation.bloomfilters.BloomFilter;
import com.capstone.simulation.data.DBComparator;
import com.capstone.simulation.data.DataBlock;
import com.capstone.simulation.proxy.Forward;
import com.capstone.simulation.proxy.Proxy;
import com.capstone.simulation.utility.Hash;

/**
 * This is an abstract class for client.
 * Clients implementing their specific algorithms must extend this class.
 *
 * @author Pavan Kumar
 */
public abstract class Client implements Runnable {

	private int id;
	private int cacheSize;
	private PriorityQueue<DataBlock> cache; // Least Frequently Used caching algorithm
	private ConcurrentLinkedDeque<Forward> dueResponses; // responses due for other clients
	private ArrayList<Integer> requests;
	private Client[] clients;
	private BloomFilter bloomFilter;
	private Proxy proxy;
	
	private int bfSize;
	
	/**
	 * Default constructor for this class
	 */
	public Client() {
		
	}
	
	public Client(int id, int cacheSize) {
		this.setId(id);
		this.setCacheSize(cacheSize);
		initializeCache();
		requests = new ArrayList<Integer>();
		proxy = Proxy.getInstance();
		bfSize = cacheSize; //	TODO Size of bloomfilter has to be calculated later
		bloomFilter = new BloomFilter(bfSize);
	}
	
	public Client(int id, int cacheSize, DataBlock[] dataBlocks) {
		this.setId(id);
		this.setCacheSize(cacheSize);
		initializeCache();
	}

	/**
	 * This method only sends request to the proxy. Data receiving will be handled in a different method 
	 * @param data Requesting data
	 */
	public void sendDataRequest(int data) {
		proxy.receiveDataRequest(getId(), data);
	}
	
	/**
	 * Caches the data block and updates bloom filter
	 * @param dataBlock
	 */
	public synchronized void receiveData(DataBlock dataBlock) {
		cache.add(dataBlock);
		int[] hashValues = Hash.getInstance().generateHashValues(dataBlock.getData());
		for (int index : hashValues) {
			bloomFilter.setBit(index);
		}
	}
	
	/**
	 * Adds a list of integers to the cache
	 * @param data
	 */
	public void addData(List<Integer> data) {
		for(int dataValue : data) {
			DataBlock block = new DataBlock(dataValue);
			cache.add(block);
		}
	}
	
	/**
	 * Enqueues data forwarding on client
	 * @param data
	 */
	public void forwardDataRequest(int clientId, int data) {
		Forward forward = new Forward(clientId, data);
		dueResponses.add(forward);
	}
	
	/**
	 * Picks first forward from dueResponses queue and sends the data
	 */
	public void sendDataToClient() {
		Forward head = dueResponses.poll();
		if (head != null) {
			int cliendId = head.getClientId();
			int data = head.getData();
			DataBlock dataBlock = new DataBlock(data);
			clients[cliendId].receiveData(dataBlock);
		}
	}
	
	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	private void setId(int id) {
		this.id = id;
	}

	/**
	 * @return the cacheSize
	 */
	public int getCacheSize() {
		return cacheSize;
	}

	/**
	 * @param cacheSize the cacheSize to set
	 */
	public void setCacheSize(int cacheSize) {
		this.cacheSize = cacheSize;
	}

	/**
	 * @return the cache
	 */
	public PriorityQueue<DataBlock> getCache() {
		return cache;
	}

	/**
	 * @param cache the cache to set
	 */
	public void setCache(PriorityQueue<DataBlock> cache) {
		this.cache = cache;
	}

	private void initializeCache() {
		cache = new PriorityQueue<DataBlock>(cacheSize, new DBComparator());
	}

	/**
	 * @return the dueResponses
	 */
	protected ConcurrentLinkedDeque<Forward> getDueResponses() {
		return dueResponses;
	}

	/**
	 * @param dueResponses the dueResponses to set
	 */
	protected void setDueResponses(ConcurrentLinkedDeque<Forward> dueResponses) {
		this.dueResponses = dueResponses;
	}

	/**
	 * @return the requests
	 */
	public ArrayList<Integer> getRequests() {
		return requests;
	}

	/**
	 * @param requests the requests to set
	 */
	public void setRequests(List<Integer> requests) {
		this.requests.addAll(requests);
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
	 * @return the bloomFilter
	 */
	protected BloomFilter getBloomFilter() {
		return bloomFilter;
	}

	/**
	 * @param bloomFilter the bloomFilter to set
	 */
	protected void setBloomFilter(BloomFilter bloomFilter) {
		this.bloomFilter = bloomFilter;
	}
	
}
