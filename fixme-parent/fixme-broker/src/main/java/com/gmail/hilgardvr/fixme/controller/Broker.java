package com.gmail.hilgardvr.fixme.controller;

import com.gmail.hilgardvr.fixme.model.Database;
import com.gmail.hilgardvr.fixme.model.FixMessage;
import com.gmail.hilgardvr.fixme.model.UserAccount;
import com.gmail.hilgardvr.fixme.view.BrokerGui;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Broker {
    private static final int BUFFSIZE = 255;
    private int id;
    private AsynchronousSocketChannel channel;
    private BrokerGui bg;
    private UserAccount ua;
    private ByteBuffer bb;
    private Database db;
    
    class BuyListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            bg.disableButtons();
            String buyPrice = bg.getBuyPrice();
            String volumeStr = bg.getBuyVolume();
            String share = bg.getBuyShareCode();
            Double price;
            int volume;
            if (buyPrice == null || volumeStr == null || share == null) {
                bg.disableButtons();
                bg.displayHoldings(ua.getHoldings());
                bg.setOutputTextField("Invalid order");
                return;
            }
            try {
                price = Double.parseDouble(buyPrice);
                volume = Integer.parseInt(volumeStr);
                FixMessage fix = new FixMessage(id, "BUY", share, volume, 1, price);
                Double cost = fix.getPrice() * fix.getQuantity();
                if (cost > ua.getCashBalance()) {
                    bg.setOutputTextField("Insufficient funds");
                    bg.enableButtons();
                    bg.displayHoldings(ua.getHoldings());
                    return;
                }
                channel.write(ByteBuffer.wrap(fix.toString().getBytes()));
                bb.clear();
                int bytesRead;
                Future<Integer> fi = channel.read(bb);
                try {
                    bytesRead = fi.get(5, TimeUnit.SECONDS);
                } catch (TimeoutException ex) {
                    ex.printStackTrace();
                    bg.setOutputTextField("Router timed out");
                    bg.enableButtons();
                    bg.displayHoldings(ua.getHoldings());
                    return;
                }
                String line = "";
                bg.enableButtons();
                if (bytesRead != -1 && bytesRead > 0) {
                    bb.flip();
                    byte[] lineBytes = new byte[bytesRead];
                    bb.get(lineBytes, 0, bytesRead);
                    line = new String(lineBytes);
                    
                    //parse result
                    if (line != null) {
                        String[] fixParts = line.split("\\|");
                        if (fixParts.length != 8) {
                            bg.setOutputTextField(line + " invalid");
                            bg.displayHoldings(ua.getHoldings());
                            return;
                        } else {
                            fix = new FixMessage(Integer.parseInt(fixParts[0]), fixParts[1], fixParts[2],  Integer.parseInt(fixParts[3]), Integer.parseInt(fixParts[4]), Double.parseDouble(fixParts[5]), true);
                            Map<String, Integer> temp = ua.getHoldings();
                            bg.setOutputTextField(fixParts[6] + " - bought " + fixParts[3] + " shares of " + fixParts[2] + " at " + fixParts[5]);
                            if (fixParts[6].equals("Executed")) {
                                ua.setCashBalance(ua.getCashBalance() - (Double.parseDouble(fixParts[5]) * Integer.parseInt(fixParts[3])));
                                int quant = fix.getQuantity();
                                    if (ua.getHoldings().containsKey(fix.getInstrument())) {
                                    quant += ua.getHoldings().get(fix.getInstrument());
                                }
                                temp.put(fix.getInstrument(), quant);
                                ua.setHoldings(temp);
                                bg.displayHoldings(temp);
                                bg.displayCashBalance(ua.getCashBalance());
                                refreshHoldingsValue();
                                db.createDocument(fix);
                            } else {
                                bg.displayHoldings(temp);
                                bg.setOutputTextField(fixParts[6] + " - Quoted price for " + fixParts[2] + ": " + fixParts[5]);
                            }
                        }
                    }
                }
                } catch (ExecutionException | NumberFormatException | InterruptedException ex) {
                    bg.setOutputTextField("An error occurred - please try again");
                    bg.enableButtons();
                    bg.displayHoldings(ua.getHoldings());
                    //ex.printStackTrace();
                    System.out.println(ex.getClass().getCanonicalName());
                }
        }
    }

    class SellListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            bg.disableButtons();
            String share = bg.getSellShare();
            String volStr = bg.getSellVolume();
            String priceStr = bg.getSellPrice();
            int volume;
            double price;
            int volHeld;

            if (share == null || volStr == null || priceStr == null
                    || share == "" || volStr == "" || priceStr == "") {
                bg.displayHoldings(ua.getHoldings());
                bg.enableButtons();
                return;
            }
            try {
                String[] parts = share.split(" ");
                share = parts[0];
                volHeld = Integer.parseInt(parts[parts.length - 1]);
                volume = Integer.parseInt(volStr);
                price = Double.parseDouble(priceStr);
                if (volHeld < volume) {
                    bg.displayHoldings(ua.getHoldings());
                    bg.enableButtons();
                    bg.setOutputTextField("Invalid volume");
                    return;
                }
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
                bg.displayHoldings(ua.getHoldings());
                bg.enableButtons();
                return;
            }
            FixMessage fix = new FixMessage(id, "SELL", share, volume, 1, price);
            try {
                channel.write(ByteBuffer.wrap(fix.toString().getBytes()));
                bb.clear();
                int bytesRead;
                Future<Integer> fi = channel.read(bb);
                try {
                    bytesRead = fi.get(5, TimeUnit.SECONDS);
                } catch (InterruptedException | TimeoutException ex) {
                    ex.printStackTrace();
                    bg.displayHoldings(ua.getHoldings());
                    bg.enableButtons();
                    return;
                }
                String line = "";
                bg.enableButtons();
                if (bytesRead != -1 && bytesRead > 0) {
                    bb.flip();
                    byte[] lineBytes = new byte[bytesRead];
                    bb.get(lineBytes, 0, bytesRead);
                    line = new String(lineBytes);
                    
                    //parse result
                    if (line != null) {
                        String[] fixParts = line.split("\\|");
                        if (fixParts.length != 8) {
                            bg.setOutputTextField(line + " invalid");
                            bg.displayHoldings(ua.getHoldings());
                            return;
                        } else {
                            fix = new FixMessage(Integer.parseInt(fixParts[0]), fixParts[1], fixParts[2],  Integer.parseInt(fixParts[3]), Integer.parseInt(fixParts[4]), Double.parseDouble(fixParts[5]), true);
                            Map<String, Integer> temp = ua.getHoldings();
                            bg.setOutputTextField(fixParts[6] + " - sold " + fixParts[3] + " shares at " + fixParts[5]);
                            if (fixParts[6].equals("Executed")) {
                                ua.setCashBalance(ua.getCashBalance() + (Double.parseDouble(fixParts[5]) * Integer.parseInt(fixParts[3])));
                                int quant = ua.getHoldings().get(fix.getInstrument()) - volume;
                                temp.put(fix.getInstrument(), quant);
                                ua.setHoldings(temp);
                                bg.displayHoldings(temp);
                                bg.displayCashBalance(ua.getCashBalance());
                                refreshHoldingsValue();
                                db.createDocument(fix);
                            } else {
                                bg.displayHoldings(temp);
                                bg.setOutputTextField(fixParts[6] + " - Quoted price for " + fixParts[2] + ": " + fixParts[5]);
                            }
                        }
                    }
                }
                

            } catch (ExecutionException ex) {
                bg.setOutputTextField("An error occurred - please try again");
                bg.enableButtons();
                bg.displayHoldings(ua.getHoldings());
                System.out.println(ex.getClass().getCanonicalName());
            }
            bg.enableButtons();
        }
    }

    class RefreshListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            refreshHoldingsValue();
        }
    }

    public Broker () {
        ua = new UserAccount();
        bg = new BrokerGui(ua);
        bg.setVisible(true);
        bg.addBuyButtonListener(new BuyListener());
        bg.addSellButtonListener(new SellListener());
        bg.addRefreshDataListener(new RefreshListener());
        db = new Database();
        db.getDb();
        
        try {
            channel = AsynchronousSocketChannel.open();
            InetSocketAddress server = new InetSocketAddress("localhost", 5000);
            Future<Void> result = channel.connect(server);
            try {
                result.get(5, TimeUnit.SECONDS);
            } catch (TimeoutException | ExecutionException e) {
                e.printStackTrace();
                bg.setOutputTextField("Could not connect to the router");
                return;
            }
            System.out.println("Borker connected to server at " + channel.getRemoteAddress());

            try {
                bb = ByteBuffer.allocate(BUFFSIZE);
                Future<Integer> fi = channel.read(bb);
                int bytesRead = fi.get();
                String line = "";
                
                if (bytesRead != -1 && bytesRead > 0) {
                    bb.flip();
                    byte[] lineBytes = new byte[bytesRead];
                    bb.get(lineBytes, 0, bytesRead);
                    line = new String(lineBytes);
                    if (line.equals("Timed out - connection closed")) {
                        System.out.println("Unsuccessfull - server closed connection on time out - exiting");
                        channel.close();
                    } else if (line.length() > 37 && line.substring(0, 37).equals("Router accepted the broker connection")) {
                        String[] parts = line.split(" ");
                        this.id = Integer.parseInt(parts[parts.length - 1]);
                        System.out.println("ID received: " + this.id);
                    }
                } else {
                    if (bytesRead == -1)
                        System.out.println("No bytes read - channel has reached end of stream - exiting");
                    else
                        System.out.println("No bytes read - exiting");
                }   
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    void refreshHoldingsValue() {
        Map<String, Double> newValues = new HashMap<String, Double>();
        for (Map.Entry<String, Integer> entry: ua.getHoldings().entrySet()) {
            String stockCode = entry.getKey();
            FixMessage fix = new FixMessage(id, "BUY", stockCode, 0, 1, 0);
            channel.write(ByteBuffer.wrap(fix.toString().getBytes()));
            bb.clear();
            int bytesRead;
            Future<Integer> fi = channel.read(bb);
            try {
                bytesRead = fi.get();
                String line = "";
                if (bytesRead != -1 && bytesRead > 0) {
                    bb.flip();
                    byte[] lineBytes = new byte[bytesRead];
                    bb.get(lineBytes, 0, bytesRead);
                    line = new String(lineBytes);
                    
                    //parse result
                    if (line != null) {
                        String[] fixParts = line.split("\\|");
                        if (fixParts.length != 8) {
                            bg.setOutputTextField("FIX message received invalid");
                            return;
                        } else {
                            double value = Double.parseDouble(fixParts[5]);
                            newValues.put(stockCode, value);
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        ua.setHoldingsValue(newValues);
        double val = ua.calcHoldingsValue();
        bg.setHoldingsValue(val);
        bg.setTotalAccountValue(ua.getAccountValue());
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