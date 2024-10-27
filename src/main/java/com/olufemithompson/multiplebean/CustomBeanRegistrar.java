package com.olufemithompson.multiplebean;

import org.reflections.Reflections;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Configuration
public class CustomBeanRegistrar implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware{

    private Map<String, Object> allProperties = new HashMap<>();
    private  ApplicationContext context;
    private  ConfigurableEnvironment environment;

//    private Map<String, ServiceConfig> serviceConfigMap;

//    public Map<String, ServiceConfig> getServiceConfigMap() {
//        return serviceConfigMap;
//    }
//
//    public void setServiceConfigMap(
//            Map<String, ServiceConfig> serviceConfigMap) {
//        this.serviceConfigMap = serviceConfigMap;
//    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
        this.environment = (ConfigurableEnvironment) applicationContext.getEnvironment();
    }


    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        getAllProperties();

//        serviceConfigMap.forEach((key, value)->System.out.println(key));

        // Use Reflections to find classes annotated with @MultipleBean
        Reflections reflections = new Reflections("com.olufemithompson");
        Set<Class<?>> multipleBeans = reflections.getTypesAnnotatedWith(MultipleBean.class);

        for (Class<?> beanClass : multipleBeans) {
            String beanName = beanClass.getSimpleName();

            // Register the main bean
            registerBeanDefinition(registry, beanClass, beanName);

            processDependencies(registry, beanClass, beanName);
        }
    }

    private void getAllProperties(){

        MutablePropertySources propertySources = environment.getPropertySources();
        for (PropertySource<?> propertySource : propertySources) {
            if (propertySource instanceof MapPropertySource) {
                Map<String, Object> properties = ((MapPropertySource) propertySource).getSource();
                properties.forEach((key, value) ->  allProperties.put(key, value));
            }
        }
    }

    private void registerBeanDefinition(BeanDefinitionRegistry registry, Class<?> beanClass, String beanName) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(beanClass);
        registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
    }

    private void processDependencies(BeanDefinitionRegistry registry, Class<?> beanClass, String beanName) {
        Constructor<?>[] constructors = beanClass.getDeclaredConstructors();
        if (constructors.length > 0) {
            Constructor<?> constructor = constructors[0];
            Parameter[] parameters = constructor.getParameters();

            for (Parameter parameter : parameters) {
                Class<?> paramType = parameter.getType();

                // Check if the parameter class has @ConfigurationProperties or @Configuration
                if (paramType.isAnnotationPresent(ConfigurationProperties.class) ||
                        paramType.isAnnotationPresent(Configuration.class)) {

                    String prefix = "multiple." + beanName + ".config." + paramType.getSimpleName().toLowerCase();
                    if (allProperties.containsKey(prefix)) {
                        // Create a new instance of the configuration class
                        try {
                            Object configInstance = createConfigInstance(paramType, prefix);
                            // Register the configuration instance as a bean
                            String configBeanName = paramType.getSimpleName() + beanName;
                            BeanDefinitionBuilder configBuilder = BeanDefinitionBuilder.genericBeanDefinition(paramType)
                                    .addConstructorArgValue(configInstance);
                            registry.registerBeanDefinition(configBeanName, configBuilder.getBeanDefinition());
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to create config instance for " + paramType.getName(), e);
                        }
                    }
                }
            }
        }
    }

    private Object createConfigInstance(Class<?> configClass, String prefix) throws Exception {
        // Use reflection to create a new instance and populate its fields based on the prefix
        Object instance = configClass.getDeclaredConstructor().newInstance();

        // Bind properties (you can enhance this method to handle specific field types)
        MutablePropertySources propertySources = environment.getPropertySources();
        for (PropertySource<?> propertySource : propertySources) {
            if (propertySource instanceof MapPropertySource) {
                Map<String, Object> properties = ((MapPropertySource) propertySource).getSource();
                properties.entrySet().stream()
                        .filter(entry -> entry.getKey().startsWith(prefix + "."))
                        .forEach(entry -> {
                            String propertyName = entry.getKey().substring(prefix.length() + 1);
                            // Set the property on the instance using reflection (you can improve this logic)
                            try {
                                configClass.getMethod("set" + capitalize(propertyName), entry.getValue().getClass())
                                        .invoke(instance, entry.getValue());
                            } catch (Exception e) {
                                throw new RuntimeException("Failed to set property: " + propertyName, e);
                            }
                        });
            }
        }

        return instance;
    }

    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }


    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // Not needed in this configuration, but available for further customization if needed
    }


}

