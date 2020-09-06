package com.gmail.hilgardvr.fixme.model;

import java.util.PriorityQueue;

public class StockInfo {
    PriorityQueue<VolumePrice> bids = new PriorityQueue<VolumePrice>();
    PriorityQueue<VolumePrice> offers = new PriorityQueue<VolumePrice>();

    class VolumePrice {
        Integer volume;
        Double price;
    }
}