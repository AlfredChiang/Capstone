package com.capstone.simulation.data;

public class Forward {
	
	int clientId;
	DataBlock dataBlock;
	
	public Forward(int clientId, DataBlock dataBlock) {
		setClientId(clientId);
		setDataBlock(dataBlock);
	}
	/**
	 * @return the clientId
	 */
	public int getClientId() {
		return clientId;
	}
	/**
	 * @param clientId the clientId to set
	 */
	public void setClientId(int clientId) {
		this.clientId = clientId;
	}
	/**
	 * @return the dataBlock
	 */
	public DataBlock getDataBlock() {
		return dataBlock;
	}
	/**
	 * @param dataBlock the dataBlock to set
	 */
	public void setDataBlock(DataBlock dataBlock) {
		this.dataBlock = dataBlock;
	}

}
