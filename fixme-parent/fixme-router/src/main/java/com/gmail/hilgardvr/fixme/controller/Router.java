package com.gmail.hilgardvr.fixme.controller;

import com.gmail.hilgardvr.fixme.model.FixMessage;
import com.gmail.hilgardvr.fixme.model.Attachment;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Router {
	private Map<Integer, Attachment> brokers;
	private Map<Integer, Attachment> markets;
	private Map<Integer, Set<String>> marketShares;
	private AsynchronousServerSocketChannel brokerServer;
	private AsynchronousServerSocketChannel marketServer;
	private int brokerId;
	private int marketId;
	private static final int BUFFSIZE = 2048;

	public Router() {
		brokerId = 100000;
		marketId = 200000;
		brokers = new HashMap<Integer, Attachment>();
		markets = new HashMap<Integer, Attachment>();
		marketShares = new HashMap<Integer, Set<String>>();

		try {
			//start broker server on port 5000
			brokerServer = AsynchronousServerSocketChannel.open();
			InetSocketAddress brokerAddress = new InetSocketAddress("localhost", 5000);
			brokerServer.bind(brokerAddress);
			System.out.println("Router accepting broker connections on: " + brokerAddress);
			runBrokerServer();

			//start market server on port 5001
			marketServer = AsynchronousServerSocketChannel.open();
			InetSocketAddress marketAddress = new InetSocketAddress("localhost", 5001);
			marketServer.bind(marketAddress);
			System.out.println("Router accepting market connections on: " + marketAddress);
			runMarketServer();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//broker section

	private void runBrokerServer() {
		brokerServer.accept(null, new BrokerConnectionHandler());
	}
	
	private class BrokerConnectionHandler implements CompletionHandler<AsynchronousSocketChannel, Attachment> {
		
		@Override
		public void completed(AsynchronousSocketChannel ch, Attachment attach) {
			try {
				brokerServer.accept(attach, this);
				System.out.println("Router accepted the broker connection: " + ch.getRemoteAddress());
				registerBroker(ch);
				System.out.println("List of registered brokers:");
				for (Map.Entry<Integer, Attachment> e: brokers.entrySet()) {
					System.out.println("Broker id: " + e.getValue().clientId + " Broker address: " + e.getValue().clientAddress);
				}
			} catch (IOException e) {
				System.out.println("Problem on completing broker connection:");
				e.printStackTrace();
			}
		}

		@Override
		public void failed(Throwable t, Attachment attach) {
			System.out.println(attach.clientId + " failed");
		}

		private void registerBroker(AsynchronousSocketChannel client) {
			Attachment attach = new Attachment();
			attach.buffer = ByteBuffer.allocate(BUFFSIZE);
			attach.client = client;
			attach.clientId = brokerId;
			try {
				attach.clientAddress = client.getRemoteAddress();
			} catch (IOException igException) {}
			attach.client.write(ByteBuffer.wrap( ("Router accepted the broker connection. Your id: " + brokerId).getBytes()));
			brokers.put(brokerId, attach);
			brokerId++;
			attach.client.read(attach.buffer, attach, new BrokerMessageHandler());
		}
	}

	class BrokerMessageHandler implements CompletionHandler<Integer, Attachment> {

		@Override
		public void completed(Integer result, Attachment attach) {
			if (result != -1) {
				attach.buffer.flip();
				int limit = attach.buffer.limit();
				byte[] lineBytes = new byte[limit];
				attach.buffer.get(lineBytes, 0, limit);
				String line = new String(lineBytes);
				String temp = "";
				if (line.length() > 100) {
					temp = line.substring(0,100);
				} else {
					temp = line;
				}
				System.out.println("Received from brokerId: " + attach.clientId + " + line: " + temp);
				if (checkCheckSum(line)) {
					executeBrokerMessage(attach, line);
				} else {
					attach.client.write(ByteBuffer.wrap("CheckSum failed".getBytes()));
				}
				attach.buffer.clear();
				attach.client.read(attach.buffer, attach, new BrokerMessageHandler());
			}
		}

		@Override
		public void failed(Throwable t, Attachment attach) {
			System.out.println(attach.clientId + " failed");
			attach.client.read(attach.buffer, attach, new BrokerMessageHandler());
		}
	}

	private void executeBrokerMessage(Attachment attach, String line) {

		//see if a market has the requested share listed
		FixMessage fix = FixMessage.toFix(line);
		String shareToTrade;
		if (line != null) {
			shareToTrade = fix.instrument;
			for (Map.Entry<Integer, Set<String>> m : marketShares.entrySet()) {
				if (m.getValue().contains(shareToTrade)) {
					int key = m.getKey();
					for (Attachment market: markets.values()) {
						if (key == market.clientId) {
							System.out.println("Market: " + market.clientId);
							market.mapToId = attach.clientId;
							market.client.write(ByteBuffer.wrap(line.getBytes()));
							return;
						}
					}
				}
			}
			attach.client.write(ByteBuffer.wrap("Security not available".getBytes()));	
		} else {
			attach.client.write(ByteBuffer.wrap("Security not available".getBytes()));
		}
	}


	// Market section
	private void runMarketServer() {
		marketServer.accept(null, new MarketConnectionHandler());
	}
	
	private class MarketConnectionHandler implements CompletionHandler<AsynchronousSocketChannel, Attachment> {
		
		@Override
		public void completed(AsynchronousSocketChannel ch, Attachment attach) {
			try {
				marketServer.accept(attach, this);
				System.out.println("Router accepted the market connection: " + ch.getRemoteAddress());
				registerMarket(ch);
				System.out.println("List of markets:");
				for (Map.Entry<Integer, Attachment> e: markets.entrySet()) {
					System.out.println("Market id: " + e.getValue().clientId + " Market address: " + e.getValue().clientAddress);
				}
			} catch (IOException e) {
				System.out.println("Problem on completing market connection:");
				e.printStackTrace();
			}
		}

		@Override
		public void failed(Throwable t, Attachment attach) {
			System.out.println(attach.clientId + " failed");
		}

		private void registerMarket(AsynchronousSocketChannel client) {
			Attachment attach = new Attachment();
			attach.buffer = ByteBuffer.allocate(BUFFSIZE);
			attach.client = client;
			attach.clientId = marketId;
			try {
				attach.clientAddress = client.getRemoteAddress();
			} catch (IOException igException) {}
			attach.client.write(ByteBuffer.wrap( ("Router accepted the market connection. Your id: " + marketId).getBytes()));
			markets.put(marketId, attach);
			
			//get shares available in specific market
			Future<Integer> fi = attach.client.read(attach.buffer);
			try {
				fi.get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
			attach.buffer.flip();
			int limit = attach.buffer.limit();
			byte[] lineBytes = new byte[limit];
			attach.buffer.get(lineBytes, 0, limit);
			String marketData = new String(lineBytes);
			System.out.println("marketData: " + marketData);
			String[] marketShares = (marketData.split("\\|")[1]).split(",");
			Set<String> shares = new HashSet<String>();
			for (String share: marketShares) {
				shares.add(share);
			}
			Router.this.marketShares.put(marketId, shares);
			marketId++;
			attach.buffer.clear();
			attach.client.read(attach.buffer, attach, new MarketMessageHandler());
		}
	}

	class MarketMessageHandler implements CompletionHandler<Integer, Attachment> {

		@Override
		public void completed(Integer result, Attachment attach) {
			if (result != -1 && result != 0) {
				attach.buffer.flip();
				int limit = attach.buffer.limit();
				byte[] lineBytes = new byte[limit];
				attach.buffer.get(lineBytes, 0, limit);
				String line = new String(lineBytes);
				String temp = "";
				if (line.length() > 50) {
					temp = line.substring(0,50);
				} else {
					temp = line;
				}
				System.out.println("Received from marketId: " + attach.clientId + " + to: " + attach.mapToId + " result: " + temp);
				executeMarketMessage(attach, line);
				attach.buffer.clear();
				/* Future<Integer> fi = attach.client.read(attach.buffer);
				try {
					fi.get(5, TimeUnit.SECONDS);

				} catch (InterruptedException | ExecutionException | TimeoutException e) {
					e.printStackTrace();

				} */
				attach.client.read(attach.buffer, attach, new MarketMessageHandler());
			} else {
				System.out.println("Market: " + attach.clientId + " at address " + attach.clientAddress 
				+ " bytes read: " + result + "- Removing market data from router");
				markets.remove(attach.clientId);
				marketShares.remove(attach.clientId);
				
				
				/* //sending response to broker
				System.out.println("Sending msg to: " + brokers.get(attach.mapToId).clientId);
				Attachment brokerToReceive = brokers.get(attach.mapToId);
				brokerToReceive.client.write(ByteBuffer.wrap("Market no longer connected - try again later".getBytes())); */
			}
		}

		@Override
		public void failed(Throwable t, Attachment attach) {
			System.out.println("Message from " + attach.clientId + " to " + attach.mapToId + " failed");
		}

	}

	private void executeMarketMessage(Attachment attach, String line) {
		System.out.println("Sending msg to: " + brokers.get(attach.mapToId).clientId);
		Attachment brokerToReceive = brokers.get(attach.mapToId);
		brokerToReceive.client.write(ByteBuffer.wrap(line.getBytes()));
	}

	private  boolean checkCheckSum(String str) {
		String[] fixParts = str.split("\\|");
        if (fixParts.length != 8) {
			return false;
        } else {
			int given = Integer.parseInt(fixParts[7]);
			String newStr = fixParts[0] + "|" + fixParts[1] + "|" + fixParts[2] + "|" + fixParts[3] + "|" + fixParts[4] + "|" + fixParts[5] + "|" + fixParts[6];
			byte[] bytes = newStr.getBytes();
			int total = 0;
			for (byte b: bytes) {
				total += b;
			}
			total %= 256;
			if (total == given) {
				return true;
			} else {
				return false;
			}
		}
	}
}