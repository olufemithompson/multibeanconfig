package com.olufemithompson.multiplebean;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class Utils {
    protected static String formatConfigProperties(String input){
        return input.replaceAll("\\$\\{|\\}", "");
    }

    protected static  String getFirstKey(String propertyKeyPath){
        if(!propertyKeyPath.contains(".")){
            return propertyKeyPath;
        }
        return propertyKeyPath.substring(0, propertyKeyPath.indexOf("."));
    }

    protected static Map<String, Object> generateNestedMap(Map<String, Object> properties) {
        Map<String, Object> rootMap = new HashMap<>();
        Set<String> keys = properties.keySet();
        for (String key : keys) {
            String[] parts = key.split("\\.");
            Map<String, Object> currentMap = rootMap;

            for (int i = 0; i < parts.length - 1; i++) {
                String part = parts[i];
                currentMap = (Map<String, Object>) currentMap.computeIfAbsent(part, k -> new HashMap<>());
            }

            String finalPart = parts[parts.length - 1];
            currentMap.put(finalPart, properties.get(key));
        }

        return rootMap;
    }

    protected static <T> T extractDataFromMap(final String key,
                                     final Map<String, ?> data) {
        if (!data.containsKey(key)) {
            throw new RuntimeException(String.format("Could not load data. Expecting key : %s", key));
        }
        return (T) data.get(key);
    }
}
