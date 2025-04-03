# HDS project : Depchain

## Running the program
First of all, create as many terminal instances as processes that you want to join the blockchain and change their directories to inside Depchain.

Then run this command in one of the terminals, to compile the code: 
`mvn clean install`

To run the program in every terminal, type the following command:
`mvn exec:java -Dexec.args="nodeId numNodes isBizantine isClient"`
Where:
 - `nodeId` is the Id of that specific node. This id needs to be unique and you will get an error if you try to use a repeated value;
 - `numNodes` is the total number of nodes that are participating in the consensus. This value needs to be the same for all the processes, and it needs to correspond to the actual number of nodes participating;
 - `isBizantine` is either set to 0 or 1. This boolean determines if a node will act arbitrarily when sending messages with important information. Note that the number of bizantine nodes cannot be equal or exceed 1/3 of `numNodes`. 
 - `isClient` is either set to 0 or 1. This boolean determines if a node will act as a client only being able to send requests to the processes.

 When all the programs are running, you should go on the terminal that you defined to be the client, and write: `APPEND arg`, where arg is the string you want to append to the blockchain. You can check in all the other terminals the output of this request. You can also send an `READALL` request, that reads the entire blockchain.

 In the folder Tests, there are two python scripts that simulate two different scenarios:
  - The first consists in 5 nodes that run the algorithm, with no bizantine nodes. To run this scenario, just open the `/Tests` directory and run scenario1.py (This only works in a wsl environment where you must have maven installed). This will automatically open 6 terminals. On the last terminal simple write `APPEND` followed by a random string to write somethin on the blockchain. Then to read what has been written in the blockchain simply write `READALL`.
  - The second scenario is similar two the first but there are now 6 nodes that run the algorithm and two of these nodes are bizantine. Following the same process as before, you will obtain similar results even with bizantine nodes.

  javac -cp "jars/*" -d bin CreateGenesisBlock.java
  java -cp "bin:jars/*" com.ist.DepChain.besu.CreateGenesisBlock
