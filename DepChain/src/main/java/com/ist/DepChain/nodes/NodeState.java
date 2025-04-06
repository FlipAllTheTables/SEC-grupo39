package com.ist.DepChain.nodes;

import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.crypto.spec.SecretKeySpec;

import java.util.HashMap;
import com.ist.DepChain.util.Pair;
import com.ist.DepChain.blocks.Block;;

public class NodeState {
    public Map<Integer, List<Integer>> acks;    //Guardar acknowledges esperados em cada comunicação
    public int seqNum;                          //HashMap<Integer, Integer> seqNums, guardar próximo seqNumber para cada comunicação
    public int myId;
    public int numNodes;
    public List<Integer> valts;
    public List<String> val;
    public List<Pair> consensusPairs;
    public int quorumSize;
    public int byzantineProcesses; 
    public int consensusIndex;
    public PrivateKey privateKey;
    public List<Block> blockChain;
    public int isByzantine;
    public List<String> valuesToAppend;
    public boolean isClient;
    public List<String> currentBlockTransactions;
    public HashMap<Integer, SecretKeySpec> sharedKeys;
    public HashMap<String, Integer> hashToClientMap;

    public NodeState(int myId, int numNodes, int isByzantine) {
        acks = new HashMap<>();
        seqNum = 0;
        this.myId = myId;
        this.numNodes = numNodes;
        blockChain = new ArrayList<>();
        valts = new ArrayList<>();
        val = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            valts.add(0);
            val.add("");
        }
        consensusPairs = new ArrayList<>();
        byzantineProcesses = numNodes / 3;
        quorumSize = numNodes - byzantineProcesses - 1;
        this.consensusIndex = 0;
        this.isByzantine = isByzantine;
        this.valuesToAppend = new ArrayList<>();
        this.isClient = false;
        this.currentBlockTransactions = new ArrayList<>();
        this.sharedKeys = new HashMap<>();
        this.hashToClientMap = new HashMap<>();
    }

    public void addConsensusPair(int value1, String value2) {
        consensusPairs.add(new Pair(value1, value2));
    }
    
}
