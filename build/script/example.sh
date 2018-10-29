# Command to create a new asset
# java -jar UtilityTxSender.jar -sxml SendingNode ReceivingNode NameOfAsset MaxAmount Node InitialAmount [Node InitialAmount]
java -jar UtilityTxSender.jar -sxml node1.xml node1.xml myNewAssetName 1000 node1.xml 5000 node2.xml 2500 node3.xml 2500

# Command to send a transaction
# java -jar TxSender.jar -sxml SendingNode ReceivingNode NameOfAsset Value
java -jar TxSender.jar -sxml node1.xml node2.xml myNewAssetName 100