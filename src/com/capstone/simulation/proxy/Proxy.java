package com.capstone.simulation.proxy;

import com.capstone.simulation.bloomfilters.BloomFilter;
import com.capstone.simulation.client.Client;
import com.capstone.simulation.data.Forward;

public interface Proxy {

	public void setNumberOfClients(int numberOfClients);
	public void setHitCount(int value);
	public void setMissCount(int value);
	public void setDiskAccessCount(int diskAccessCount);
	public void setClients(Client[] clients);
	public void setSingletMap();
	public int getHitCount();
	public void hitOccured();
	public void setBloomFilterSize(int bloomFilterSize);
	public void setBloomFilters();
	public BloomFilter[] getBloomFilters();
	public int getMissCount();
	public int getDiskAccessCount();
	public void receiveDataRequest(int clientId, int data);
	public void addDataToBloomFilter(int clientId, int data, int importance);
	public boolean isDataSinglet(int clientId, int data);
	public void updateSingletMap(int data, boolean increment);
	public Forward getVictim(int clientId);
	public int lookForData(int clientId, int data, int fromClientId);
	public int getDataOccurrenceCount(int data);
	public int getSequenceOccurrence(int firstBlock);
	public void printBloomFilters();
	public int getAvgZeroCells();
}
