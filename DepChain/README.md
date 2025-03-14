# HDS project : Depchain

## Running the program
First of all, create as many terminal instances as processes that you want to join the blockchain and change their directories to inside Depchain.

Then run this command in one of the terminals, to compile the code: 
`mvn clean install`

To run the program in every terminal, type the following command:
`mvn exec:java -Dexec.args="nodeId numNodes isBizantine"`
Where:
 - `nodeId` is the Id of that specific node. This id needs to be unique and you will get an error if you try to use a repeated value;
 - `numNodes` is the total number of nodes that are participating in the consensus. This value needs to be the same for all the processes, and it needs to correspond to the actual number of nodes participating;
 - `isBizantine` is either set to 0 or 1. This boolean determines if a node will act arbitrarily when sending messages with important information. Note that the number of bizantine nodes cannot be equal or exceed 1/3 of `numNodes`. 