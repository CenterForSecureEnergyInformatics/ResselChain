/*
* This file is part of ResselChain.
* Copyright Center for Secure Energy Informatics 2018
* Fabian Knirsch, Andreas Unterweger, Clemens Brunner
* This code is licensed under a modified 3-Clause BSD License. See LICENSE file for details.
*/

package at.entrust.resselchain.chain;

import java.io.Serializable;
import java.util.Arrays;

public class Participant implements Serializable {

	String name;
	byte[] publickey;
	String address; // IP-Address
	int port; // Port this participant listens on
	String specialRole;
	
	public Participant(String name, byte[] publickey, String address, int port) {
		this(name, publickey, address, port, null);
	}
	
	public Participant(String name, byte[] publickey, String address, int port, String specialRole) {
		super();
		this.name = name;
		this.publickey = publickey;
		this.address = address;
		this.port = port;
		this.specialRole = specialRole;
	}
	
	public Participant(String name) {
		super();
		this.name = name;
	}
	
	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((address == null) ? 0 : address.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + port;
		result = prime * result + Arrays.hashCode(publickey);
		result = prime * result + ((specialRole == null) ? 0 : specialRole.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Participant))
			return false;
		Participant other = (Participant) obj;
		if (address == null) {
			if (other.address != null)
				return false;
		} else if (!address.equals(other.address))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (port != other.port)
			return false;
		if (!Arrays.equals(publickey, other.publickey))
			return false;
		if (specialRole == null) {
			if (other.specialRole != null)
				return false;
		} else if (!specialRole.equals(other.specialRole))
			return false;
		return true;
	}

	public String getName() {
		return name;
	}
	
	public byte[] getPublickey() {
		return publickey;
	}
	
	public String getAddress() {
		return address;
	}
	
	public int getPort() {
		return port;
	}
	
}
