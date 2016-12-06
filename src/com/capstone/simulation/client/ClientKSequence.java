package com.capstone.simulation.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import com.capstone.simulation.data.DataBlock;

public class ClientKSequence extends Client {

	private int k;
	private Random random;
	private List<Integer> sequencesList;
	

	public ClientKSequence (int id, int cacheSize, CyclicBarrier barrier, int k) {
		super(id, cacheSize, barrier);
		this.setK(k);
		random = new Random();
		setSequencesList();
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
			sendDataRequest(requestsIterator.next());
		}
	}
	
	@Override
	public void receiveData(DataBlock dataBlock) {
		DataBlock removedBlock = null;
		
		synchronized(getCache()) {
//			System.out.println("Receiving client: " + this.getId() + " data: " + dataBlock.getData());
			removedBlock = super.addDataToCache(dataBlock);
		}
		
//		Add datablock to sequenceList if it forms a new sequence in cache
		boolean sequencePresent = isSequencePresentForBlock(dataBlock.getData());
//		System.out.println("Sequence status for dataValue: " + dataBlock.getData() + " XXX" + sequencePresent);
		int sequenceFirstBlock = (dataBlock.getData() - (dataBlock.getData()%getK()) + 1);
		if (sequencePresent && !sequencesList.contains(sequenceFirstBlock)) {
//			System.out.println("Adding dataValue: " + dataBlock.getData() + " to sequence list");
			getSequencesList().add(dataBlock.getData());
		}
				
		if (isSequencePresentForBlock(dataBlock.getData())) {
//			System.out.println("Adding dataValue: " + dataBlock.getData() + " to sequence list");
			getSequencesList().add(dataBlock.getData());
		}
		if (removedBlock != null) {
//			Check if removedBlock is a singlet
			ArrayList<DataBlock> blocksInSequence = getDataBlockSequence(removedBlock);
			
			int sequenceOccurrence = proxy.getSequenceOccurrence(removedBlock.getData());
			
			int randomIndex = random.nextInt(getClients().length);
			while((randomIndex + 1) == this.getId()) {
				randomIndex = random.nextInt(getClients().length);
			}
			
			if (blocksInSequence.size() == getK()) {
//				Complete Sequence
				if (sequenceOccurrence == 1) {
//					Handling Singlet: Forwarding the whole sequence to a random client
					((ClientKSequence)getClients()[randomIndex]).receiveForward(blocksInSequence);
				}
//				Block with multiple sets of sequences will be discarded
			} else {
//				Incomplete Sequence
				if (sequenceOccurrence == 0) {
//					Incomplete singlet
					((ClientKSequence)getClients()[randomIndex]).receiveForward(blocksInSequence);
				}
//				Block part of an incomplete sequence will be discarded
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
		proxy.addDataToBloomFilter(getId(), dataBlock.getData(), importance);
	}
	
	/**
	 * 
	 * @param singletList
	 */
	public void receiveForward(ArrayList<DataBlock> singletList) {
		if (singletList.size() != 0) {
//			System.out.print("Client: " + getId() + " received singletList ");
			for (int i = 0; i < singletList.size(); i++) {
//				System.out.print(singletList.get(i).getData() + " ");
			}
//			System.out.println();
			
			List<Integer> subSequence = getSubSequence(singletList.get(0));
			for(DataBlock block : singletList) {
				super.addDataToCache(block);
			}
			
//			Check how many datablocks of the singletList is already present in the cache
			if (subSequence.size() == 0) {
//				1. No datablock of the singletList is present in the cache
//				2. Check which of the sequences present is and non singlet and the oldest and replace it with the singletList
				
				 
			} else {
//				When there are some blocks from singletList present in the cache then add the datablocks
//				that are not already present should be added.
				
			}
			
		}
	}
	
	@Override
	protected DataBlock addDataToCache(DataBlock dataBlock) {
//		System.out.println("AddDataToCache in KSequence");
		DataBlock head = null;
		getCache();
		return head;
	}
	
	/**
	 * Checks if a block is part of a complete sequence in the cache 
	 * @param block
	 * @return True if block is part of a complete sequence. False otherwise
	 */
	private ArrayList<DataBlock> getDataBlockSequence (DataBlock block) {
		int data = block.getData();
		int positionInSequence = data % getK();
		ArrayList<DataBlock> list = new ArrayList<DataBlock>();
		for (int i = 0; i < getK(); i++) {
			int currentBlockData = data + i - positionInSequence + 1;
			DataBlock currentBlock = new DataBlock(currentBlockData);
			if (getCache().containsKey(currentBlock)) {
				list.add(currentBlock);
			}
		}
		return list;
	}
	
	/**
	 * Checks if a sequence is singlet.
	 * A sequence is considered a Singlet if one or more blocks of it has only one copy present in the system. 
	 * @param firstBlock
	 * @return true if sequence is a singlet. False otherwise
	 */
	private boolean isSequenceSinglet(int firstBlock) {
		for (int i = firstBlock ; i <= getK(); i ++) {
			if (proxy.isDataSinglet(getId(), i)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns a subsequence of datablocks present in a given sequence
	 * Here sequence is indicated by the first block and K (sequence length) values
	 * @param firstBlock
	 * @return list of blocks that are present in the cache
	 */
	public List<Integer> getSubSequence(DataBlock firstBlock) {
		List<Integer> list = new ArrayList<Integer>();

		int firstBlockData = firstBlock.getData();
		for (int i = firstBlockData; i < firstBlockData + getK(); i++) {
			if (getCache().containsValue(i)) {
				list.add(i);
			}
		}
		return list;
	}

	@Override
	public void addData(List<Integer> data) {
		for(int dataValue : data) {
			DataBlock block = new DataBlock(dataValue);
			getCache().put(block, dataValue);
//			proxy.addDataToBloomFilter(getId(), dataValue);
			proxy.updateSingletMap(dataValue, true);
		}
		
		for(int dataValue : data) {
			boolean sequencePresent = isSequencePresentForBlock(dataValue);
//			System.out.println("Sequence status for dataValue: " + dataValue + " " + sequencePresent);
			int mod = dataValue%getK();
			int sequenceFirstBlock = (dataValue - (mod == 0 ? getK() : mod) + 1);
			
			if (sequencePresent && !sequencesList.contains(sequenceFirstBlock)) {
//				System.out.println("Adding dataValue: " + sequenceFirstBlock + " to sequence list");
				getSequencesList().add(sequenceFirstBlock);
			}
		}
	}
	
	private boolean isSequencePresentForBlock(int block) {
		if (getK() == 1) {
			return false;
		}
		int positionInSequence = block % getK();
		boolean sequencePresent = true;
		
		if (positionInSequence == 0) {
			for (int i = getK(); i >= 1; i--) {
				int currentBlockData = block - i + 1;
				if (!getCache().containsValue(currentBlockData)) {
					sequencePresent = false;
					break;
				}
			}
		} else {
			for (int i = 0; i < getK(); i++) {
				int currentBlockData = block + i - positionInSequence + 1;
				if (!getCache().containsValue(currentBlockData)) {
					sequencePresent = false;
					break;
				}
			}
		}
		
		return sequencePresent;
	}
	
	@Override
	public boolean forwardDataRequest(int clientId, int data) {
//		System.out.println("Entered forwardDataRequest in KSequence");
		DataBlock forwardData = new DataBlock(data);
		
		if (getCache().containsKey(forwardData)) {
//			boolean sequencePresent = isSequencePresentForBlock(data);
			DataBlock localBlock = super.getBlock(data);
			ArrayList<DataBlock> blocksInSequence = getDataBlockSequence(localBlock);
			((ClientKSequence)getClients()[clientId]).receiveForward(blocksInSequence);
//			if (sequencePresent) {
//				ArrayList<DataBlock> blocksInSequence = getDataBlockSequence(localBlock);
//				((ClientKSequence)getClients()[clientId]).receiveForward(blocksInSequence);
//			} else {
//				getClients()[clientId].receiveData((DataBlock)localBlock.clone());
//			}
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * @return the k
	 */
	public int getK() {
		return k;
	}
	/**
	 * @param k the k to set
	 */
	public void setK(int k) {
		this.k = k;
	}

	/**
	 * @return the sequencesList
	 */
	public List<Integer> getSequencesList() {
		return sequencesList;
	}

	/**
	 * @param sequencesList the sequencesList to set
	 */
	public void setSequencesList() {
		this.sequencesList = new ArrayList<Integer>();
	}
	
	/**
	 * Checks if a sequence is present in the cache
	 * @param firstBlock
	 * @return
	 */
	public boolean isSequenceCached(int firstBlock) {
		return getSequencesList().contains(firstBlock);
	}
}
