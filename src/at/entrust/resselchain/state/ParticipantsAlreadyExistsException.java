/*
* This file is part of ResselChain.
* Copyright Center for Secure Energy Informatics 2018
* Fabian Knirsch, Andreas Unterweger, Clemens Brunner
* This code is licensed under a modified 3-Clause BSD License. See LICENSE file for details.
*/

package at.entrust.resselchain.state;

@SuppressWarnings("serial")
public class ParticipantsAlreadyExistsException extends Exception {
	  public ParticipantsAlreadyExistsException() { super(); }
	  public ParticipantsAlreadyExistsException(String message) { super(message); }
	  public ParticipantsAlreadyExistsException(String message, Throwable cause) { super(message, cause); }
	  public ParticipantsAlreadyExistsException(Throwable cause) { super(cause); }
}
