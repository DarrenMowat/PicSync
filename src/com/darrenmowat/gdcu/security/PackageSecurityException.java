package com.darrenmowat.gdcu.security;

/**
 * Exception which is thrown when this package has been signed differently or renamed
 * @author Darren Mowat
 *
 */
public class PackageSecurityException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	private String userIdentifier;

	public PackageSecurityException(String msg, String userIdentifier) {
		super(msg);
		this.userIdentifier = userIdentifier;
	}
	
	/**
	 * 
	 * @return the provided userIdentifier, may be null if one wasnt passed
	 */
	public String getUserIdentifier() {
		return userIdentifier;
	}
	
}