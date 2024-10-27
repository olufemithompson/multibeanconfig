package com.olufemithompson.multiplebean;

import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.validation.BindingResult;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.olufemithompson.multiplebean.Constants.PARENT_CONFIG_NAME;
import static com.olufemithompson.multiplebean.Utils.extractDataFromMap;
import static com.olufemithompson.multiplebean.Utils.formatConfigProperties;
import static com.olufemithompson.multiplebean.Utils.generateNestedMap;
import static com.olufemithompson.multiplebean.Utils.getFirstKey;

@Configuration
public class MultipleBeanDefinitionPostProcessor implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware{
    private Map<String, Object> multipleBeanProperties = new HashMap<>();
    Map<String, Object> flattenedProperties = new HashMap<>();

    private  ConfigurableEnvironment environment;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.environment = (ConfigurableEnvironment) applicationContext.getEnvironment();
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {

        parseProperties();
        Set<String> beanNames  = multipleBeanProperties.keySet();


        Reflections reflections = new Reflections( new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forJavaClassPath()));

        Set<Class<?>> multipleBeans = reflections.getTypesAnnotatedWith(MultipleBean.class);


        for (Class<?> multipleBeanClass : multipleBeans) {

            for(String beanName: beanNames){
                Map<String, Object> beanConfig = extractDataFromMap(beanName, multipleBeanProperties);
                String beanClassName = multipleBeanClass.getSimpleName();
                if(!beanConfig.containsKey("class")){
                    throw new RuntimeException(String.format("Declared bean : %s should have a class property", beanName));
                }

                String configuredBeanClassName = beanConfig.get("class").toString();
                if(configuredBeanClassName.equals(beanClassName)){
                    Map<String, String> configDependencies = registerConfigDependencies(
                            beanName,
                            multipleBeanClass,
                            beanConfig,
                            registry
                    );

                    registerValueAnnotatedFieldDependencies(beanName, multipleBeanClass);

                    // Register the main bean
                    registerBeanDefinition(
                            registry,
                            multipleBeanClass,
                            beanName,
                            configDependencies
                            );

                }
            }
        }
    }



    private Map<String, String> registerConfigDependencies(
            String beanName,
            Class<?> multipleBeanClass,
            Map<String, Object> beanConfig,
            BeanDefinitionRegistry registry){
        Map<String, String> customConfigBeans = new HashMap<>();
        for (Field field : multipleBeanClass.getDeclaredFields()) {
            Class<?> configClass = field.getType();
            ConfigurationProperties annotation = configClass.getAnnotation(ConfigurationProperties.class);
            if (annotation != null) {
                String originalPrefix = annotation.value();
                String key = getFirstKey(originalPrefix);
                if(beanConfig.containsKey(key)){
                    String customPrefix = PARENT_CONFIG_NAME+"."+beanName+"."+originalPrefix;

                    Object customConfigInstance = Binder.get(environment)
                            .bind(customPrefix, Bindable.of(configClass))
                            .orElseThrow(() -> new RuntimeException("Unable to bind properties for custom config"));

                    String customBeanName = beanName + configClass.getSimpleName();
                    registerBeanDefinition(registry, configClass, customBeanName, Map.of());

                    MultipleBeanConfigRegistry.registerConfig(customBeanName, customConfigInstance);
                    MultipleBeanConfigRegistry.registerBeanConfigReference(beanName+field.getName(), customBeanName);
                    customConfigBeans.put(configClass.getSimpleName(), customBeanName);
                }
            }
        }
        return customConfigBeans;
    }

    private <T> BindResult<T> bindConfig(String prefix, Class<T> configClass) {
        return Binder.get(environment)
                .bind(prefix, Bindable.of(configClass));
    }

    private void registerValueAnnotatedFieldDependencies(
            String beanName,
            Class<?> multipleBeanClass){
        for (Field field : multipleBeanClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Value.class)) {
                field.setAccessible(true);
                String propertyKey = formatConfigProperties(field.getAnnotation(Value.class).value());
                String multipleBeanPropertyKey = PARENT_CONFIG_NAME+"."+beanName+"."+propertyKey;
                if(flattenedProperties.containsKey(multipleBeanPropertyKey)){
                    MultipleBeanConfigRegistry.registerValue(
                            beanName+field.getName(),
                            flattenedProperties.get(multipleBeanPropertyKey)
                    );
                }
            }
        }
    }



    private void registerBeanDefinition(BeanDefinitionRegistry registry,
                                        Class<?> multipleBeanClass,
                                        String beanName,
                                        Map<String, String> configBeanReference
                                        ) {

        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(multipleBeanClass);
        Constructor<?>[] constructors = multipleBeanClass.getDeclaredConstructors();
        if (constructors.length > 0) {
            for(int i = 0; i < constructors.length; i++){
                Constructor<?> constructor = constructors[i];
                for (int j = 0; j < constructor.getParameterCount(); j++) {
                    Parameter parameter = constructor.getParameters()[j];
                    Class<?> paramType = parameter.getType();
                    Value valueAnnotation = parameter.getAnnotation(Value.class);
                    if (valueAnnotation != null) {
                        String propertyKey = formatConfigProperties(valueAnnotation.value());
                        String multipleBeanPropertyKey = PARENT_CONFIG_NAME+"."+beanName+"."+propertyKey;
                        Object value = flattenedProperties.get(multipleBeanPropertyKey);
                        if(value != null){
                            Object convertedValue = ((OriginTrackedValue)value).getValue();
                            builder.addConstructorArgValue(convertedValue);
                        }
                    } else {
                        Set<String> classNames = configBeanReference.keySet();
                        if(classNames.contains(paramType.getSimpleName())){
                            String paramBeanName = configBeanReference.get(paramType.getSimpleName());
                            RuntimeBeanReference runtimeBeanReference = new RuntimeBeanReference(
                                    paramBeanName
                            );
                            builder.addConstructorArgValue(runtimeBeanReference);
                        }else{
                            builder.addConstructorArgReference(parameter.getName());
                        }
                    }

                }
            }
        }
        registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
    }


    private void parseProperties(){
        MutablePropertySources propertySources = environment.getPropertySources();
        for (PropertySource<?> propertySource : propertySources) {
            if (propertySource instanceof MapPropertySource) {
                Map<String, Object> properties = ((MapPropertySource) propertySource).getSource();

                properties.forEach((key, value) ->  {
                    if(key.startsWith(PARENT_CONFIG_NAME)){
                        flattenedProperties.put(key, value);
                    }
                });
            }
        }
        Map<String, Object> nestedMap = generateNestedMap(flattenedProperties);
        multipleBeanProperties = extractDataFromMap(PARENT_CONFIG_NAME, nestedMap);
    }
}

