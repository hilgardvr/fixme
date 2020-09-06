package com.gmail.hilgardvr.fixme.controller;

import com.gmail.hilgardvr.fixme.model.FixMessage;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import java.util.ArrayList;

public class MarketClient {
    private static final int BUFFSIZE = 2048 * 1000;
    private AsynchronousSocketChannel channel;
    private InetSocketAddress address;
    private Future<Void> conResult;
    private int id;
    private ArrayList<String> marketShareCodes;
    Runnable executeOrder;

    public MarketClient() {
        String hostName = "localhost";
        int port = 5001;
        marketShareCodes = new ArrayList<>();
        marketShareCodes.add("TSLA");
        marketShareCodes.add("MSFT");
        marketShareCodes.add("GOOGL");
        marketShareCodes.add("AMZN");
        /* marketShareCodes.add("FB"); */
        
        try {
            channel = AsynchronousSocketChannel.open();
            address = new InetSocketAddress(hostName, port);
            conResult = channel.connect(address);
            conResult.get();

            while (true) {
                ByteBuffer bb = ByteBuffer.allocate(BUFFSIZE);
                String str = "";
                Future<Integer> fi;
                int bytes = 0;
                try {
                    fi = channel.read(bb);
                    bytes = fi.get();
                    
                    if (bytes != -1 && bytes > 0) {
                        bb.flip();
                        byte[] line = new byte[bytes];
                        bb.get(line, 0, bytes);
                        str = new String(line);
                    } else {
                        continue;
                    }
        
                    if (str.length() > 37 && str.substring(0, 37).equals("Router accepted the market connection")) {
                        String[] parts = str.split(" ");
                        this.id = Integer.parseInt(parts[parts.length - 1]);
                        System.out.println(str);
                        String shareCodesString = String.join(",", marketShareCodes);
                        String marketInfoToRouter = id + "|" + shareCodesString;
                        System.out.println("Response to router: " + marketInfoToRouter);
                        channel.write(ByteBuffer.wrap(marketInfoToRouter.getBytes()));
                        continue;
                    }


                    //use executor framework
                    HandleReadWrite hrw = new HandleReadWrite(bytes, bb, str);
                    Executor exec = new ThreadPerTask();
                    exec.execute(hrw);

                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                    System.out.println("Connection with router was interrupted - exiting");
                    break;
                }
                
                
            }
        } catch (InterruptedException | ExecutionException | IOException e) {
            e.printStackTrace();
        }
    }

    private double getMarketData(String str) {
        double allData;
        //String iex;

        allData = -1;
        //iex = "https://api.iextrading.com/1.0/stock/market/batch?symbols=" + str + "&types=quote";

        try {
            URL url = new URL("https://api.iextrading.com/1.0/stock/market/batch?symbols=" + str + "&types=quote");
            InputStream is = url.openStream();
            JsonReader rdr = Json.createReader(is);
            JsonObject obj = rdr.readObject();
            double d = (obj.getJsonObject(str).getJsonObject("quote").getJsonNumber("latestPrice")).doubleValue();
            allData = d;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return allData;
    }

    class HandleReadWrite implements Runnable {
        int bytes;
        ByteBuffer bb;
        String str;

        HandleReadWrite(int bytes, ByteBuffer bb, String str) {
            this.bytes = bytes;
            this.bb = bb;
            this.str = str;
        }

        @Override
        public void run() {
            FixMessage fix = FixMessage.toFix(str);
            FixMessage returnFix = null;
            if (fix == null) {
                channel.write(ByteBuffer.wrap("Rejected - invalid FIX notation".getBytes()));
                bb.clear();
            } else {
                double price = getMarketData(fix.instrument);
                if (fix.orderType.equals("BUY") && fix.quantity < 10000 && fix.price >= price && price != -1) {
                    returnFix = new FixMessage(id, fix.orderType, fix.instrument, fix.quantity, fix.brokerId, price, true);
                } else if (fix.orderType.equals("SELL") && fix.quantity < 10000 && fix.price <= price && price != -1) {
                    returnFix = new FixMessage(id, fix.orderType, fix.instrument, fix.quantity, fix.brokerId, price, true);
                } else {
                    returnFix = new FixMessage(id, fix.orderType, fix.instrument, fix.quantity, fix.brokerId, price, false);
                }
                channel.write(ByteBuffer.wrap(returnFix.toString().getBytes()));
            }
        }
    }

    class ThreadPerTask implements Executor {

        @Override
        public void execute(Runnable r) {
            new Thread(r).start();
        }
    }

    @Override
    public String toString() {
        String server = "";
        try {
            server = channel.getRemoteAddress().toString();
        } catch (IOException ignException) {}
        return ("BrokerId: " + id + (server.equals("") ? " not connected" : " connected"));
    }

}