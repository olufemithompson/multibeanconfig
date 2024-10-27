package com.olufemithompson.multiplebean;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

public class MultipleBeanFactory<T> implements FactoryBean<T> {

    private final Class<T> beanClass;
    private final ApplicationContext context;
    private final Environment environment;

    public MultipleBeanFactory(Class<T> beanClass, ApplicationContext context, Environment environment) {
        this.beanClass = beanClass;
        this.context = context;
        this.environment = environment;
    }

    @Override
    public T getObject() throws Exception {
        // Create an instance of the bean
        T instance = BeanUtils.instantiateClass(beanClass);

        // Inject dependencies dynamically based on fields and constructors
        injectDependencies(instance);

        // Configure the instance based on environment properties if needed
        configureInstance(instance);

        return instance;
    }

    @Override
    public Class<?> getObjectType() {
        return beanClass;
    }

    private void injectDependencies(T instance) {
        // Use Spring's AutowireCapableBeanFactory to inject dependencies
        context.getAutowireCapableBeanFactory().autowireBean(instance);
    }

    private void configureInstance(T instance) {
        // Load configuration from properties, e.g., environment.getProperty()
        // and apply it to the instance fields as needed
    }
}
