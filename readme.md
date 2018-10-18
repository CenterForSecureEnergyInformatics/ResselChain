ResselChain
===

This is an implementation of a private permissioned blockchain in Java.

Dependencies
---
ResselChain depends on the following software:
 - Java8
 - Sqlite Jdbc 3.19.3
 - Xom 1.2.10

How to run
---

1. Download the necessary dependencies and store them in ./lib.
2. Invoke the *init*, *unjar_dependencies*, *jars*, *compile* Ant Build targets in this order.
3. To implement your own consensus algorithm and state storage, see file *./src/statetable/AssetStateTable.java*.
4. Place the configuration for all nodes in ./conf (see nodeExample.xml and all.xml). You may use *GenerateKeyPair.java* in ./src/main to generate key pairs.  
5. Run *RC.jar* from the build directory. (`java -jar RC.jar conf/nodeExample.xml conf/all.xml`) 
    
    a) Use *UtilityTxSender.jar* to determine a default distribution of assets.
    
    b) Use *TxSender.jar* to send assets to other participants.
    
    c) Use *Status.jar* to observe the status of a node.
    
    d) Use *Amount.jar* to see the distribution of assets.
    
    e) Optionally use *BlockList.jar* and *XMLSender.jar* for debugging.
         

Licence
---
This code is licensed under a modified 3-Clause BSD License. See LICENSE file for details.


