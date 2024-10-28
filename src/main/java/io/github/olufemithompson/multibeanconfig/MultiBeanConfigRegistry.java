package io.github.olufemithompson.multibeanconfig;

import java.util.HashMap;
import java.util.Map;

final class MultiBeanConfigRegistry {
    private static Map<String, Object> configRegistry = new HashMap<>();
    private static Map<String, Object> valueRegistry = new HashMap<>();
    private static Map<String, String> beanConfigReferenceRegistry = new HashMap<>();


    public static void registerConfig(String name, Object config){
        configRegistry.put(name, config);
    }
    public static Object getConfig(String name){
        return configRegistry.get(name);
    }

    public static void registerValue(String name, Object config){
        valueRegistry.put(name, config);
    }

    public static Object getValue(String name){
        return valueRegistry.get(name);
    }


    public static void registerBeanConfigReference(String name, String reference){
        beanConfigReferenceRegistry.put(name, reference);
    }

    public static String getBeanConfigReference(String name){
        return beanConfigReferenceRegistry.get(name);
    }
}
