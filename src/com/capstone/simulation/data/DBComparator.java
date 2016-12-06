package com.capstone.simulation.data;

import java.util.Comparator;

/**
 * This class implements Comparator for DataBlock based on access count
 * 
 * @author Pavan Kumar
 */
public class DBComparator implements Comparator<DataBlock>{


	@Override
	/**
	 * Compares access count and timestamp(maintains FIFO)
	 */
	public int compare(DataBlock block1, DataBlock block2) {
		if (block1 != null && block2 != null) {
			if (block1.getAccessCount() > block2.getAccessCount()) {
				return 1;
			} else if (block1.getAccessCount() < block2.getAccessCount()) {
				return -1;
			} else {
				if (block1.getTimeStamp() > block2.getTimeStamp()) {
					return 1;
				} else if (block1.getAccessCount() < block2.getAccessCount()) {
					return -1;
				} else {
					return 0;
				}
			}
		} else if (block1 != null) {
			return 1;
		} else if (block2 != null) {
			return -1;
		}
		return 0;
	}

}
