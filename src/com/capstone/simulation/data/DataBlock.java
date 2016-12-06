package com.capstone.simulation.data;


/**
 * This class represents data block and the data is saved as an integer value
 * 
 * @author Pavan Kumar
 */
public class DataBlock implements Cloneable{

	private int data;
	private int accessCount;
	private int recirculationCount;
	private long timeStamp;
	public static final int RECIRCULATION_CONST = 2;

	public DataBlock(int data) {
		this.setData(data);
		this.setAccessCount(0);
		this.setRecirculationCount(RECIRCULATION_CONST);
		this.setTimeStamp(System.currentTimeMillis());
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
	/**
	 * @return the recirculationCount
	 */
	public int getRecirculationCount() {
		return recirculationCount;
	}
	/**
	 * @param recirculationCount the recirculationCount to set
	 */
	public void setRecirculationCount(int recirculationCount) {
		this.recirculationCount = recirculationCount;
	}
	
	/**
	 * @return the timeStamp
	 */
	public long getTimeStamp() {
		return timeStamp;
	}
	/**
	 * @param timeStamp the timeStamp to set
	 */
	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}
	@Override
	public boolean equals(Object o) {
		if (o == this) {
            return true;
        }
		if (!(o instanceof DataBlock)) {
            return false;
        }
		
		DataBlock d = (DataBlock) o;
		return getData() == d.getData(); 
	}
	
	@Override
	public int hashCode() {
		return data;
	}
	
	@Override
	public Object clone() {
		DataBlock copy = new DataBlock(this.getData());
		copy.setAccessCount(this.getAccessCount());
		return copy;
	}
	
}
