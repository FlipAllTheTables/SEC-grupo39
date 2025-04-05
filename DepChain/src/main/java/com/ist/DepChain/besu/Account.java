package com.ist.DepChain.besu;

import java.security.PublicKey;

import org.hyperledger.besu.evm.account.MutableAccount;

public class Account {
    String address;
    String balanceDep;
    String balanceIst;
    public MutableAccount account;
    PublicKey publicKey;

    public Account(String address, String balance, PublicKey publicKey) {
        this.publicKey = publicKey;
        this.address = address;
        this.balanceDep = balance;
    }

    @Override
    public String toString() {
        return "Address: " + address +
                "\nBalance: " + balanceDep +
                "\n";
    }
}
