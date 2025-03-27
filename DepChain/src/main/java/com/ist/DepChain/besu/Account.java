package com.ist.DepChain.besu;

import org.hyperledger.besu.evm.account.MutableAccount;

public class Account {
    String address;
    String balance;
    public MutableAccount account;

    public Account(String address, String balance) {
        this.address = address;
        this.balance = balance;
    }

    @Override
    public String toString() {
        return "Address: " + address +
                "\nBalance: " + balance +
                "\n";
    }
}
