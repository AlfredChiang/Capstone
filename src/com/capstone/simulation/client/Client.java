package com.capstone.simulation.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CyclicBarrier;

import com.capstone.simulation.data.DataBlock;
import com.capstone.simulation.data.Forward;
import com.capstone.simulation.proxy.Proxy;
import com.capstone.simulation.utility.BloomFilterType;

/**
 * This is an abstract class for client.
 * Clients implementing their specific algorithms must extend this class.
 *
 * @author Pavan Kumar
 */
public abstract class Client implements Runnable {

	private int id;
	private int cacheSize;
	private LinkedHashMap<DataBlock, Integer> cache; // Least Recently Used caching algorithm
	private ConcurrentLinkedDeque<Forward> dueResponses; // responses due for other clients
	private ArrayList<Integer> requests;
	private Client[] clients;
	protected Proxy proxy;
	private BloomFilterType bloomFilterType;
	
	private CyclicBarrier barrier;
	
	/**
	 * Default constructor for this class
	 */
	public Client() {
		
	}
	
	public Client(int id, int cacheSize, CyclicBarrier barrier) {
		this.setId(id);
		this.setCacheSize(cacheSize);
		setBarrier(barrier);
		initializeCache();
		requests = new ArrayList<Integer>();
//		proxy = ProxyBF.getInstance();
		
		dueResponses = new ConcurrentLinkedDeque<Forward>();
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

		DataBlock requestData = new DataBlock(data);
//		Send request if local cache doesn't have data
		if (!getCache().containsKey(requestData)) {
//			System.out.println("Client " + getId() + " sending request for " + data);
			proxy.receiveDataRequest(getId(), data);
		} else {
//			Data is present in the local cache. So, count a cache hit
			proxy.hitOccured();
		}
		
	}

	public abstract void receiveData(DataBlock dataBlock);
	
	/**
	 * Checks the cache size and adds new datablock to it. If the cache is full it removes the head of cache.
	 */
	protected DataBlock addDataToCache(DataBlock dataBlock) {
		DataBlock head = null;
		if (!(getCache().size() < getCacheSize())) {
//			head = getCache().poll();
//			getCache().add(dataBlock);
			DataBlock[] blocks = new DataBlock[getCache().size()];
			getCache().keySet().toArray(blocks);
			if (blocks.length > 0) {
				head = blocks[0];
				getCache().remove(head);
			}
		}
		getCache().put(dataBlock, dataBlock.getData());
//		System.out.println("Added data to cache");
		return head;
	}
	
	/**
	 * Adding singlet to cache using a modified replacement algorithm
	 * Step 1: It first iterates backwards (reverse insertion order) to discard the last duplicated block
	 * Step 2: If there are no duplicated blocks, then it discards the oldest recirculating block with fewest recirculation count
	 * @param singlet
	 */
	protected void addSingletToCache(DataBlock singlet) {
		boolean discarded = false;
		ListIterator<DataBlock> iter = new ArrayList<DataBlock>(getCache().keySet()).listIterator(getCache().size());
		while(iter.hasPrevious()) {
			DataBlock currentBlock = iter.previous();
			if (currentBlock.getAccessCount() > 0) {
				iter.remove();
				discarded = true;
				break;
			}
		}
		
		if (!discarded) {
			int leastRecirculationCount = DataBlock.RECIRCULATION_CONST;
			DataBlock[] blocks = new DataBlock[getCache().size()];
			getCache().keySet().toArray(blocks);
			DataBlock discardingBlock = blocks[blocks.length - 1];
			iter =  new ArrayList<DataBlock>(getCache().keySet()).listIterator(getCache().size());
			
			while (iter.hasPrevious()) {
				DataBlock currentBlock = iter.previous();
				if (currentBlock.getRecirculationCount() < leastRecirculationCount) {
					leastRecirculationCount = currentBlock.getRecirculationCount();
					discardingBlock = currentBlock;
				}
			}
			
			getCache().remove(discardingBlock);
		}
		getCache().put(singlet, singlet.getData());
	}
	
	public abstract void addData(List<Integer> data);
	
	/**
	 * Forwards data to requested client
	 * @param clientId
	 * @param data
	 * @return True if the data has been successfully forwarded. False otherwise.
	 */
	public boolean forwardDataRequest(int clientId, int data) {
		DataBlock forwardData = new DataBlock(data);
//		Forward forward = new Forward(clientId, forwardData);
		
		if (getCache().containsKey(forwardData)) {
//			System.out.println("Client " + getId() + " forwarding data " + data + " to client " + (clientId + 1));
//			proxy.hitOccured();
//			dueResponses.add(forward);
//			Increment the accessCount
			DataBlock localBlock = getBlock(data);
			localBlock.setAccessCount(localBlock.getAccessCount() + 1);
			localBlock.setRecirculationCount(DataBlock.RECIRCULATION_CONST);
			clients[clientId].receiveData((DataBlock)localBlock.clone());
			return true;
		} else {
//			System.out.println("Miss occured on client: " + getId() + " requested client: "+ (clientId + 1) + " for data: " + data);
//			proxy.missOccured(getId(), clientId, data);
			return false;
		}
		
	}
	
	/**
	 * Picks first forward from dueResponses queue and sends the data
	 */
	public void sendDataToClient() {
		Forward head = dueResponses.poll();
		if (head != null) {
			int clientId = head.getClientId();
			DataBlock dataBlock = head.getDataBlock();
//			System.out.println("Client " + getId() + " sending data " + dataBlock.getData() + " to client " + (clientId+1));
//			SimLogger.getInstance().myLogger.log(Level.INFO, "LOG: Client " + getId() + " sending data " + dataBlock.getData() + " to client " + (clientId+1));
			clients[clientId].receiveData(dataBlock);
		}
	}
	
	/**
	 * Adds all the cache data to its corresponding Bloom filter in proxy
	 */
	public void updateAllCacheToProxy() {
		Iterator<DataBlock> iter = cache.keySet().iterator();
		while(iter.hasNext()) {
			int importance = 0;
			DataBlock dataBlock = iter.next();
			
			switch (getBloomFilterType()) {
			case IBF:
				importance = dataBlock.getAccessCount();
				break;
			default:
				importance = 0;
			}
			
			proxy.addDataToBloomFilter(getId(), dataBlock.getData(), importance);
		}
	}
	
	/**
	 * Returns a datablock with given data
	 * @param data
	 * @return
	 */
	protected DataBlock getBlock(int data) {
		Iterator<DataBlock> iter = getCache().keySet().iterator();
		
		while(iter.hasNext()) {
			DataBlock currentBlock = iter.next();
			if (currentBlock.getData() == data) {
				return currentBlock;
			}
		}
		return null;
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
	public LinkedHashMap<DataBlock, Integer> getCache() {
		return cache;
	}

	/**
	 * @param cache the cache to set
	 */
	public void setCache(LinkedHashMap<DataBlock, Integer> cache) {
		this.cache = cache;
	}

	private void initializeCache() {
//		cache = new PriorityQueue<DataBlock>(cacheSize, new DBComparator());
		cache = new LinkedHashMap<DataBlock, Integer>(cacheSize);
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
	 * @return the barrier
	 */
	public CyclicBarrier getBarrier() {
		return barrier;
	}

	/**
	 * @param barrier the barrier to set
	 */
	public void setBarrier(CyclicBarrier barrier) {
		this.barrier = barrier;
	}

	public abstract void run();

	/**
	 * @return the proxy
	 */
	public Proxy getProxy() {
		return proxy;
	}

	/**
	 * @param proxy the proxy to set
	 */
	public void setProxy(Proxy proxy) {
		this.proxy = proxy;
	}

	/**
	 * @return the bloomFilterType
	 */
	public BloomFilterType getBloomFilterType() {
		return bloomFilterType;
	}

	/**
	 * @param bloomFilterType the bloomFilterType to set
	 */
	public void setBloomFilterType(BloomFilterType bloomFilterType) {
		this.bloomFilterType = bloomFilterType;
	}
	
}
