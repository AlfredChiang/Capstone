package com.capstone.simulation.utility;

import java.nio.ByteBuffer;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

public class Hash {
	private final static int DATA_SIZE = 4; // Represents data size of int in bytes
	private int hashFunctionsCount = 3;
	
	private static final Hash hash = new Hash();

	private Hash() {

	}

	public static Hash getInstance() {
		return hash;
	}
	
	/**
	 * @return the hashFunctionsCount
	 */
	public int getHashFunctionsCount() {
		return hashFunctionsCount;
	}

	/**
	 * @param hashFunctionsCount the hashFunctionsCount to set
	 */
	public void setHashFunctionsCount(int hashFunctionsCount) {
		this.hashFunctionsCount = hashFunctionsCount;
	}

	/**
     * Returns a 32-bit hash value.
     *
     * @return 32-bit hash value
     */
    public static int jenkins32(byte[] input) {
        int pc = 0;
        int pb = 0;

        return (int) jenkins(input, input.length, pc, pb, true);
    }
    
    /**
     * Jenkins algorithm.
     *
     * @param k           message on which hash is computed
     * @param length      message size
     * @param pc          primary init value
     * @param pb          secondary init value
     * @param is32BitHash true if just 32-bit hash is expected.
     *
     * @return
     */
    private static long jenkins(byte[] k, int length, int pc, int pb, boolean is32BitHash) {
        int a, b, c;

        a = b = c = 0xdeadbeef + length + pc;
        c += pb;

        int offset = 0;
        while (length > 12) {
            a += k[offset + 0];
            a += k[offset + 1] << 8;
            a += k[offset + 2] << 16;
            a += k[offset + 3] << 24;
            b += k[offset + 4];
            b += k[offset + 5] << 8;
            b += k[offset + 6] << 16;
            b += k[offset + 7] << 24;
            c += k[offset + 8];
            c += k[offset + 9] << 8;
            c += k[offset + 10] << 16;
            c += k[offset + 11] << 24;

            // mix(a, b, c);
            a -= c;
            a ^= rot(c, 4);
            c += b;
            b -= a;
            b ^= rot(a, 6);
            a += c;
            c -= b;
            c ^= rot(b, 8);
            b += a;
            a -= c;
            a ^= rot(c, 16);
            c += b;
            b -= a;
            b ^= rot(a, 19);
            a += c;
            c -= b;
            c ^= rot(b, 4);
            b += a;

            length -= 12;
            offset += 12;
        }

        switch (length) {
            case 12:
                c += k[offset + 11] << 24;
            case 11:
                c += k[offset + 10] << 16;
            case 10:
                c += k[offset + 9] << 8;
            case 9:
                c += k[offset + 8];
            case 8:
                b += k[offset + 7] << 24;
            case 7:
                b += k[offset + 6] << 16;
            case 6:
                b += k[offset + 5] << 8;
            case 5:
                b += k[offset + 4];
            case 4:
                a += k[offset + 3] << 24;
            case 3:
                a += k[offset + 2] << 16;
            case 2:
                a += k[offset + 1] << 8;
            case 1:
                a += k[offset + 0];
                break;
            case 0:
                return is32BitHash ? c : ((((long) c) << 32)) | ((long) b &0xFFFFFFFFL);
        }

        // Final mixing of thrree 32-bit values in to c
        c ^= b;
        c -= rot(b, 14);
        a ^= c;
        a -= rot(c, 11);
        b ^= a;
        b -= rot(a, 25);
        c ^= b;
        c -= rot(b, 16);
        a ^= c;
        a -= rot(c, 4);
        b ^= a;
        b -= rot(a, 14);
        c ^= b;
        c -= rot(b, 24);

        return is32BitHash ? c : ((((long) c) << 32)) | ((long) b &0xFFFFFFFFL);
    }

    private static long rot(int x, int distance) {
        return (x << distance) | (x >>> (32 - distance));
        // return (x << distance) | (x >>> -distance);
    }
    
    /**
	 * Calculates hash values for the data
	 * @param data for which hash values are calculated
	 * @return hash values as array of integers
	 */
    public synchronized int[] generateHashValues(int data) {
    	int[] hashValues = new int[getHashFunctionsCount()];
		byte[] dataBytes = ByteBuffer.allocate(DATA_SIZE).putInt(data).array();
		HashFunction murmur = Hashing.murmur3_32();

		for (int i = 0; i < hashValues.length; i++) {
			hashValues[i] = (Hash.jenkins32(dataBytes) + i * murmur.hashInt(data).asInt());
//			System.out.println("Generated " + i + "th hash " + hashValues[i] + " for data " + data);
		}
		return hashValues;
    }
    
}
