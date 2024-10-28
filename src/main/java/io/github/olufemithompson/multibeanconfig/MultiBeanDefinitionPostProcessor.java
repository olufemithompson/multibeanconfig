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

import static io.github.olufemithompson.multibeanconfig.Constants.CLASS_CONFIG_PARAM;
import static io.github.olufemithompson.multibeanconfig.Constants.CUSTOM_CONFIG_PROPERTY_SOURCE;
import static io.github.olufemithompson.multibeanconfig.Constants.PARENT_CONFIG_NAME;
import static io.github.olufemithompson.multibeanconfig.Utils.extractDataFromMap;
import static io.github.olufemithompson.multibeanconfig.Utils.flatten;
import static io.github.olufemithompson.multibeanconfig.Utils.formatConfigProperties;
import static io.github.olufemithompson.multibeanconfig.Utils.generateNestedMap;
import static io.github.olufemithompson.multibeanconfig.Utils.getFirstKey;
import static io.github.olufemithompson.multibeanconfig.Utils.kebabToCamelCase;
import static io.github.olufemithompson.multibeanconfig.Utils.mergeMissingKeys;

/**
 * A {@link BeanDefinitionRegistryPostProcessor} that enables the registration of multiple beans
 * of the same type with distinct configurations in a Spring application context.
 * <br>
 * <br>
 * This class provides a flexible approach to managing multiple instances of the same
 * bean type by allowing unique configurations for each bean instance. It leverages
 * reflection to locate classes annotated with the custom {@link MultiBean} annotation and
 * processes configuration dependencies marked by {@link ConfigurationProperties} and {@link Value}
 * annotations. This allows each bean to be configured with its own specific settings
 * based on property values specified in the application properties.
 *
 * <br>
 * <br>
 *
 * Key features include:
 * <ul>
 *  <li>
 *      Parsing application properties to set up distinct configurations for each bean.
 *  </li>
 *  <li>
 *      Registering dependencies for fields annotated with {@link ConfigurationProperties} and {@link Value}
 *      within each bean class.
 *  </li>
 *  <li>
 *      Ensuring that each configuration dependency is bound to the corresponding properties.
 *  </li>
 * </ul>
 *
 * Ideal for scenarios requiring simultaneous use of multiple beans with different configurations,
 * such as managing separate database connections or APIs within the same application.
 *
 */
@Configuration
class MultiBeanDefinitionPostProcessor implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware{
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



        Reflections reflections = new Reflections( new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forJavaClassPath()));

        Set<Class<?>> multipleBeans = reflections.getTypesAnnotatedWith(MultiBean.class);
        if(multipleBeans.size() > 0){
            Map<String, Object>  multipleBeanProperties = extractDataFromMap(
                    "no 'multibean' section defined in your application properties",
                    PARENT_CONFIG_NAME,
                    applicationProperties);
            Set<String> beanNames  = multipleBeanProperties.keySet();

            for(String beanName: beanNames){
                Map<String, Object> beanConfig = extractDataFromMap(
                        null,
                        beanName,
                        multipleBeanProperties
                );

                if(!beanConfig.containsKey(CLASS_CONFIG_PARAM)){
                    throw new RuntimeException(String.format("Declared bean : %s should have a %s property", CLASS_CONFIG_PARAM, beanName));
                }
                String configuredBeanClassName = beanConfig.get(CLASS_CONFIG_PARAM).toString();
                boolean foundClass = false;
                for (Class<?> multipleBeanClass : multipleBeans) {
                    String beanClassName = multipleBeanClass.getSimpleName();
                    if(configuredBeanClassName.equals(beanClassName)){
                        foundClass = true;

                        // Get dependencies annotated with @ConfigurationProperties
                        Map<String, String> configDependencies = registerConfigDependencies(
                                beanName,
                                multipleBeanClass,
                                beanConfig,
                                registry
                        );

                        //Register properties annotation with @Value
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
                Map<String, Object> defaultConfig = extractDataFromMap(null,key, applicationProperties);
                if(beanConfig.containsKey(key)){

                    Map<String, Object> beanAppConfig = extractDataFromMap(null,key, beanConfig);
                    mergeMissingKeys(defaultConfig, beanAppConfig);
                    beanConfig.put(key, beanAppConfig);

                    Map<String, Object> propertySourceMap  = flatten(Map.of(beanName, beanConfig),PARENT_CONFIG_NAME);
                    MapPropertySource propertySource = new MapPropertySource(CUSTOM_CONFIG_PROPERTY_SOURCE, propertySourceMap);
                    environment.getPropertySources().addFirst(propertySource);

                    String customPrefix = PARENT_CONFIG_NAME+"."+beanName+"."+originalPrefix;
                    Object customConfigInstance = Binder.get(environment)
                            .bind(customPrefix, Bindable.of(configClass))
                            .orElseThrow(() -> new RuntimeException(String.format("Unable to bind properties for %s config", CUSTOM_CONFIG_PROPERTY_SOURCE)));

                    String formattedBeanName = kebabToCamelCase(beanName);

                    String customBeanName = formattedBeanName + configClass.getSimpleName();
                    registerBeanDefinition(registry, configClass, customBeanName, Map.of());

                    MultiBeanConfigRegistry.registerConfig(customBeanName, customConfigInstance);
                    MultiBeanConfigRegistry.registerBeanConfigReference(formattedBeanName+field.getName(), customBeanName);
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
                    MultiBeanConfigRegistry.registerValue(
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
    }
}

