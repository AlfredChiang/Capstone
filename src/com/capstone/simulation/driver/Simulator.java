package com.capstone.simulation.driver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CyclicBarrier;

import com.capstone.simulation.client.Client;
import com.capstone.simulation.client.ClientGF;
import com.capstone.simulation.client.ClientKSequence;
import com.capstone.simulation.client.ClientMyAlgo;
import com.capstone.simulation.client.ClientNChance;
import com.capstone.simulation.client.ClientRH;
import com.capstone.simulation.proxy.Proxy;
import com.capstone.simulation.proxy.ProxyFactory;
import com.capstone.simulation.server.Server;
import com.capstone.simulation.utility.Algorithm;
import com.capstone.simulation.utility.BloomFilterType;
import com.capstone.simulation.utility.ExperimentVariable;
import com.capstone.simulation.utility.Strings;

/**
 * This class takes a properties file as input Instantiates client objects,
 * server and proxy based upon the algorithm
 * 
 * @author Pavan Kumar
 *
 */
public class Simulator {
	
	private final int HASH_FUNCTIONS_COUNT = 3;
	
	private final float LOG_2 = 0.69f; 
	
	private int clientCount; // This variable is used when client count is
								// constant in the experiment
	private int minClientCount; // This is used when client count is the
								// variable in the experiment
	private int clientCountIncrement; // This is used when client count is the
										// variable in the experiment
	private int maxClientCount; // This is used when client count is the
								// variable in the experiment

	private int cacheSize; // This variable is used when cache size is constant
							// in the experiment
	private int minCacheSize; // This is used when cache size is the variable in
								// the experiment
	private int cacheSizeIncrement; // This is used when cache size is the
									// variable in the experiment
	private int maxCacheSize; // This is used when cache size is the variable in
								// the experiment
	private int diskSize; //The data ranges from [0 - diskSize) 

	private Algorithm algorithm;
	
	private BloomFilterType bloomFilterType;
	private ExperimentVariable experimentVariable;

	private String traceFile;
	private ArrayList<Integer> dataSet;

	private Client[] clients;
	private Server server;
	private Proxy proxy;
	private float requestSizeFactor;
	private CyclicBarrier barrier;
	private int k;

	public static void main(String[] args) {
		// Exit if config file is not provided.
		if (args.length == 0) {
			System.out.println("Config file missing");
			System.exit(0);
		}
		Simulator simulator = new Simulator();
		String configFile = args[0];

		simulator.loadProperties(configFile);
//		simulator.loadTraceData();
		simulator.beginExperiment(simulator.clientCount, simulator.cacheSize, simulator.algorithm,
				simulator.experimentVariable);

	}

	/**
	 * 
	 * @param configFile
	 */
	private void loadProperties(String configFile) {
		InputStream inputStream;

		try {
			inputStream = new FileInputStream(configFile);
			Properties configProperties = new Properties();
			configProperties.load(inputStream);

			clientCount = Integer.parseInt(configProperties.containsKey(Strings.clientCount)
					? configProperties.getProperty(Strings.clientCount)
					: configProperties.getProperty(Strings.clientCountMin));
			minClientCount = Integer.parseInt(configProperties.containsKey(Strings.clientCountMin)
					? configProperties.getProperty(Strings.clientCountMin) : Strings.zero);
			maxClientCount = Integer.parseInt(configProperties.containsKey(Strings.clientCountMax)
					? configProperties.getProperty(Strings.clientCountMax) : Strings.zero);
			clientCountIncrement = Integer.parseInt(configProperties.containsKey(Strings.clientCountIncrement)
					? configProperties.getProperty(Strings.clientCountIncrement) : Strings.zero);

			cacheSize = Integer.parseInt(
					configProperties.containsKey(Strings.cacheSize) ? configProperties.getProperty(Strings.cacheSize)
							: configProperties.getProperty(Strings.cacheSizeMin));
			minCacheSize = Integer.parseInt(configProperties.containsKey(Strings.cacheSizeMin)
					? configProperties.getProperty(Strings.cacheSizeMin) : Strings.zero);
			maxCacheSize = Integer.parseInt(configProperties.containsKey(Strings.cacheSizeMax)
					? configProperties.getProperty(Strings.cacheSizeMax) : Strings.zero);
			cacheSizeIncrement = Integer.parseInt(configProperties.containsKey(Strings.cacheSizeIncrement)
					? configProperties.getProperty(Strings.cacheSizeIncrement) : Strings.zero);

			algorithm = Algorithm.valueOf(configProperties.getProperty(Strings.algorithmType));
			
			bloomFilterType = BloomFilterType.valueOf(configProperties.getProperty(Strings.bloomFilterType));

			experimentVariable = ExperimentVariable.valueOf(configProperties.getProperty(Strings.experimentVariable));
			
			diskSize = Integer.parseInt(configProperties.containsKey(Strings.diskSize)
					? configProperties.getProperty(Strings.diskSize) : Strings.zero);
			
			requestSizeFactor = Float.parseFloat(configProperties.containsKey(Strings.requestSizeFactor)
					? configProperties.getProperty(Strings.requestSizeFactor) : Strings.one);

			traceFile = configProperties.getProperty(Strings.traceFile);
		} catch (NumberFormatException nfe) {
			// TODO Add log here
			System.exit(0);
		} catch (IOException e) {
			// TODO Add log here
			System.exit(0);
		} catch (NullPointerException e) {
			// TODO Add log here
			System.exit(0);
		}

	}

	/**
	 * 
	 * @param clientSize
	 *            Minimum client size the experiment begins with
	 * @param cacheSize
	 *            Minimum cache size the experiment begins with
	 * @param algorithmType
	 *            Type of cooperative caching algorithm to run
	 * @param experimentVariable
	 *            Name of variable in this experiment
	 */
	private void beginExperiment(int clientSize, int cacheSize, Algorithm algorithmType,
			ExperimentVariable experimentVariable) {
		int currentClientSize = clientSize;
		int currentCacheSize = cacheSize;
		boolean doneExperiment = false;

		while (!doneExperiment) {

			k = 5;
			float bfSize = (float)(HASH_FUNCTIONS_COUNT * diskSize) / LOG_2;
			initialize(currentClientSize, currentCacheSize, algorithmType);
//			System.out.println("Setting Bloom filter size to: " + (int)bfSize);
			proxy.setBloomFilterSize((int)bfSize);
			proxy.setBloomFilters();
//			allocateRandomData(currentClientSize, currentCacheSize, diskSize);
//			allocateUniqueTraceDataUniqueClientsData(currentClientSize, currentCacheSize, diskSize);
//			allocateTraceDataInAllClients(currentClientSize, currentCacheSize, diskSize);
//			allocateTraceDataInOneClient(currentClientSize, currentCacheSize, diskSize);
//			allocateRandomTraceDataRandomClientsData(currentClientSize, currentCacheSize, diskSize);
			allocateRandomTraceAndCachePerClient(currentClientSize, currentCacheSize, diskSize);
//			allocateKSequences(currentClientSize, currentCacheSize, diskSize, k);
//			allocateRiggedKSequences(currentClientSize, currentCacheSize, diskSize, 10);
//			allocateRiggedKSequences2(currentClientSize, currentCacheSize, diskSize, 10);
//			allocateRiggedIBFData(currentClientSize, currentCacheSize, diskSize);
			
			for (int i = 0; i < currentClientSize; i++) {
				clients[i].setBloomFilterType(bloomFilterType);
				clients[i].updateAllCacheToProxy();
			}
//			proxy.printBloomFilters();
			
			runExperiment();
			System.out.println("Client size: " + currentClientSize + " Hit count: " + proxy.getHitCount() + " Miss count: " + proxy.getMissCount() + " Disk access count: " + proxy.getDiskAccessCount() + " Average unfilled cells: " + proxy.getAvgZeroCells());

			switch (experimentVariable) {
			
			case NumberOfClients:
				if (currentClientSize >= maxClientCount) {
					doneExperiment = true;
				} else {
					currentClientSize += clientCountIncrement;
				}
				break;

			case CacheSize:
				if (currentCacheSize >= maxCacheSize) {
					doneExperiment = true;
				} else {
					currentCacheSize += cacheSizeIncrement;
				}
				break;

			default:
				break;
			}

		}

	}

	/**
	 * 
	 * @param clientSize
	 * @param cacheSize
	 * @param algorithmType
	 */
	private void initialize(int clientSize, int cacheSize, Algorithm algorithmType) {

		server = Server.getInstance();
		barrier = new CyclicBarrier(clientSize);
		
		proxy = ProxyFactory.buildProxy(bloomFilterType);
		proxy.setNumberOfClients(clientSize);
		
		switch (algorithmType) {
		case RobinHood:
			clients = new ClientRH[clientSize];
			for (int i = 0; i < clientSize; i++) {
				ClientRH client = new ClientRH(i + 1, cacheSize, barrier);
				clients[i] = client;
				clients[i].setProxy(proxy);
			}
			break;
		case GreedyForwarding:
			clients = new ClientGF[clientSize];
			for (int i = 0; i < clientSize; i++) {
				ClientGF client = new ClientGF(i + 1, cacheSize, barrier);
				clients[i] = client;
				clients[i].setProxy(proxy);
			}
			break;
		case NChance:
			clients = new ClientNChance[clientSize];
			for (int i = 0; i < clientSize; i++) {
				ClientNChance client = new ClientNChance(i + 1, cacheSize, barrier);
				clients[i] = client;
				clients[i].setProxy(proxy);
			}
			break;
		case CustomAlgo:
			clients = new ClientMyAlgo[clientSize];
			for (int i = 0; i < clientSize; i++) {
				ClientMyAlgo client = new ClientMyAlgo(i + 1, cacheSize, barrier);
				clients[i] = client;
				clients[i].setProxy(proxy);
			}
			break;
		case KSequence:
			clients = new ClientKSequence[clientSize];
			for (int i = 0; i < clientSize; i++) {
				ClientKSequence client = new ClientKSequence(i + 1, cacheSize, barrier, k);
				clients[i] = client;
				clients[i].setProxy(proxy);
			}
			break;
		default:
			// Throw exception
			System.exit(0);
		}
		
		proxy.setHitCount(0);
		proxy.setMissCount(0);
		proxy.setDiskAccessCount(0);
		proxy.setClients(clients);
		proxy.setSingletMap();
	}

	/**
	 * Loads trace data from file to a list
	 */
	private void loadTraceData() {
		Scanner scanner;
		dataSet = new ArrayList<Integer>();
		try {
			scanner = new Scanner(new File(traceFile));
			while (scanner.hasNextInt()) {
				dataSet.add(scanner.nextInt());
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Allocates data and requests to clients
	 */
	private void allocateData(int numberOfClients, int cacheSize, ArrayList<Integer> dataSet) {
		int fromIndex = 0;

		for (int i = 0; i < numberOfClients; i++) {
			Collections.shuffle(dataSet);
			clients[i].addData(dataSet.subList(fromIndex, fromIndex + cacheSize));
//			clients[i].setRequests(dataSet.subList(fromIndex + cacheSize, dataSet.size()));
			clients[i].setRequests(dataSet);
			clients[i].setClients(clients);
		}
	}
	
	private void runExperiment() {
		long startTime = System.currentTimeMillis();
		Thread[] clientThreads = new Thread[clients.length];
		for (int i = 0; i < clients.length; i++) {
//			System.out.println("Starting client with id " + clients[i].getId());
			clientThreads[i] = new Thread(clients[i]);
			clientThreads[i].start();
			
		}
		
		try {
			
//			latch.await(); // Waits for all the clients to be up and running to begin the experiment
//			SimLogger.getInstance().myLogger.log(Level.INFO, "All clients are up & running");
			for (int i = 0; i < clients.length; i++) {
				clientThreads[i].join();
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		long endTime = System.currentTimeMillis();
		
		System.out.println("Execution time: " + (endTime - startTime));
	}
	
	/**
	 * This is a method to allocate non overlapping data to all the clients such
	 * that all the data is allocated completely among clients.
	 * 
	 * @param numberOfClients
	 * @param cacheSize
	 * @param dataSet
	 */
	private void allocateAllDataInAllClientsInOrder(int numberOfClients, int cacheSize, ArrayList<Integer> dataSet) {
		int fromIndex = 0;

		for (int i = 0; (i < numberOfClients) && (fromIndex <= dataSet.size()); i++) {
			clients[i].addData(dataSet.subList(fromIndex, fromIndex + cacheSize));
			clients[i].setRequests(dataSet);
//			clients[i].setRequests(dataSet.subList(fromIndex + cacheSize, dataSet.size()));
			clients[i].setClients(clients);
			fromIndex += cacheSize;
		}
	}
	
	/**
	 * This is a method to generate and allocate random data to clients
	 * 
	 * @param numberOfClients
	 * @param cacheSize
	 */
	private void allocateRandomData(int numberOfClients, int cacheSize, int diskSize) {
		System.out.println("Allocating random data");
		dataSet = new ArrayList<Integer>();
		TreeMap<Integer, Integer> map = new TreeMap<Integer, Integer>();
		Random rand = new Random();
		
//		Fills cache in clients first
		for (int i = 0; i < numberOfClients; i++) {
			HashSet<Integer> set = new HashSet<Integer>();
			
			while (set.size() < cacheSize) {
				int data = rand.nextInt(diskSize);
				if (set.add(data)) {
					dataSet.add(data);
					if (map.containsKey(data)) {
						map.put(data, map.get(data) + 1);
					} else {
						map.put(data, 1);
					}
				}
			}
			clients[i].addData(new ArrayList<Integer>(set));
		}
		
//		Generate a list of random trace data
//		int numberOfRequests = (int)((float)numberOfClients * (float) cacheSize * requestSizeFactor);
//		while (dataSet.size() < numberOfRequests) {
//			dataSet.add(rand.nextInt(diskSize));
//		}
		
//		Fills requests in clients
		for (int i = 0; i < numberOfClients; i++) {
			clients[i].setRequests(dataSet);
			clients[i].setClients(clients);
		}
		
//		For analyzing data repetitions
		Set<Integer> keys = map.keySet();
		Iterator<Integer> iter = keys.iterator();
		while(iter.hasNext()) {
			int data = iter.next();
//			System.out.println("Data: " + data + " Count: " + map.get(data));
		}
		
		if (numberOfClients > 0 && clients[0] instanceof ClientMyAlgo) {
			Set<Integer> set = new HashSet<Integer>(dataSet);
			for (int data : dataSet) {
				set.add(data);
			}
			int threshold = Math.round((float)dataSet.size()/set.size());
			
			for (int i = 0; i < numberOfClients; i++) {
				((ClientMyAlgo)clients[i]).setUpperBound(threshold);
			}
		}
	}
	
	/**
	 * This method tests the simulation framework in such a way that it allocates cache of the 1st client
	 * to the trace data. None of the other clients will have the trace data in their caches.
	 * Expected output in this scenario is hitcount = numberOClient * cacheSize, diskAccess = 0  
	 * 
	 * @param numberOfClients
	 * @param cacheSize
	 * @param diskSize
	 */
	private void allocateTraceDataInOneClient(int numberOfClients, int cacheSize, int diskSize) {
		dataSet = new ArrayList<Integer>();
		int counter = 1;
		
//		Fills cache in clients first
		for (int i = 0; i < numberOfClients; i++) {
			ArrayList<Integer> list = new ArrayList<Integer>();
			
			while (list.size() < cacheSize) {
				list.add(counter);
				if (i == 0) {
					dataSet.add(counter);
				}
				counter++;
			}
			clients[i].addData(list);
		}
		
//		Fills requests in clients
		for (int i = 0; i < numberOfClients; i++) {
			clients[i].setRequests(dataSet);
			clients[i].setClients(clients);
			if (numberOfClients > 0 && clients[0] instanceof ClientMyAlgo) {
				((ClientMyAlgo)clients[i]).setUpperBound(3);
			}
		}
	}
	
	/**
	 * This method tests the simulation framework in such a way that it allocates cache of all the clients
	 * to the trace data. No clients will have any cache in common. For any client the data not present in itself has to come
	 * from other clients. It is used to test if the data is being received from appropriate clients.
	 * Expected output in this scenario is hitcount = numberOClient * trace data size, diskAccess = 0  
	 * 
	 * @param numberOfClients
	 * @param cacheSize
	 * @param diskSize
	 */
	private void allocateTraceDataInAllClients(int numberOfClients, int cacheSize, int diskSize) {
		dataSet = new ArrayList<Integer>();
		int counter = 1;
		
//		Fills cache in clients first
		for (int i = 0; i < numberOfClients; i++) {
			ArrayList<Integer> list = new ArrayList<Integer>();
			
			while (list.size() < cacheSize) {
				list.add(counter);
				dataSet.add(counter);
				counter++;
			}
			clients[i].addData(list);
		}
		
//		Fills requests in clients
		for (int i = 0; i < numberOfClients; i++) {
			clients[i].setRequests(dataSet);
			clients[i].setClients(clients);
			if (numberOfClients > 0 && clients[0] instanceof ClientMyAlgo) {
				((ClientMyAlgo)clients[i]).setUpperBound(3);
			}
		}
	}
	
	/**
	 * This method tests the simulation framework in such a way that it allocates different caches to all
	 * the clients. The trace data is also generated different from any of the client cache.
	 * The expected output in this scenario is hit count = 0, disk access = trace data size * client size 
	 * @param numberOfClients
	 * @param cacheSize
	 * @param diskSize
	 */
	private void allocateUniqueTraceDataUniqueClientsData(int numberOfClients, int cacheSize, int diskSize) {
		dataSet = new ArrayList<Integer>();
		int counter = 1;
		
//		Fills trace data of size cacheSize
		while (dataSet.size() < cacheSize) {
			dataSet.add(counter);
			counter++;
		}
		
//		Fills cache in clients
		for (int i = 0; i < numberOfClients; i++) {
			ArrayList<Integer> list = new ArrayList<Integer>();
			
			while (list.size() < cacheSize) {
				list.add(counter);
				counter++;
			}
			clients[i].addData(list);
		}
		
//		Fills requests in clients
		for (int i = 0; i < numberOfClients; i++) {
			clients[i].setRequests(dataSet);
			clients[i].setClients(clients);
			if (numberOfClients > 0 && clients[0] instanceof ClientMyAlgo) {
				((ClientMyAlgo)clients[i]).setUpperBound(3);
			}
		}
	}
	
	/**
	 * This method tests the simulation framework in such a way that it allocates random caches to all
	 * the clients and random trace data.
	 * The expected output in this scenario is random 
	 * @param numberOfClients
	 * @param cacheSize
	 * @param diskSize
	 */
	private void allocateRandomTraceDataRandomClientsData(int numberOfClients, int cacheSize, int diskSize) {
		dataSet = new ArrayList<Integer>();
		Random rand = new Random();
		
//		Fills trace data of size cacheSize
		while (dataSet.size() < cacheSize) {
			int data = rand.nextInt(diskSize);
			
			if (!dataSet.contains(data)) {
				dataSet.add(data);
			}
		}
		
//		Fills cache in clients
		for (int i = 0; i < numberOfClients; i++) {
			ArrayList<Integer> list = new ArrayList<Integer>();
			
			while (list.size() < cacheSize) {
				int data = rand.nextInt(diskSize);
				if (!list.contains(data)) {
					list.add(data);
				}
			}
			clients[i].addData(list);
		}
		
//		Fills requests in clients
		for (int i = 0; i < numberOfClients; i++) {
			clients[i].setRequests(dataSet);
			clients[i].setClients(clients);
		}
	}

	private void allocateRandomTraceAndCachePerClient(int numberOfClients, int cacheSize, int diskSize) {
		Random rand = new Random();
		
		for (int i = 0; i < numberOfClients; i++) {
			ArrayList<Integer> cacheData = new ArrayList<Integer>();
			ArrayList<Integer> traceData = new ArrayList<Integer>();
			
//			System.out.print("ClientID: " + clients[i].getId() + "||");
			while (cacheData.size() < cacheSize) {
				int data = rand.nextInt(diskSize);
				if (!cacheData.contains(data)) {
					cacheData.add(data);
//					System.out.print(" " + data + " ");
				}
			}
			
			while (traceData.size() < cacheSize) {
				int data = rand.nextInt(diskSize);
				if (!traceData.contains(data)) {
					traceData.add(data);
				}
			}
//			System.out.println("");
			clients[i].setRequests(traceData);
			clients[i].addData(cacheData);
			clients[i].setClients(clients);
			
			if (numberOfClients > 0 && clients[0] instanceof ClientMyAlgo) {
				((ClientMyAlgo)clients[i]).setUpperBound(3);
			}
		}
	}
	
	private void allocateKSequences(int numberOfClients, int cacheSize, int diskSize, int k) {
		Random rand = new Random();
		
		for (int i = 0; i < numberOfClients; i++) {
			ArrayList<Integer> cacheData = new ArrayList<Integer>();
			ArrayList<Integer> traceData = new ArrayList<Integer>();
			
			System.out.print("ClientID: " + clients[i].getId() + "||");
			while (cacheData.size() < cacheSize) {
//				int data = rand.nextInt(diskSize);
//				if (!cacheData.contains(data)) {
//					cacheData.add(data);
//					System.out.print(" " + data + " ");
//				}
				
				int data = (rand.nextInt(diskSize) / k) * k + 1;
				int counter = 1;
				while(counter <= k && cacheData.size() < cacheSize) {
					
					if (!cacheData.contains(data)) {
						cacheData.add(data);
						System.out.print(" " + data + " ");
					} else {
						data = (rand.nextInt(diskSize) / 10) * 10 + 1;
						counter = 1;
						continue;
					}
					
//					cacheData.add(data);
					data++;
					counter++;
				}
			}
			
			while(traceData.size() < cacheSize) {
				int randomNumber = (rand.nextInt(diskSize) / 10) * 10 + 1;
				int counter = 1;
				
				while(counter <= k && traceData.size() < cacheSize) {
					traceData.add(randomNumber);
					randomNumber++;
					counter++;
				}
			}
			
			clients[i].setRequests(traceData);
			clients[i].addData(cacheData);
			clients[i].setClients(clients);
		}
	}
	
	/**
	 * This is a very special case of KSequence which has only 2 clients
	 * @param numberOfClients
	 * @param cacheSize
	 * @param diskSize
	 * @param k
	 */
	private void allocateRiggedKSequences(int numberOfClients, int cacheSize, int diskSize, int k) {
//		for (int i = 0; i < numberOfClients; i++) {
			ArrayList<Integer> cacheData = new ArrayList<Integer>();
			ArrayList<Integer> traceData = new ArrayList<Integer>();
			
			int counter = 1;
			while (counter <= 10) {
				cacheData.add(counter);
				counter++;
			}
			clients[0].addData(cacheData);
			clients[1].addData(cacheData);
			cacheData.clear();
			
			while (counter <= 20) {
				cacheData.add(counter);
				traceData.add(counter);
				counter++;
			}
			clients[0].addData(cacheData);
			clients[1].setRequests(traceData);
			cacheData.clear();
			traceData.clear();
			
			while (counter <= 30) {
				cacheData.add(counter);
				traceData.add(counter);
				counter++;
			}
			clients[0].setRequests(traceData);
			clients[1].addData(cacheData);
			clients[0].setClients(clients);
			clients[1].setClients(clients);
		}
//	}
	
	/**
	 * This is another special case of KSequence
	 * @param numberOfClients
	 * @param cacheSize
	 * @param diskSize
	 * @param k
	 */
	private void allocateRiggedKSequences2(int numberOfClients, int cacheSize, int diskSize, int k) {
		ArrayList<Integer> cacheData = new ArrayList<Integer>();
		ArrayList<Integer> traceData = new ArrayList<Integer>();
		
		int counter = 1;
		while (counter <= 10) {
			cacheData.add(counter);
			counter++;
		}
		clients[0].addData(cacheData);
		clients[0].setRequests(cacheData);
		clients[1].setRequests(cacheData);
		cacheData.clear();
		
		while (counter <= 20) {
			cacheData.add(counter);
			counter++;
		}
		clients[0].addData(cacheData);
		cacheData.clear();
		
		while (counter <= 30) {
			cacheData.add(counter);
			counter++;
		}
		clients[1].addData(cacheData);
		cacheData.clear();
		
		while (counter <= 40) {
			cacheData.add(counter);
			counter++;
		}
		clients[1].addData(cacheData);
		cacheData.clear();
		
		clients[0].setClients(clients);
		clients[1].setClients(clients);
	}
	
	private void allocateRiggedIBFData(int numberOfClients, int cacheSize, int diskSize) {
		Integer[] data1 = {52, 107, 93, 220, 6, 423, 643, 201, 401, 715};
		Integer[] data2 = {726, 562, 259, 376, 527, 729, 322, 436, 669, 215};
		Integer[] data3 = {802, 421, 615, 912, 233, 851, 268, 817, 638, 320};
		
		Integer[] trace1 = {26, 17, 3, 20, 16, 23, 63, 21, 41, 71};
		Integer[] trace2 = {26, 62, 59, 76, 27, 29, 22, 36, 69, 15};
		Integer[] trace3 = {80, 42, 61, 12, 33, 51, 68, 17, 38, 20};
		
		ArrayList<Integer> list1 = new ArrayList<Integer>(Arrays.asList(data1));
		ArrayList<Integer> list2 = new ArrayList<Integer>(Arrays.asList(data2));
		ArrayList<Integer> list3 = new ArrayList<Integer>(Arrays.asList(data3));
		
		ArrayList<Integer> req1 = new ArrayList<Integer>(Arrays.asList(trace1));
		ArrayList<Integer> req2 = new ArrayList<Integer>(Arrays.asList(trace2));
		ArrayList<Integer> req3 = new ArrayList<Integer>(Arrays.asList(trace3));
		
		clients[0].addData(list1); clients[0].setRequests(req1);
		clients[1].addData(list2); clients[1].setRequests(req2);
		clients[2].addData(list3); clients[2].setRequests(req3);
		clients[0].setClients(clients);
		clients[1].setClients(clients);
		clients[2].setClients(clients);
	}
}
