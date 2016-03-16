package com.capstone.simulation.server;

/**
 * Singleton implementation of server that has all the data.
 * @author Pavan Kumar
 * 
 */
public class Server {

	private static final Server server = new Server();
	
	private Server() {
		
	}
	
	public static Server getInstance() {
		return server;
	}
	
	public int getData(int data) {
		return data;
	}
}
