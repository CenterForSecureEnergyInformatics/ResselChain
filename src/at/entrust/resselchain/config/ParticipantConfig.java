/*
* This file is part of ResselChain.
* Copyright Center for Secure Energy Informatics 2018
* Fabian Knirsch, Andreas Unterweger, Clemens Brunner
* This code is licensed under a modified 3-Clause BSD License. See LICENSE file for details.
*/

package at.entrust.resselchain.config;

import java.security.PrivateKey;
import java.security.PublicKey;

public class ParticipantConfig {

	private final PrivateKey privateKey;
	private final PublicKey publicKey;
	private final String name; // Name of this node
	private final String specialRole;
	
	public ParticipantConfig(PrivateKey privateKey, PublicKey publicKey, String name) {
		this(privateKey, publicKey, name, null); // special role is default null
	}
	
	public ParticipantConfig(PrivateKey privateKey, PublicKey publicKey, String name, String specialRole) {
		this.privateKey = privateKey;
		this.publicKey = publicKey;
		this.name = name;
		this.specialRole = specialRole;
	}
	
	public PrivateKey getPrivateKey() {
		return privateKey;
	}
	
	public PublicKey getPublicKey() {
		return publicKey;
	}
	
	public String getName() {
		return name;
	}
	
	public String getSpecialRole() {
		return specialRole;
	}
	
}
