package org.kewt.databaseprovider.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.kewt.databaseprovider.utils.ConfigUtil;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;

public class WritableUserDelegate extends UserModelDelegate {

	protected static final Logger LOGGER = Logger.getLogger(WritableUserDelegate.class);
	
	protected DatabaseUser databaseUser;
	
	protected boolean dirty;
	
	protected Map<String, String> attributeMapping;
	
	public WritableUserDelegate(UserModel delegate, DatabaseUser databaseUser, ComponentModel model) {
		super(delegate);
		this.databaseUser = databaseUser;
		this.attributeMapping = ConfigUtil.getCustomAttributeMapping(model);
	}
	
	@Override
	public void setUsername(String username) {
		if (Objects.equals(getUsername(), username)) return;
		LOGGER.debugv("  setUsername: {0}", username);
		super.setUsername(username);
		databaseUser.setUsername(username);
		dirty = true;
	}
	
	@Override
	public void setEmail(String email) {
		if (Objects.equals(getEmail(), email)) return;
		LOGGER.debugv("  setEmail: {0}", email);
		super.setEmail(email);
		databaseUser.setEmail(email);
		dirty = true;
	}
	
	@Override
	public void setFirstName(String firstName) {
		if (Objects.equals(getFirstName(), firstName)) return;
		LOGGER.debugv("  setFirstName: {0}", firstName);
		super.setFirstName(firstName);
		databaseUser.setFirstName(firstName);
		dirty = true;
	}
	
	@Override
	public void setLastName(String lastName) {
		if (Objects.equals(getLastName(), lastName)) return;
		LOGGER.debugv("  setLastName: {0}", lastName);
		super.setLastName(lastName);
		databaseUser.setLastName(lastName);
		dirty = true;
	}
	
	@Override
	public void setAttribute(String name, List<String> values) {
		if (Objects.equals(getAttributeStream(name).collect(Collectors.toList()), values)) return;
		LOGGER.debugv("  setAttribute: {0}, {1}", name, values);
		String value = values != null && !values.isEmpty() ? values.get(0) : null;
		
		if (attributeMapping.containsKey(name)) {
			updateAttribute(name, value);
		} else {
			switch (name) {
				case "email":
					setEmail(value);
					break;
				case "firstName":
					setFirstName(value);
					break;
				case "lastName":
					setLastName(value);
					break;
				default:
					super.setAttribute(name, values);
					break;
			}
		}
	}
	
	@Override
	public void setSingleAttribute(String name, String value) {
		if (Objects.equals(getFirstAttribute(name), value)) return;
		LOGGER.debugv("  setSingleAttribute: {0}, {1}", name, value);
		
		if (attributeMapping.containsKey(name)) {
			updateAttribute(name, value);
		} else {
			switch (name) {
				case "email":
					setEmail(value);
					break;
				case "firstName":
					setFirstName(value);
					break;
				case "lastName":
					setLastName(value);
					break;
				default:
					super.setSingleAttribute(name, value);
					break;
			}
		}
	}
	
	private void updateAttribute(String name, String value) {
		Map<String, String> attributes = databaseUser.getAttributes();
		if (attributes == null) {
			attributes = new HashMap<>();
		}
		attributes.put(name, value);
		databaseUser.setAttributes(attributes);
		dirty = true;
	}
	
	public boolean isDirty() {
		return dirty;
	}
	
	public DatabaseUser getDatabaseUser() {
		return databaseUser;
	}
	
	@Override
	public String toString() {
		return "DatabaseUserDelegate[username" + getUsername() + ",email=" + getEmail() + ",firstName=" + getFirstName() + ",lastName=" + getLastName() + "]";
	}

}
