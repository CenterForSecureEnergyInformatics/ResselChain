/*
* This file is part of ResselChain.
* Copyright Center for Secure Energy Informatics 2018
* Fabian Knirsch, Andreas Unterweger, Clemens Brunner
* This code is licensed under a modified 3-Clause BSD License. See LICENSE file for details.
*/

package at.entrust.resselchain.statetable;

@SuppressWarnings("serial")
public class AtomicTransactionException extends Exception {
	  public AtomicTransactionException() { super(); }
	  public AtomicTransactionException(String message) { super(message); }
	  public AtomicTransactionException(String message, Throwable cause) { super(message, cause); }
	  public AtomicTransactionException(Throwable cause) { super(cause); }
}
