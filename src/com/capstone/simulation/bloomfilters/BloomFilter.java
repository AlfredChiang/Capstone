package com.capstone.simulation.bloomfilters;

import java.util.BitSet;

/**
 * This class represents a bloom filter
 * 
 * @author Pavan Kumar
 */
public class BloomFilter {
	
	private int size;
	private BitSet bitArray;
	
	/**
	 * Constructor
	 * @param size the size of bit array in bloom filter. It is also used to generate hash code between (0 - size)
	 */
	public BloomFilter(int size) {
		this.setSize(size);
		bitArray = new BitSet(this.size);
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
	private void setSize(int size) {
		this.size = size;
	}

	/**
	 * @return the bitArray
	 */
	public BitSet getBitArray() {
		return bitArray;
	}
	
	public boolean isBitSet(int position) {
		return bitArray.get(position);
	}
	
	public void setBit(int position) {
		bitArray.set(position);
	}
	
}
