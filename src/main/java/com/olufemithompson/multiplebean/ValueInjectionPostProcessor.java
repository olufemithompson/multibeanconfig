package com.olufemithompson.multiplebean;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

//@Component
public class ValueInjectionPostProcessor  implements BeanPostProcessor {

    @Autowired
    private Environment environment;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

        Class<?> clazz = bean.getClass();
        if (clazz.isAnnotationPresent(MultipleBean.class)) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Value.class)) {

                    Value valueAnnotation = field.getAnnotation(Value.class);
                    String value = environment.getProperty(valueAnnotation.value());
                    try {
                        field.setAccessible(true);
                        field.set(bean, value);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("Failed to inject property: " + field.getName(), e);
                    }
                }
            }

        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}
