package org.kewt.databaseprovider.repository;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ObjectUtils;
import org.jboss.logging.Logger;
import org.kewt.databaseprovider.DBFederationConstants;
import org.kewt.databaseprovider.database.DatabaseConnection;
import org.kewt.databaseprovider.database.callbacks.QueryReader;
import org.kewt.databaseprovider.model.DatabaseUser;
import org.kewt.databaseprovider.utils.Pair;
import org.keycloak.component.ComponentModel;
import org.keycloak.util.JsonSerialization;

public class DatabaseUserRepository {

	protected static final Logger LOGGER = Logger.getLogger(DatabaseUserRepository.class);

	private DatabaseConnection connection;
	
	private ComponentModel model;
	
	private String usersTable;
	
	private String idColumn;
	
	private String usernameColumn;
	
	private String emailColumn;
	
	private String firstNameColumn;
	
	private String lastNameColumn;
	
	private String passwordColumn;
	
	private Map<String, Integer> columnTypes;
	
	private QueryReader<DatabaseUser> reader;
	
	private Map<String, String> attributeMapping;
	
	public DatabaseUserRepository(DatabaseConnection connection, ComponentModel model) {
		this.connection = connection;
		this.model = model;
		this.usersTable = ObjectUtils.firstNonNull(this.model.get(DBFederationConstants.CONFIG_USERS_TABLE), "users");
		this.idColumn = ObjectUtils.firstNonNull(this.model.get(DBFederationConstants.CONFIG_ID_COLUMN), "id");
		this.usernameColumn = ObjectUtils.firstNonNull(this.model.get(DBFederationConstants.CONFIG_USERNAME_COLUMN), "username");
		this.emailColumn = ObjectUtils.firstNonNull(this.model.get(DBFederationConstants.CONFIG_EMAIL_COLUMN), "email");
		this.firstNameColumn = ObjectUtils.firstNonNull(this.model.get(DBFederationConstants.CONFIG_FIRSTNAME_COLUMN), "first_name");
		this.lastNameColumn = ObjectUtils.firstNonNull(this.model.get(DBFederationConstants.CONFIG_LASTNAME_COLUMN), "last_name");
		this.passwordColumn = ObjectUtils.firstNonNull(this.model.get(DBFederationConstants.CONFIG_PASSWORD_COLUMN), "password_hash");
		
		this.columnTypes = connection.getColumnTypes(this.usersTable);
		LOGGER.debugv("  columnTypes: {0}", columnTypes);
		
		String mappingConfig = model.get(DBFederationConstants.CONFIG_CUSTOM_ATTRIBUTE_TO_COLUMN_MAPPING);
		Map<String,String> attributeMapping = new HashMap<>();

		if (mappingConfig != null && !mappingConfig.trim().isEmpty()) {
			try {
				List<Pair> pairs = JsonSerialization.readValue(
					mappingConfig,
					new com.fasterxml.jackson.core.type.TypeReference<List<Pair>>() {}
				);
				for (Pair p : pairs) {
					if (p != null && p.key != null) attributeMapping.put(p.key, p.value);
				}
			} catch (IOException e) {
				throw new RuntimeException("Failed to parse attribute mapping configuration", e);
			}
		}
		this.attributeMapping = attributeMapping;
		LOGGER.debugv("  attributeMapping: {0}", attributeMapping);

		this.reader = (ResultSet rs) -> {
			Set<String> columns = new HashSet<>();
			ResultSetMetaData metadata = rs.getMetaData();
		    for (int i = 1; i <= metadata.getColumnCount(); i++) {
		    	columns.add(metadata.getColumnName(i));
		    }

			DatabaseUser user = new DatabaseUser();
			if (columns.contains(idColumn)) {
				user.setId(rs.getInt(idColumn));
			}
			if (columns.contains(usernameColumn)) {
				user.setUsername(rs.getString(usernameColumn));
			}
			if (columns.contains(emailColumn)) {
				user.setEmail(rs.getString(emailColumn));
			}
			if (columns.contains(firstNameColumn)) {
				user.setFirstName(rs.getString(firstNameColumn));
			}
			if (columns.contains(lastNameColumn)) {
				user.setLastName(rs.getString(lastNameColumn));
			}
			if (columns.contains(passwordColumn)) {
				user.setPasswordHash(rs.getString(passwordColumn));
			}
			
			Map<String, String> attributes = new HashMap<>();
			for (Map.Entry<String, String> entry : attributeMapping.entrySet()) {
				String attributeName = entry.getKey();
				String columnName = entry.getValue();
				if (columns.contains(columnName)) {
					Object value = rs.getObject(columnName);
					attributes.put(attributeName, value != null ? value.toString() : null);
				}
			}
			user.setAttributes(attributes);
			
			return user;
		};
	}
	
	public List<DatabaseUser> listUsers() {
		String sql = "select " + getColumns() + " from " + usersTable;
		return connection.queryList(sql, null, reader);
	}
	
	public List<DatabaseUser> listUsers(Integer firstResult, Integer maxResults) {
		String sql = "select " + getColumns() + " from " + usersTable + " limit ? offset ?";
		return connection.queryList(sql, (PreparedStatement statement) -> {
			statement.setInt(1, maxResults);
			statement.setInt(2, firstResult);
		}, reader);
	}
	
	public List<DatabaseUser> searchUsers(String search, Integer firstResult, Integer maxResults) {
		if (search.equals("*")) {
			return listUsers(firstResult, maxResults);
		}
		String sql = "select " + getColumns() + " from " + usersTable + " where " + usernameColumn + " like ? or " + usernameColumn + " like ? or " + firstNameColumn + " like ? or " + lastNameColumn + " like ? limit ? offset ?";
		String value = "%" + search + "%";
		return connection.queryList(sql, (PreparedStatement statement) -> {
			statement.setString(1, value);
			statement.setString(2, value);
			statement.setString(3, value);
			statement.setString(4, value);
			statement.setInt(5, maxResults);
			statement.setInt(6, firstResult);
			
		}, reader);
	}
	
	public Integer countUsers() {
		return connection.querySingle("select count(*) from " + usersTable, null, (ResultSet rs) -> {
			return rs.getInt(1);
		});
	}
	
	public DatabaseUser getUserById(Integer id) {
		String sql = "select " + getColumns() + " from " + usersTable + " where " + idColumn + " = ?";
		return connection.querySingle(sql, (PreparedStatement statement) -> {
			statement.setInt(1, id);
		}, reader);
	}
	
	public DatabaseUser getUserByUsername(String username) {
		String sql = "select " + getColumns() + " from " + usersTable + " where " + usernameColumn + " = ?";
		return connection.querySingle(sql, (PreparedStatement statement) -> {
			statement.setString(1, username);
		}, reader);
	}
	
	public DatabaseUser getUserByEmail(String email) {
		String sql = "select " + getColumns() + " from " + usersTable + " where " + emailColumn + " = ?";
		return connection.querySingle(sql, (PreparedStatement statement) -> {
			statement.setString(1, email);
		}, reader);
	}
	
	public boolean updatePassword(Integer id, String password) {
		String sql = "update " + usersTable + " set " + passwordColumn + " = ? where " + idColumn + " = ?";
        return connection.execute(sql, (PreparedStatement statement) -> {
        	statement.setString(1, password);
			statement.setInt(2, id);
        }) > 0;
	}
	
	public Integer insert(DatabaseUser user) {
		List<String> columns = new ArrayList<>();
		List<String> values = new ArrayList<>();

		columns.add(usernameColumn);
		values.add("?");
		
		columns.add(emailColumn);
		values.add("?");
		
		columns.add(firstNameColumn);
		values.add("?");
		
		columns.add(lastNameColumn);
		values.add("?");
		
		columns.add(passwordColumn);
		values.add("?");
		
		if (user.getAttributes() != null) {
			for (Map.Entry<String, String> entry : user.getAttributes().entrySet()) {
				String attributeName = entry.getKey();
				String attributeValue = entry.getValue();
				if (attributeMapping.containsKey(attributeName)) {
					columns.add(attributeMapping.get(attributeName));
					values.add("?");
				}
			}
		}

		String sql = "insert into " + usersTable + " (" + String.join(", ", columns) + ") values (" + String.join(", ", values) + ")";
		return connection.executeAndReturnGeneratedKeys(sql, (PreparedStatement statement) -> {
			int i = 1;
			statement.setString(i++, user.getUsername());
			statement.setString(i++, user.getEmail());
			statement.setString(i++, user.getFirstName());
			statement.setString(i++, user.getLastName());
			statement.setString(i++, user.getPasswordHash());
			
			if (user.getAttributes() != null) {
				for (Map.Entry<String, String> entry : user.getAttributes().entrySet()) {
					String attributeName = entry.getKey();
					if (attributeMapping.containsKey(attributeName)) {
						String columnName = attributeMapping.get(attributeName);
						statement.setObject(i++, convertToTargetType(columnName, entry.getValue()));
					}
				}
			}
		}, (ResultSet rs) -> {
			user.setId(rs.getInt(1));
			return user.getId();
		});
	}
	
	public boolean update(DatabaseUser user) {
		List<String> sets = new ArrayList<>();
		sets.add(usernameColumn + " = ?");
		sets.add(emailColumn + " = ?");
		sets.add(firstNameColumn + " = ?");
		sets.add(lastNameColumn + " = ?");
		
		if (user.getAttributes() != null) {
			for (Map.Entry<String, String> entry : user.getAttributes().entrySet()) {
				String attributeName = entry.getKey();
				if (attributeMapping.containsKey(attributeName)) {
					sets.add(attributeMapping.get(attributeName) + " = ?");
				}
			}
		}
		
		String sql = "update " + usersTable + " set " + String.join(", ", sets) + " where " + idColumn + " = ?";
		return connection.execute(sql, (PreparedStatement statement) -> {
			int i = 1;
			statement.setString(i++, user.getUsername());
			statement.setString(i++, user.getEmail());
			statement.setString(i++, user.getFirstName());
			statement.setString(i++, user.getLastName());
			
			if (user.getAttributes() != null) {
				for (Map.Entry<String, String> entry : user.getAttributes().entrySet()) {
					String attributeName = entry.getKey();
					if (attributeMapping.containsKey(attributeName)) {
						String columnName = attributeMapping.get(attributeName);
						statement.setObject(i++, convertToTargetType(columnName, entry.getValue()));
					}
				}
			}
			
			statement.setInt(i++, user.getId());
		}) > 0;
	}
	
	public boolean delete(DatabaseUser user) {
		String sql = "delete from " + usersTable + " where " + idColumn + " = ?";
		return connection.execute(sql, (PreparedStatement statement) -> {
			statement.setInt(1, user.getId());
		}) > 0;
	}
	
	
	// Private Methods
	
	private Object convertToTargetType(String columnName, String value) {
		if (value == null) return null;
		Integer targetType = columnTypes.get(columnName);
		if (targetType == null) return value;
		
		try {
			switch (targetType) {
				case Types.INTEGER:
				case Types.SMALLINT:
				case Types.TINYINT:
					return Integer.valueOf(value);
				case Types.BIGINT:
					return Long.valueOf(value);
				case Types.FLOAT:
				case Types.REAL:
					return Float.valueOf(value);
				case Types.DOUBLE:
					return Double.valueOf(value);
				case Types.DECIMAL:
				case Types.NUMERIC:
					return new BigDecimal(value);
				default:
					return value;
			}
		} catch (Exception e) {
			LOGGER.warnv("Failed to convert value ''{0}'' to type {1} for column {2}", value, targetType, columnName);
			return value;
		}
	}
	
	private String getColumns() {
		List<String> columns = new ArrayList<>();
		columns.add(idColumn);
		columns.add(usernameColumn);
		columns.add(emailColumn);
		columns.add(firstNameColumn);
		columns.add(lastNameColumn);
		columns.add(passwordColumn);
		columns.addAll(attributeMapping.values());
		return String.join(",", columns);
	}

}