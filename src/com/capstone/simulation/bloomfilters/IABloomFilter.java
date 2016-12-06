package com.capstone.simulation.bloomfilters;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.capstone.simulation.utility.Hash;

/**
 * This class represents Importance Aware Bloom Filter
 * @author Pavan Kumar
 *
 */
public class IABloomFilter implements BloomFilter{
	private int size;
	private final int P = 10;
	private final int M = 7;
	private int[] array;
	
	public IABloomFilter(int size) {
		this.setSize(size);
		array = new int[size];
	}

	/**
	 * @return the size
	 */
	public int getSize() {
		return size;
	}

	/**
	 * @param size the size to set
	 */
	public void setSize(int size) {
		this.size = size;
	}

	/**
	 * @return the p
	 */
	public int getP() {
		return P;
	}

	/**
	 * @return the m
	 */
	public int getM() {
		return M;
	}

	/**
	 * @return the array
	 */
	public int[] getArray() {
		return array;
	}

	/**
	 * @param array the array to set
	 */
	public void setArray(int[] array) {
		this.array = array;
	}
	
	public boolean isIndexSetToValue(int index, int value) {
		if (array[index] == value) {
			return true;
		}
		return false;
	}
	
	public boolean isIndexSet(int index) {
		index = Math.floorMod(index, getSize());
//		System.out.println("Value at index: " + index + " is " + array[index]);
		if (array[index] > 0) {
			return true;
		}
		return false;
	}
	
	public void setIndexToValue(int index, int value) {
		array[index] = value;
	}
	
	public void updateBF(int dataBlock, int importance) {
		if (!hasDataBlock(dataBlock)) {
			if(importance > getM()) {
//				Keeping importance in the range 0 - 7
				importance = getM();
			}
			List<Integer> pIndexes = new ArrayList<Integer>();
			Random rand = new Random();
			
			while (pIndexes.size() != P) {
				int index = rand.nextInt(size);
				if(!pIndexes.contains(index)) {
					pIndexes.add(index);
				}
			}
			
			for(int index : pIndexes) {
				if(array[index] >= 1) {
					array[index] -= 1;
				}
			}
			
			int[] arrayPositions = Hash.getInstance().generateHashValues(dataBlock);
			for(int position : arrayPositions) {
				position = Math.floorMod(position, getSize());
				if (importance == 0) {
					array[position] = 1;
				} else if(array[position] < importance) {
					array[position] = importance;
				}
			}
			
		}
	}
	
	public boolean hasDataBlock(int data) {
		int[] positions = Hash.getInstance().generateHashValues(data);
		
		for (int position : positions) {
			position = Math.floorMod(position, getSize());
			if (array[position] == 0) {
				return false;
			}
		}
		return true;
	}
	
	public int getZeroCells() {
		int count = 0;
		for (int i = 0; i < array.length; i++) {
			if (array[i] == 0) {
				count++;
			}
		}
		return count;
	}
	
}
