/*
* This file is part of ResselChain.
* Copyright Center for Secure Energy Informatics 2018
* Fabian Knirsch, Andreas Unterweger, Clemens Brunner
* This code is licensed under a modified 3-Clause BSD License. See LICENSE file for details.
*/

package at.entrust.resselchain.tree;

@SuppressWarnings("serial")
public class InvalidBlockOrderException extends Exception {
	  public InvalidBlockOrderException() { super(); }
	  public InvalidBlockOrderException(String message) { super(message); }
	  public InvalidBlockOrderException(String message, Throwable cause) { super(message, cause); }
	  public InvalidBlockOrderException(Throwable cause) { super(cause); }
}
