# HDS project : Depchain

## Running the program
Before anyhting, if you want to alter any information in the genesis block, like adding more accounts or changing their balance, you should go to `CreateGenesisBlock.java` and change it there. Then run the following commands:

javac -cp "jars/*" -d bin CreateGenesisBlock.java
java -cp "bin:jars/*" com.ist.DepChain.besu.CreateGenesisBlock

This will also create the RSA keys associated with each account

First of all, create as many terminal instances as processes that you want to join the blockchain and change their directories to inside Depchain.

Then run this command in one of the terminals, to compile the code: 
`mvn clean install`

To run the program in every terminal, type the following command:
`mvn exec:java -Dexec.args="nodeId numNodes testMode isClient"`
Where:
 - `nodeId` is the Id of that specific node. This id needs to be unique and you will get an error if you try to use a repeated value;
 - `numNodes` is the total number of nodes that are participating in the consensus. This value needs to be the same for all the processes, and it needs to correspond to the actual number of nodes participating;
 - `isBizantine` is either set to 0 or 1. This boolean determines if a node will act arbitrarily when sending messages with important information. Note that the number of bizantine nodes cannot be equal or exceed 1/3 of `numNodes`. 
 - `isClient` is either set to 0 or 1. This boolean determines if a node will act as a client only being able to send requests to the processes.

 When all the programs are running, you should go on the terminal that you defined to be the client, and write: `TX` to open the menu to create a transaction. From there you can select the address of the sender of the message and the address of the receiver of the message as well as a value of DepCoin to be transacted.
 Then you will have to choose what type of transaction you want done in the smart contract:

 - transfer: send some IstCoin from your account to the receiver's address.
 - transferFrom: send some IstCoin from a sender account to a receiver account. The sender of this message must have permission to access the sender's balance.
 - addToBlackList: Add an address to the blacklist (can only be done by the owner of the contract).
 - removeFromBlackList: Remove an address from the blacklist (can only be done by the owner of the contract).
 - approve: Allows an account to spend some ammount of istCoin from your account. 

 There is also an option to generate 10 random transactions. This is done for debugging purposes, as you need 10 transactions to fill a block and start the consensus protocol. To perform this action, simple type `LOOP` in the client terminal.
