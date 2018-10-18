/*
* This file is part of ResselChain.
* Copyright Center for Secure Energy Informatics 2018
* Fabian Knirsch, Andreas Unterweger, Clemens Brunner
* This code is licensed under a modified 3-Clause BSD License. See LICENSE file for details.
*/

package at.entrust.resselchain.tree;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import at.entrust.resselchain.chain.Block;

public class TreeNode implements Serializable {

	private static final long serialVersionUID = -6983773305822756070L;
	private ArrayList<Long> children = new ArrayList<>();
	private Block block = null;
	private long parent = -1;
	private long nodeId = -1;
	
	private static long nodeCount = 0;
	
	public TreeNode() {
		// TODO Auto-generated constructor stub
	}
	
	public TreeNode(Block block, TreeNode parent) {
		super();
		this.block = block;
		this.parent = (parent == null) ? -1 : parent.nodeId;
		this.nodeId = nodeCount++;
		save(this);
	}
	
	public TreeNode(Block block, long parent) {
		super();
		this.block = block;
		this.parent = parent;
		this.nodeId = nodeCount++;
		save(this);
	}
	
	public TreeNode addChild(Block block) {
		TreeNode newNode = new TreeNode(block, nodeId);
		save(newNode);
		children.add(newNode.nodeId);
		save(this);
		return newNode;
	}
	
	public TreeNode addChild(TreeNode node) {
		node.parent = nodeId;
		save(node);
		children.add(node.nodeId);
		save(this);
		return node;
	}
	
	public TreeNode clearChildren() {
		children.clear();
		save(this);
		return this;
	}
	
	public TreeNode removeChildren(TreeNode node) {
		children.remove(node.nodeId);
		save(this);
		return this;
	}
	
	public Block getBlock() {
		return block;
	}
	
	public TreeNode getParent() {
		return load(getBlock().getBlockNumber() -1, parent);
	}
	
	public ArrayList<TreeNode> getChildren() {
		ArrayList<TreeNode> nodes = new ArrayList<>();
		for (Long n : children) {
			nodes.add(load(getBlock().getBlockNumber()+1, n));
		}
		return nodes;
	}
	
	//private static HashMap<Long, TreeNode> treeNodes = new HashMap<>();
	
	public static void delete(TreeNode node) {
		File f = new File("chainstate/" + node.getBlock().getBlockNumber() + "-" + node.nodeId);
		f.delete();
	}
	
	public static void save(TreeNode node) {
		//treeNodes.put(node.nodeId, node);
		FileOutputStream fileOut = null;
		GZIPOutputStream zo = null;
		ObjectOutputStream out = null;
		try {
			File f = new File("chainstate/" + node.getBlock().getBlockNumber() + "-" + node.nodeId);
			if (f.exists()) f.delete();
			f.createNewFile();
			fileOut = new FileOutputStream(f);
			zo = new GZIPOutputStream(fileOut);
			out = new ObjectOutputStream(zo);
			out.writeObject(node);
			out.flush();
		} catch(IOException e) {
			e.printStackTrace();
		} finally {
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				zo.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				fileOut.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static TreeNode load(Block block){
		ArrayList<TreeNode> nodes = getSiblings(block.getBlockNumber());
		for (TreeNode tn : nodes){
			if (tn.getBlock().equals(block))
				return tn;
		}
		return null;
	}
	
	public static TreeNode load(long blockNumber, long nodeId) {
		//if (nodeId == -1) return null;
		//return treeNodes.get(nodeId);
		
		if (nodeId == -1) return null;
		TreeNode n = null;
		
		FileInputStream fileIn = null;
		GZIPInputStream zi = null;
		ObjectInputStream in = null;
		try {
			fileIn = new FileInputStream("chainstate/" + blockNumber + "-" + nodeId);
			zi = new GZIPInputStream(fileIn);
			in = new ObjectInputStream(zi);
			n = (TreeNode)in.readObject();
		}catch(IOException e) {
			e.printStackTrace();
		}catch(ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				zi.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				fileIn.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return n;	
	}
	
	public static ArrayList<TreeNode> getSiblings(long blockNumber) {
		File dir = new File("chainstate");
		File[] files = dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.matches(blockNumber + "-.*");
			}
		});
		ArrayList<TreeNode> ret = new ArrayList<>();
		for(File f : files) {
			ret.add(load(blockNumber, Long.valueOf(f.getName().split("-")[1])));
		}
		return ret;
	}
	
	// do not call this method after initial setup of this node
	public static void setNodeCount(long nodeCount) {
		TreeNode.nodeCount = nodeCount;
	}
}
