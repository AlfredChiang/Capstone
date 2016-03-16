package com.capstone.simulation.driver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;
import java.util.Random;
import java.util.Scanner;

import com.capstone.simulation.client.Client;
import com.capstone.simulation.client.ClientGF;
import com.capstone.simulation.client.ClientNChance;
import com.capstone.simulation.client.ClientSC;
import com.capstone.simulation.proxy.Proxy;
import com.capstone.simulation.server.Server;
import com.capstone.simulation.utility.Algorithm;
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

	private Algorithm algorithm;
	private ExperimentVariable experimentVariable;

	private String traceFile;
	private ArrayList<Integer> dataSet;

	private Client[] clients;
	private Server server;
	private Proxy proxy;

	public static void main(String[] args) {
		// Exit if config file is not provided.
		if (args.length == 0) {
			System.exit(0);
		}
		Simulator simulator = new Simulator();
		String configFile = args[0];

		simulator.loadProperties(configFile);
		simulator.loadTraceData();
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

			experimentVariable = ExperimentVariable.valueOf(configProperties.getProperty(Strings.experimentVariable));

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

			initialize(currentClientSize, currentCacheSize, algorithmType);
			allocateData(currentClientSize, currentCacheSize, dataSet);
			

			switch (experimentVariable) {
			case NumberOfClients:
				if (currentClientSize == maxClientCount) {
					doneExperiment = true;
				} else {
					currentClientSize += clientCountIncrement;
				}

				break;

			case CacheSize:
				if (currentCacheSize == maxCacheSize) {
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
		proxy = Proxy.getInstance();
		switch (algorithmType) {
		case SummaryCache:
			clients = new ClientSC[clientSize];
			for (int i = 0; i < clientSize; i++) {
				ClientSC client = new ClientSC(i + 1, cacheSize);
				clients[i] = client;
			}
			break;
		case GreedyForwarding:
			clients = new ClientGF[clientSize];
			for (int i = 0; i < clientSize; i++) {
				ClientGF client = new ClientGF(i + 1, cacheSize);
				clients[i] = client;
			}
			break;
		case NChance:
			clients = new ClientNChance[clientSize];
			for (int i = 0; i < clientSize; i++) {
				ClientNChance client = new ClientNChance(i + 1, cacheSize);
				clients[i] = client;
			}
			break;
		default:
			// Throw exception
			System.exit(0);
		}
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
			clients[i].setRequests(dataSet.subList(fromIndex + cacheSize, dataSet.size()));
			clients[i].setClients(clients);
		}
	}

}
