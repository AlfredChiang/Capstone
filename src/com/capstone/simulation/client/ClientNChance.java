package com.capstone.simulation.client;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import com.capstone.simulation.data.DataBlock;
import com.capstone.simulation.data.Forward;

public class ClientNChance extends Client {
	
	private LinkedList<Forward> singletForwardsQueue; 
	private Random random;

	
	public ClientNChance(int id, int cacheSize, CyclicBarrier barrier) {
		super(id, cacheSize, barrier);
		random = new Random();
		singletForwardsQueue = new LinkedList<Forward>();
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
		
		Iterator<Integer> requestsIterator = getRequests().iterator();

		while (requestsIterator.hasNext()) {
//			System.out.println("Entered loop");
			if (requestsIterator.hasNext()) {
				sendDataRequest(requestsIterator.next());
			}
			
		}
	}
	
	/**
	 * Caches the data block and updates bloom filter
	 * @param dataBlock
	 */
	@Override
	public void receiveData(DataBlock dataBlock) {
		DataBlock removedBlock = null;
		synchronized (getCache()) {
//			System.out.println("Receiving client: " + this.getId() + " data: " + dataBlock.getData());

			// Checking if received block is a singlet
			if (dataBlock.getRecirculationCount() != DataBlock.RECIRCULATION_CONST) {
				// If received block is a singlet, prevent ripple effect by
				// calling modified replacement algorithm
				addSingletToCache(dataBlock);
			} else {
				removedBlock = addDataToCache(dataBlock);
			}
		}
		if (removedBlock != null) {
			// Check if removedBlock is a singlet
			boolean singlet = proxy.isDataSinglet(getId(), removedBlock.getData());
			if (singlet) {
//				System.out.println("Data is singlet");
				removedBlock.setRecirculationCount(removedBlock.getRecirculationCount() - 1);

				if (removedBlock.getRecirculationCount() >= 0) {
//					System.out.println("Client: " + getId() + " Singlet " + removedBlock.getData()
//							+ " recirculation count: " + removedBlock.getRecirculationCount());

					// ***** Code from enqueueSinglet *****
					int randomIndex = random.nextInt(getClients().length);
					// Making sure the Singlet is not forwarded to the same
					// client
					if (getClients().length == 1) {
						return; // If only 1 client is there, don't forward the
								// singlet
					}
					while ((randomIndex + 1) == this.getId()) {
						randomIndex = random.nextInt(getClients().length);
					}
					// ***** Code from enqueueSinglet *****
					getClients()[randomIndex].receiveData(removedBlock);

					// enqueueSinglet(removedBlock);
				}
			} else {
				// Removed block is not null so discarded and singlet map is
				// updated
				proxy.updateSingletMap(removedBlock.getData(), false);
			}

		}

		int importance = 0;
		switch (getBloomFilterType()) {
		case IBF:
			importance = dataBlock.getAccessCount();
			break;
		default:
			importance = 0;
			break;
		}
		proxy.updateSingletMap(dataBlock.getData(), true); // Update occurrence
															// count of
															// dataBlock in
															// singlet map
		proxy.addDataToBloomFilter(getId(), dataBlock.getData(), importance);
		// System.out.println("Exited receive data");
	}
	
	@Override
	public void addData(List<Integer> data) {
		for(int dataValue : data) {
			DataBlock block = new DataBlock(dataValue);
			getCache().put(block, dataValue);
//			System.out.println("");
			proxy.updateSingletMap(dataValue, true);
		}
	}

	/**
	 * @return the singletForwardsQueue
	 */
	public LinkedList<Forward> getSingletForwardsQueue() {
		return singletForwardsQueue;
	}

	/**
	 * @param singletForwardsQueue the singletForwardsQueue to set
	 */
	public void setSingletForwardsQueue(LinkedList<Forward> singletForwardsQueue) {
		this.singletForwardsQueue = singletForwardsQueue;
	}

}