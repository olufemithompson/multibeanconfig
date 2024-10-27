package com.olufemithompson.multibeanconfig;

import org.springframework.boot.origin.OriginTrackedValue;

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

            boolean shouldProceed = true;
            for (int i = 0; i < parts.length - 1; i++) {
                String part = parts[i];
                try{
                    currentMap = (Map<String, Object>) currentMap.computeIfAbsent(part, k -> new HashMap<>());

                }
                catch (java.lang.ClassCastException e){
                    shouldProceed=false;
                }
            }

            if(shouldProceed){
                String finalPart = parts[parts.length - 1];
                currentMap.put(finalPart, properties.get(key));
            }
        }

        return rootMap;
    }

    protected static String kebabToCamelCase(String kebabCase) {
        if (!kebabCase.contains("-")) {
            return kebabCase;
        }
        StringBuilder camelCase = new StringBuilder();

        boolean capitalizeNext = false;
        for (char c : kebabCase.toCharArray()) {
            if (c == '-') {
                capitalizeNext = true;
            } else {
                if (capitalizeNext) {
                    camelCase.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    camelCase.append(c);
                }
            }
        }

        return camelCase.toString();
    }

    protected static  Map<String, Object> flatten(Map<String, Object> nestedMap, String prefix) {
        Map<String, Object> flatMap = new HashMap<>();
        flatten(nestedMap, prefix,flatMap);
        return flatMap;
    }

    private static void flatten(Map<String, Object> nestedMap, String prefix, Map<String, Object> flatMap) {

        for (Map.Entry<String, Object> entry : nestedMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            String newKey = prefix.isEmpty() ? key : prefix + "." + key;

            if (value instanceof Map<?, ?>) {
                flatten((Map<String, Object>) value, newKey, flatMap);
            } else {
                if(value instanceof OriginTrackedValue originTrackedValue){
                    flatMap.put(newKey, originTrackedValue.getValue());
                }else{
                    flatMap.put(newKey, value);
                }

            }
        }
    }


    protected static <T> T extractDataFromMap(final String key,
                                     final Map<String, ?> data) {
        if (!data.containsKey(key)) {
            throw new RuntimeException(String.format("Could not load data. Expecting key : %s", key));
        }
        return (T) data.get(key);
    }

    protected static void mergeMissingKeys(Map<String, Object> defaultMap, Map<String, Object> customMap) {
        for (Map.Entry<String, Object> entry : defaultMap.entrySet()) {
            String key = entry.getKey();
            Object defaultValue = entry.getValue();

            // If the key is not present in the custom map, set it
            if (!customMap.containsKey(key)) {
                customMap.put(key, defaultValue);
            } else {
                // If the value is a nested map, recurse into it
                if (defaultValue instanceof Map<?, ?> && customMap.get(key) instanceof Map<?, ?>) {
                    mergeMissingKeys((Map<String, Object>) defaultValue, (Map<String, Object>) customMap.get(key));
                }
            }
        }
    }
}
