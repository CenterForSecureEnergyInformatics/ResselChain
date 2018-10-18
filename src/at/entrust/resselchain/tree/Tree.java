/*
* This file is part of ResselChain.
* Copyright Center for Secure Energy Informatics 2018
* Fabian Knirsch, Andreas Unterweger, Clemens Brunner
* This code is licensed under a modified 3-Clause BSD License. See LICENSE file for details.
*/

package at.entrust.resselchain.tree;
import java.util.ArrayList;
import java.util.Arrays;
import at.entrust.resselchain.chain.Block;
import at.entrust.resselchain.state.ChainState;

public class Tree {
	private TreeNode root = null;
	private ArrayList<TreeNode> lastNodes = new ArrayList<>(); // list of last node in the longest chains
	
	private ArrayList<TreeNode> lastNodesForReversion = new ArrayList<>();
	private TreeNode insertedNodeForReversion = null;
	
	public TreeNode getRootNode() {
		return root;
	}
	
	public ArrayList<TreeNode> getLastNodes() {
		return lastNodes;
	}
	
	// do not call this method after initial setup of this node
	public void setLastNodes(ArrayList<TreeNode> lastNodes, TreeNode root) {
		this.root = root;
		this.lastNodes = lastNodes;
	}
	
	public void appendSubTreeByBlockNumber(Tree tree) throws InvalidBlockOrderException {
		TreeNode root = tree.getRootNode();
		
		// Append the root node of the sub tree to the main tree
		TreeNode newNode = appendBlockByBlockNumber(root.getBlock());
		
		// Append this tree's nodes to the newly created node in the main tree
		if (root.getChildren().size() == 0)
			return;
		
		for (TreeNode n : root.getChildren())
			newNode.addChild(n); // this keeps all references to further children; they are all in the main tree now
		
		if (tree.getLastNodes().get(0).getBlock().getBlockNumber() == lastNodes.get(0).getBlock().getBlockNumber()) {
			// if the last block number in the sub tree is equal to the block number in the longest chain of the main tree
			// then add this to the nodes of the longest chain
			lastNodes.addAll(tree.getLastNodes());
		}
		else if (tree.getLastNodes().get(0).getBlock().getBlockNumber() > lastNodes.get(0).getBlock().getBlockNumber()) {
			// if the last block number in the sub tree is greater than the block number in the longest chain
			// then make this the longest chain in the main tree
			lastNodes.clear();
			lastNodes.addAll(tree.getLastNodes());
		}
		
	}
	
	public TreeNode appendRevertableBlockByBlockNumber(Block block) throws InvalidBlockOrderException {
		// copy list of last nodes
		lastNodesForReversion.clear();
		for (TreeNode n : lastNodes) {
			lastNodesForReversion.add(n);
		}
		// append block and get reference to inserted node
		insertedNodeForReversion = appendBlockByBlockNumber(block);
		return insertedNodeForReversion;
	}
	
	public void revertLastBlockInsertion() {
		if (insertedNodeForReversion == null) return; // nothing to revert
		// reverse list of last nodes
		lastNodes.clear();
		for (TreeNode n : lastNodesForReversion) {
			lastNodes.add(n);
		}

		//if genesis block
		if (insertedNodeForReversion.getParent() == null)
			return;

		// delete last inserted node from file system

		insertedNodeForReversion.getParent().removeChildren(insertedNodeForReversion);
		TreeNode.delete(insertedNodeForReversion);
		insertedNodeForReversion = null;
	}
	
	public TreeNode appendBlockByBlockNumber(Block block) throws InvalidBlockOrderException {
		if (root == null && lastNodes.size() != 0)
			throw new InvalidBlockOrderException("Tree not properly initialized");
		
		if (root == null) {
			// this adds the root node
			root = new TreeNode(block, null);
			lastNodes.add(root);
			return root;
		}
		
		long newBlockNumber = block.getBlockNumber();
		
		// Debug only
		/*System.out.print("Last nodes: ");
		for(TreeNode node : lastNodes) {
			System.out.print(node.getBlock().getBlockNumber() + " ");
		}
		System.out.println("");
		*/
		
		// check if new block is a child of any last block in the longest chain
		for(TreeNode node : lastNodes) {
			if (newBlockNumber == node.getBlock().getBlockNumber() + 1) {
				// simply append block to longest chain
				return appendBlockToLongestChain(block);
			}
		}
		
		// otherwise, find block with proper number to append new block
		// Note: At this point there must exist at least one last node; all last nodes have the same block number
		// we therefore retrieve the block number from last node with index 0 (which must exist)
		long currentBlockNumber = lastNodes.get(0).getBlock().getBlockNumber();
		
		if (currentBlockNumber < newBlockNumber)
			throw new InvalidBlockOrderException("The block number of the new block is larger than the longest chain");
		
		// check if new block is already a sibling
		ArrayList<TreeNode> siblings = TreeNode.getSiblings(block.getBlockNumber());
		for (TreeNode node : siblings) {
			if (node.getBlock().equals(block)) {
				// if yes, return
				return node;
			}
		}
		
		siblings = TreeNode.getSiblings(block.getBlockNumber() - 1);
		boolean match = false;
		TreeNode newChild = null;
		for (TreeNode node : siblings) {
			if (isValid(node.getBlock(), block)) {
				newChild = node.addChild(block);
				match = true;
				break;
			}
		}
		
		if (match == false) {
			throw new InvalidBlockOrderException("No matching parent found");
		}
		
		// check if newly added block is the longest chain now
		if (block.getBlockNumber() > currentBlockNumber) {
			lastNodes.clear();
			lastNodes.add(newChild);
		} else if (block.getBlockNumber() == currentBlockNumber) {
			lastNodes.add(newChild);
		}
		
		return newChild;
	}


	public Block getBlock(long blockNumber) throws InvalidBlockOrderException {
		TreeNode parent = getLastNodes().get(0).getParent();

		if (blockNumber == getLastNodes().get(0).getBlock().getBlockNumber()){
			return ChainState.INSTANCE.getLastBlock();
		}

		if (blockNumber > parent.getBlock().getBlockNumber())
			throw new InvalidBlockOrderException("Blocknumber is not known");

		if (blockNumber < 0){
			return getRootNode().getBlock();
		}

		while (parent.getBlock().getBlockNumber() > blockNumber){
			parent = parent.getParent();
		}

		return parent.getBlock();
	}

	public byte[] getBlockHash (long blockNumber) throws InvalidBlockOrderException{
		return getBlock(blockNumber).getBlockHash();
	}

	public TreeNode appendBlockToLongestChain(Block block) throws InvalidBlockOrderException {
		if (root == null && lastNodes.size() != 0)
			throw new InvalidBlockOrderException("Tree not properly initialized");
		
		if (root == null) {
			// this adds the root node
			root = new TreeNode(block, null);
			lastNodes.add(root);
			return root;
		}
		else if (lastNodes.size() == 1) {
			// there is exactly one longest chain
			// append new node and set the newly added node as the longest chain
			TreeNode parentNode = lastNodes.get(0);
			
			if (!isValid(parentNode.getBlock(), block))
				throw new InvalidBlockOrderException();
			
			TreeNode newNode = parentNode.addChild(block);
			lastNodes.clear();
			lastNodes.add(newNode);
			return newNode;
		}
		else {
			// there are more than one chains with equal length
			// try all chains if block fits and set the newly added node as the longest chain,
			// otherwise throw exception

			for (TreeNode parentNode : lastNodes) {		
				if (isValid(parentNode.getBlock(), block)) {
					TreeNode newNode = parentNode.addChild(block);
					lastNodes.clear();
					lastNodes.add(newNode);
					return newNode;
				}
			}
			
			throw new InvalidBlockOrderException();
		}
	}
	
	private boolean isValid(Block parent, Block child) {
		// check validity of added blocks
		// this only verifies if the chain is valid, but does not check
		// for internal block details, such as valid signature or valid transactions
		if (parent.getBlockNumber() != child.getBlockNumber()-1)
			return false;
		if (!Arrays.equals(parent.getBlockHash(), child.getPreviousBlockHash()))
			return false;
		return true;
	}
	
	public void travserseTree() {
		if (root == null) return;
		//traverse(root, 0);
	}
	
	private void traverse(TreeNode node, int level)
	{
		// Debug only: prints tree
		for(int i=0; i < level; i++)
			System.out.print(" ");
		System.out.print(node.getBlock().getBlockNumber() + " (" + node.getChildren().size() + ")\n");
		
		if (node.getChildren().size() == 0)
			return;
		for(TreeNode n : node.getChildren()) {
			traverse(n, level+1);
		}
	}

	public TreeNode getBlockOfLongestChain(long blockNumber) {
		//find first block with no fork
		TreeNode meinBlock = lastNodes.get(0);
		long longestChainBlockNumber = meinBlock.getBlock().getBlockNumber();

		for (long l = blockNumber; l < longestChainBlockNumber; l++){
			ArrayList<TreeNode> nodes = TreeNode.getSiblings(l);
			if (nodes.size() == 1) {
				meinBlock = nodes.get(0);
				break;
			}
		}

		//go back to requested blocknumber
		while(meinBlock.getBlock().getBlockNumber() > blockNumber){
			meinBlock = meinBlock.getParent();
		}

		return meinBlock;
	}
}
