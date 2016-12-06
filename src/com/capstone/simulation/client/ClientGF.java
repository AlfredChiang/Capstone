package com.capstone.simulation.client;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import com.capstone.simulation.data.DataBlock;

public class ClientGF extends Client{

	public ClientGF(int id, int cacheSize, CyclicBarrier barrier) {
		super(id, cacheSize, barrier);
	}

	@Override
	public void run() {

		try {
			getBarrier().await(); // Waiting for the barrier. This is done to
									// make sure all the clients begin execution
									// at the same time.
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		SimLogger.getInstance().myLogger.log(Level.INFO, "Client: " + getId() + " is Up");

		Iterator<Integer> iterator = getRequests().iterator();

		while ((getDueResponses().size() > 0) || iterator.hasNext()) {
			if (iterator.hasNext()) {
				sendDataRequest(iterator.next());
			}
			if (getDueResponses().size() > 0) {
				sendDataToClient();
			}
			// System.out.println(getId() + " " + x + " " +
			// (iterator.hasNext()));
		}
		// System.out.println("Exited client " + this.getId());
	}
	
	/**
	 * Caches the data block and updates bloom filter
	 * 
	 * @param dataBlock
	 */
	@Override
	public void receiveData(DataBlock dataBlock) {

		DataBlock removedBlock = null;
		int importance = 0;
		synchronized (getCache()) {
			removedBlock = addDataToCache(dataBlock);
//			System.out.println("Removed block: " + removedBlock.getData());
		}

		switch (getBloomFilterType()) {
		case IBF:
			importance = dataBlock.getAccessCount();
			break;
		default:
			importance = 0;
			break;
		}
		proxy.addDataToBloomFilter(getId(), dataBlock.getData(), importance);
	}
	
	/**
	 * Adds a list of integers to the cache
	 * @param data
	 */
	@Override
	public void addData(List<Integer> data) {
		for(int dataValue : data) {
			DataBlock block = new DataBlock(dataValue);
//			getCache().add(block);
			getCache().put(block, dataValue);
		}
	}
}
