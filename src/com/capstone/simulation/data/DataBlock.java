package com.capstone.simulation.data;


/**
 * This class represents data block and the data is saved as an integer value
 * 
 * @author Pavan Kumar
 */
public class DataBlock{

	private int data;
	private int accessCount;

	public DataBlock(int data) {
		this.setData(data);
		this.setAccessCount(0);
	}
	/**
	 * @return the data
	 */
	public int getData() {
		return data;
	}

	/**
	 * @param data the data to set
	 */
	public void setData(int data) {
		this.data = data;
	}
	/**
	 * @return the accessCount
	 */
	public int getAccessCount() {
		return accessCount;
	}
	/**
	 * @param accessCount the accessCount to set
	 */
	public void setAccessCount(int accessCount) {
		this.accessCount = accessCount;
	}
	
}
