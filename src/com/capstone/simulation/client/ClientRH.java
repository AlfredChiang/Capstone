package com.capstone.simulation.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import com.capstone.simulation.data.DataBlock;
import com.capstone.simulation.data.Forward;

public class ClientRH extends Client{
	
	private LinkedList<Forward> singletForwardsQueue;
	private Map<Forward, Forward> singletMapping; // Mapping for singlet and its victim (in that order)
	private Random random;

	
	public ClientRH(int id, int cacheSize, CyclicBarrier barrier) {
		super(id, cacheSize, barrier);
		random = new Random();
		singletForwardsQueue = new LinkedList<Forward>();
		singletMapping = new HashMap<Forward, Forward>();
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
			
//			if (getSingletForwardsQueue().size() > 0) {
//				forwardSinglet(getSingletForwardsQueue().poll());
//			}
//			
//			if (getDueResponses().size() > 0) {
//				sendDataToClient();
//			}
//			System.out.println("Client: " + getId() + " still inside run while " + (getDueResponses().size() > 0) + " " + requestsIterator.hasNext() + " " + (getSingletForwardsQueue().size() > 0));
		}
//		System.out.println("Exited run for client: " + getId());
	}
	
	/**
	 * Caches the data block and updates bloom filter
	 * @param dataBlock
	 */
	@Override
	public void receiveData(DataBlock dataBlock) {
//		System.out.println("Receiving client: " + this.getId() + " data: " + dataBlock.getData());
		DataBlock removedBlock = null;
		synchronized (getCache()) {
//			Checking if received block is a singlet
			if (dataBlock.getRecirculationCount() != DataBlock.RECIRCULATION_CONST) {
				// If received block is a singlet, prevent ripple effect by
				// calling modified replacement algorithm
				addSingletToCache(dataBlock);
			} else {
				removedBlock = addDataToCache(dataBlock);
			}
		}
		
		if (removedBlock != null) {
//			Check if removedBlock is a singlet
//			System.out.println("Check if removedBlock is a singlet");
			boolean singlet = proxy.isDataSinglet(getId(), removedBlock.getData());
			if (singlet) {
//				System.out.println("Data is singlet");
				
//				Decrementing recirculation count by 1
				removedBlock.setRecirculationCount(removedBlock.getRecirculationCount() - 1);
				
				if (removedBlock.getRecirculationCount() >= 0) {
//					System.out.println("Client: " + getId() + " Singlet " + removedBlock.getData()  + " recirculation count: " + removedBlock.getRecirculationCount());
//					enqueueSinglet(removedBlock);
					Forward victim = proxy.getVictim(getId());
					
					if (victim == null || victim.getClientId() == Integer.MIN_VALUE) {
//						Jump to NChance model
//						System.out.println("Switched to NChance");
						int randomIndex = random.nextInt(getClients().length);
//						Making sure the Singlet is not forwarded to the same client
						if (getClients().length == 1) {
							return; // If only 1 client is there, don't forward the singlet
						}
						while((randomIndex + 1) == this.getId()) {
							randomIndex = random.nextInt(getClients().length);
						}
						getClients()[randomIndex].receiveData(removedBlock);
					} else {
//						System.out.println("Victim: client id: " + (victim.getClientId() + 1) + " victim block: " + victim.getDataBlock().getData());
						boolean sentSingletToVictim = false;
						while (!sentSingletToVictim && victim.getClientId()!= Integer.MIN_VALUE) {
							sentSingletToVictim = ((ClientRH)(getClients()[victim.getClientId()])).receiveSinglet(removedBlock, victim.getDataBlock());
							if (!sentSingletToVictim) {
								victim.setClientId(proxy.lookForData(getId() - 1, victim.getDataBlock().getData(), victim.getClientId() + 1));
//								System.out.println("Victim changed to: " + victim.getClientId());
							}
							
						}
						
					}
				} else {
//					Updating singlet map when removed block is a singlet and its recirculating count reached its minimum and discarded
					proxy.updateSingletMap(removedBlock.getData(), false);
				}
			} else {
//				Removed block is not null so discarded and singlet map is updated
				proxy.updateSingletMap(removedBlock.getData(), false);
			}
			
		}
		
		proxy.updateSingletMap(dataBlock.getData(), true); // Update occurrence count of dataBlock in singlet map
		
		int importance = 0;
		switch (getBloomFilterType()) {
		case IBF:
			importance = dataBlock.getAccessCount();
			break;
		default:
			importance = 0;
			break;
		}
		proxy.addDataToBloomFilter(getId(), dataBlock.getData(), importance);
//		System.out.println("Client " + getId() + " exited receive data for data: " + dataBlock.getData());
	}
	
	/**
	 * Replaces the replace object with singlet
	 * @param singlet
	 * @param replace Most occurring datablock
	 * @return True if replacement is successful. False otherwise
	 */
	public boolean receiveSinglet(DataBlock singlet, DataBlock replace) {
		synchronized (getCache()) {
			if (getCache().remove(replace) != null) {
				getCache().put(singlet, singlet.getData());
//				System.out.println("Replaced data: " + replace.getData() + " with data: "+ singlet.getData());
//				Update singlet map for removed block
//				No update for singlet
				proxy.updateSingletMap(replace.getData(), false);
				
				int importance = 0;
				switch (getBloomFilterType()) {
				case IBF:
					importance = singlet.getAccessCount();
					break;
				default:
					importance = 0;
					break;
				}
				proxy.addDataToBloomFilter(getId(), singlet.getData(), importance);
				return true;
			} else {
				return false;
			}
//			getCache().add(singlet);
		}
	}
	
	@Override
	public void addData(List<Integer> data) {
		for(int dataValue : data) {
			DataBlock block = new DataBlock(dataValue);
//			getCache().add(block);
			getCache().put(block, dataValue);
//			proxy.addDataToBloomFilter(getId(), dataValue);
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

	/**
	 * @return the singletMapping
	 */
	public Map<Forward, Forward> getSingletMapping() {
		return singletMapping;
	}

	/**
	 * @param singletMapping the singletMapping to set
	 */
	public void setSingletMapping(Map<Forward, Forward> singletMapping) {
		this.singletMapping = singletMapping;
	}

}
