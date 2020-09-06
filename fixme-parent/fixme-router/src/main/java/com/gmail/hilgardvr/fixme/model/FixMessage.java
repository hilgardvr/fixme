package com.gmail.hilgardvr.fixme.model;

public class FixMessage {
    public int marketId;
    public String orderType;
    public String instrument;
    public int quantity;
    public int brokerId;
    public double price;
    public String executionResult;
    public int checkSum;

    //broker constructor
    public FixMessage(int brokerId, String orderType, String instrument, int quantity, int marketId, double price) {
        this.brokerId = brokerId;
        this.orderType = orderType;
        this.instrument = instrument;
        this.quantity = quantity;
        this.marketId = marketId;
        this.price = price;
        this.executionResult = "PENDING_RESULT";
    }

    //market constructor
    public FixMessage(int marketId, String orderType, String instrument, int quantity, int brokerId, double price, Boolean result) {
        this.marketId = marketId;
        this.orderType = orderType;
        this.instrument = instrument;
        this.quantity = quantity;
        this.brokerId = brokerId;
        this.price = price;
        setExcutionResult(result);
    }

    public String toString() {
        String str =  marketId + "|" + orderType + "|" + instrument + "|" + quantity + "|" + brokerId + "|" + price + "|" + executionResult + "|" + checkSum;
        this.checkSum = calcCheckSum(str);
        return (str + this.checkSum);
    }

    private int calcCheckSum(String str) {
        byte[] bytes = str.getBytes();
        int total = 0;
        for (byte b: bytes) {
            total += b;
        }
        total %= 256;
        return total;
    }

    public static FixMessage toFix(String str) {
        //System.out.println("Inside fix: " + str);
        String[] fixParts = str.split("\\|");
        /* for (String part: fixParts) {
            System.out.println(part);
        } */
        if (fixParts.length != 8) {
            System.out.println(str + " - invalid FIX format");
            return null;
        }
        try {
            FixMessage newFix = new FixMessage(Integer.parseInt(fixParts[0]), fixParts[1], fixParts[2], Integer.parseInt(fixParts[3]), Integer.parseInt(fixParts[4]), Double.parseDouble(fixParts[5]));
            return newFix;
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setExcutionResult(Boolean r) {
        this.executionResult = r ? "Executed" : "Rejected";
    }

    public Double getPrice() {
        return this.price;
    }
    
    public int getQuantity() {
        return this.quantity;
    }
    
    public String getInstrument() {
        return this.instrument;
    }
}