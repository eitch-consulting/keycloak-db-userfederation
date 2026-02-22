package org.kewt.databaseprovider.model;

import java.util.Map;
import java.util.Objects;

import org.jboss.logging.Logger;
import org.keycloak.models.UserModel;

public class DatabaseUser {

	protected static final Logger LOGGER = Logger.getLogger(DatabaseUser.class);
	
	private Integer id;
	
	private String username;
	
	private String email;
	
	private String passwordHash;
	
	private String firstName;
	
	private String lastName;
	
	private Map<String, String> attributes;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
	
	public String getPasswordHash() {
		return passwordHash;
	}
	
	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}
	
	@Override
	public String toString() {
		return "DatabaseUser[username="+ username + ",email=" + email + ",firstName=" + firstName + ",lastName=" + lastName + ",attributes=" + attributes + "]";
	}
	
	public boolean outOfSync(UserModel user) {
		if (!(Objects.equals(username, user.getUsername())) ||
			!(Objects.equals(email, user.getEmail())) ||
			!(Objects.equals(firstName, user.getFirstName())) ||
			!(Objects.equals(lastName, user.getLastName()))) {
			return true;
		}
		
		if (attributes != null) {
			for (Map.Entry<String, String> entry : attributes.entrySet()) {
				String attrName = entry.getKey();
				String attrValue = entry.getValue();
				String localValue = user.getFirstAttribute(attrName);
				if (!Objects.equals(attrValue, localValue)) {
					LOGGER.debugv("  outOfSync: attrName ''{0}'', attrValue ''{1}'', localValue ''{2}''", attrName, attrValue, localValue);
					return true;
				}
			}
		}
		
		return false;
	}
	
	public void syncToUserModel(UserModel user) {
		user.setUsername(this.username);
		user.setEmail(this.email);
		user.setFirstName(this.firstName);
		user.setLastName(this.lastName);
		
		if (attributes != null) {
			for (Map.Entry<String, String> entry : attributes.entrySet()) {
				String attrName = entry.getKey();
				String attrValue = entry.getValue();
				user.setSingleAttribute(attrName, attrValue);
			}
		}
	}

}
