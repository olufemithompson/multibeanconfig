package io.github.olufemithompson.multibeanconfig;

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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static io.github.olufemithompson.multibeanconfig.Constants.PARENT_CONFIG_NAME;
import static io.github.olufemithompson.multibeanconfig.Utils.extractDataFromMap;
import static io.github.olufemithompson.multibeanconfig.Utils.flatten;
import static io.github.olufemithompson.multibeanconfig.Utils.formatConfigProperties;
import static io.github.olufemithompson.multibeanconfig.Utils.generateNestedMap;
import static io.github.olufemithompson.multibeanconfig.Utils.getFirstKey;
import static io.github.olufemithompson.multibeanconfig.Utils.kebabToCamelCase;
import static io.github.olufemithompson.multibeanconfig.Utils.mergeMissingKeys;

@Configuration
public class MultipleBeanDefinitionPostProcessor implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware{
    private Map<String, Object> applicationProperties = new HashMap<>();
    Map<String, Object> flattenedProperties = new HashMap<>();

    private  ConfigurableEnvironment environment;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.environment = (ConfigurableEnvironment) applicationContext.getEnvironment();
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {

        parseProperties();
        Map<String, Object> multipleBeanProperties = extractDataFromMap(PARENT_CONFIG_NAME, applicationProperties);
        Set<String> beanNames  = multipleBeanProperties.keySet();


        Reflections reflections = new Reflections( new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forJavaClassPath()));

        Set<Class<?>> multipleBeans = reflections.getTypesAnnotatedWith(MultipleBean.class);

        for(String beanName: beanNames){
            Map<String, Object> beanConfig = extractDataFromMap(beanName, multipleBeanProperties);

            if(!beanConfig.containsKey("class")){
                throw new RuntimeException(String.format("Declared bean : %s should have a class property", beanName));
            }
            String configuredBeanClassName = beanConfig.get("class").toString();
            boolean foundClass = false;
            for (Class<?> multipleBeanClass : multipleBeans) {
                String beanClassName = multipleBeanClass.getSimpleName();
                if(configuredBeanClassName.equals(beanClassName)){
                    foundClass = true;

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
                if(!foundClass){
                    throw new RuntimeException(String.format("Cannot find Class %s for declared bean %s",configuredBeanClassName, beanName));
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
                Map<String, Object> defaultConfig = extractDataFromMap(key, applicationProperties);
                if(beanConfig.containsKey(key)){

                    Map<String, Object> beanAppConfig = extractDataFromMap(key, beanConfig);
                    mergeMissingKeys(defaultConfig, beanAppConfig);
                    beanConfig.put(key, beanAppConfig);

                    Map<String, Object> propertySourceMap  = flatten(Map.of(beanName, beanConfig),"multiple");
                    MapPropertySource propertySource = new MapPropertySource("custom", propertySourceMap);
                    environment.getPropertySources().addFirst(propertySource);

                    String customPrefix = PARENT_CONFIG_NAME+"."+beanName+"."+originalPrefix;
                    Object customConfigInstance = Binder.get(environment)
                            .bind(customPrefix, Bindable.of(configClass))
                            .orElseThrow(() -> new RuntimeException("Unable to bind properties for custom config"));

                    String formattedBeanName = kebabToCamelCase(beanName);

                    String customBeanName = formattedBeanName + configClass.getSimpleName();
                    registerBeanDefinition(registry, configClass, customBeanName, Map.of());

                    MultipleBeanConfigRegistry.registerConfig(customBeanName, customConfigInstance);
                    MultipleBeanConfigRegistry.registerBeanConfigReference(formattedBeanName+field.getName(), customBeanName);
                    customConfigBeans.put(configClass.getSimpleName(), customBeanName);
                }
            }
        }
        return customConfigBeans;
    }

    private void registerValueAnnotatedFieldDependencies(
            String beanName,
            Class<?> multipleBeanClass){
        String formattedBeanName = kebabToCamelCase(beanName);
        for (Field field : multipleBeanClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Value.class)) {
                field.setAccessible(true);
                String propertyKey = formatConfigProperties(field.getAnnotation(Value.class).value());
                String multipleBeanPropertyKey = PARENT_CONFIG_NAME+"."+beanName+"."+propertyKey;
                if(flattenedProperties.containsKey(multipleBeanPropertyKey)){
                    MultipleBeanConfigRegistry.registerValue(
                            formattedBeanName+field.getName(),
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
        registry.registerBeanDefinition(kebabToCamelCase(beanName), builder.getBeanDefinition());
    }


    private void parseProperties(){
        MutablePropertySources propertySources = environment.getPropertySources();
        for (PropertySource<?> propertySource : propertySources) {
            if (propertySource instanceof MapPropertySource) {
                Map<String, Object> properties = ((MapPropertySource) propertySource).getSource();

                properties.forEach((key, value) ->  {
                    flattenedProperties.put(key, value);
                });
            }
        }
        applicationProperties = generateNestedMap(flattenedProperties);

        System.out.println(flattenedProperties);
        System.out.println(applicationProperties);
    }

}

