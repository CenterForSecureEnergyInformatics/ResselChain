/*
* This file is part of ResselChain.
* Copyright Center for Secure Energy Informatics 2018
* Fabian Knirsch, Andreas Unterweger, Clemens Brunner
* This code is licensed under a modified 3-Clause BSD License. See LICENSE file for details.
*/

package at.entrust.resselchain.mining;

@SuppressWarnings("serial")
public class NothingToMineException extends Exception {
	  public NothingToMineException() { super(); }
	  public NothingToMineException(String message) { super(message); }
	  public NothingToMineException(String message, Throwable cause) { super(message, cause); }
	  public NothingToMineException(Throwable cause) { super(cause); }
}
