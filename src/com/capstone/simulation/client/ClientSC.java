package com.capstone.simulation.client;

import java.util.Iterator;

public class ClientSC extends Client{
	
	public ClientSC(int id, int cacheSize) {
		super(id, cacheSize);
	}

	@Override
	public void run() {
		Iterator<Integer> iterator = getRequests().iterator();
		while((getDueResponses().size() > 0) || iterator.hasNext()) {
			
			if (iterator.hasNext()) {
				sendDataRequest(iterator.next());
			}
			if (getDueResponses().size() > 0) {
				sendDataToClient();
			}
		}
	}

}
