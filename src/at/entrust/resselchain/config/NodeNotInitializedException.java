/*
* This file is part of ResselChain.
* Copyright Center for Secure Energy Informatics 2018
* Fabian Knirsch, Andreas Unterweger, Clemens Brunner
* This code is licensed under a modified 3-Clause BSD License. See LICENSE file for details.
*/

package at.entrust.resselchain.config;

@SuppressWarnings("serial")
public class NodeNotInitializedException extends Exception {
	  public NodeNotInitializedException() { super(); }
	  public NodeNotInitializedException(String message) { super(message); }
	  public NodeNotInitializedException(String message, Throwable cause) { super(message, cause); }
	  public NodeNotInitializedException(Throwable cause) { super(cause); }
}
