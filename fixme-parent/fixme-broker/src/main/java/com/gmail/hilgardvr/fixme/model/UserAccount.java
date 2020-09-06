/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.gmail.hilgardvr.fixme.model;

import java.util.*;

/**
 *
 * @author hilgard
 */
public class UserAccount {
    private double cashBalance;
    private Map<String, Integer> holdings;
    private Map<String, Double> holdingsValues;
    private double holdingCashValue;
    private double accountValue; 
    
    
    public UserAccount() {
        this.cashBalance = 1000000;
        this.holdings = new HashMap<String, Integer>();
        this.holdingsValues = new HashMap<String, Double>();
    }
    
    public double getCashBalance() {
        return this.cashBalance;
    }
    
    public Map<String, Integer> getHoldings() {
        Map<String, Integer> temp = new HashMap<>(this.holdings);
        return temp;
    }
    
    public void setHoldingsValue(Map<String, Double> newValues) {
        this.holdingsValues = newValues;
    }
    
    public void setCashBalance(double bal) {
        this.cashBalance = bal;
    }
    
    public void setHoldings(Map<String, Integer> mp) {
        this.holdings = mp;
    }

    public double calcHoldingsValue() {
        holdingCashValue = 0;
        for (Map.Entry<String, Integer> share : holdings.entrySet()) {
            double value = holdingsValues.get(share.getKey());
            holdingCashValue += value * share.getValue();
        }
        updateAccountValue();
        return holdingCashValue;
    }

    private void updateAccountValue() {
        accountValue = holdingCashValue + cashBalance;
    }

    public double getAccountValue() {
        return this.accountValue;
    }
}
