package com.capstone.simulation.proxy;

import com.capstone.simulation.utility.BloomFilterType;

public class ProxyFactory {

	public static Proxy buildProxy(BloomFilterType bloomFilterType) {
		Proxy proxy = null;
		
		switch (bloomFilterType) {
			case Standard:
				proxy = ProxyBF.getInstance();
				break;
				
			case IBF:
				proxy = ProxyIBF.getInstance();
				break;
				
			default:
				System.out.println("Bloom filter type not allowed");
				System.exit(0);
				break;
		}
		return proxy;
	}
}
