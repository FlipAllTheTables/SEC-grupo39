package com.ist.DepChain.besu;

import java.security.PublicKey;

import org.hyperledger.besu.evm.account.MutableAccount;

public class Account {
    String address;
    String balance;
    public MutableAccount account;
    PublicKey publicKey;

    public Account(String address, String balance, PublicKey publicKey) {
        this.publicKey = publicKey;
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
