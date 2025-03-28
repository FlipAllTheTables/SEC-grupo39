package com.ist.DepChain.nodes;

import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import com.ist.DepChain.util.Pair;

public class NodeState {
    public Map<Integer, List<Integer>> acks;    //Guardar acknowledges esperados em cada comunicação
    public int seqNum;                          //HashMap<Integer, Integer> seqNums   Sequence numbers recebidos em cada comunicação
    public int myId;
    public int numNodes;
    public List<Integer> valts;
    public List<String> val;
    public List<Pair> consensusPairs;
    public int quorumSize;
    public int bizantineProcesses; 
    public int consensusIndex;
    public PrivateKey privateKey;
    public List<String> blockChain;
    public boolean isBizantine;
    public List<String> valuesToAppend;
    public boolean isClient;

    //TODO Adicionar HashMap<Integer, SecretKey> sharedKeys       HashMap que associa cada comunicação com uma chave partilhada

    public NodeState(int myId, int numNodes, boolean isBizantine) {
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
        bizantineProcesses = numNodes / 3;
        quorumSize = numNodes - bizantineProcesses - 1;
        this.consensusIndex = 0;
        this.isBizantine = isBizantine;
        this.valuesToAppend = new ArrayList<>();
        this.isClient = false;
    }

    public void addConsensusPair(int value1, String value2) {
        consensusPairs.add(new Pair(value1, value2));
    }
    
}
