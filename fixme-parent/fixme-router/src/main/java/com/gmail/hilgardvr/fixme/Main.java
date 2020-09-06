package com.gmail.hilgardvr.fixme;

import com.gmail.hilgardvr.fixme.controller.*;

public class Main {
	
	public static void main(String[] args) throws Exception {
		//new AsyncBrokerServer();
		new Router();
		//new AsyncMarketServer();
		try {
			while (true)
				Thread.sleep(600000);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.print("Server exiting");
		return;
	}
}
