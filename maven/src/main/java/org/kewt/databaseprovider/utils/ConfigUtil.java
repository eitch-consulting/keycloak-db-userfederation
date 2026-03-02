package org.kewt.databaseprovider.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.kewt.databaseprovider.DBFederationConstants;
import org.keycloak.component.ComponentModel;

public final class ConfigUtil {

    protected static final Logger LOGGER = Logger.getLogger(ConfigUtil.class);

    public static Map<String, String> getCustomAttributeMapping(ComponentModel model) {
        List<String> mappingConfigs = model.getConfig().getList(DBFederationConstants.CONFIG_CUSTOM_ATTRIBUTE_TO_COLUMN_MAPPING);
        Map<String, String> attributeMapping = new HashMap<>();

        if (mappingConfigs != null && !mappingConfigs.isEmpty()) {
            for (String mapping : mappingConfigs) {
                if (mapping != null && mapping.contains("=")) {
                    String[] parts = mapping.split("=", 2);
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    if (!key.isEmpty() && !value.isEmpty()) {
                        attributeMapping.put(key, value);
                    } else {
                        LOGGER.errorv("Invalid key or value in keycloak attribute to database column mapping: {0}={1}", key, value);
                    }
                } else {
                    LOGGER.errorv("Invalid keycloak attribute to database column mapping: {0}", mapping);
                }
            }
        }
        return attributeMapping;
    }

}
