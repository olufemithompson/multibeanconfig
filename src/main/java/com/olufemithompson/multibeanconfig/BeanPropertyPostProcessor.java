package com.olufemithompson.multibeanconfig;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

@Component
public class BeanPropertyPostProcessor implements BeanPostProcessor, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class<?> clazz = bean.getClass();
        if (clazz.isAnnotationPresent(MultipleBean.class)) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Value.class)) {
                    injectValueField(bean, field, beanName);
                }

                if (field.isAnnotationPresent(Autowired.class)) {
                    injectAutowiredField(bean, field, beanName);
                }
            }
        }
        return bean;
    }

    private void injectValueField(Object bean, Field field, String beanName) {
        Object customValue = MultipleBeanConfigRegistry.getValue(
                beanName+field.getName()
        );
        if(customValue != null){
            try {
                Object convertedValue = ((OriginTrackedValue)customValue).getValue();
                field.setAccessible(true);
                field.set(bean, convertedValue);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to inject property: " + field.getName(), e);
            }
        }
    }

    private void injectAutowiredField(Object bean, Field field, String beanName) {
        String propertyReference = MultipleBeanConfigRegistry.getBeanConfigReference(
                beanName+field.getName()
        );
        if(propertyReference != null){
            Object property = this.applicationContext.getBean(
                    propertyReference, field.getType()
            );
            if(property != null){
                try {
                    field.setAccessible(true);
                    field.set(bean, property);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to inject property: " + field.getName(), e);
                }
            }
        }

    }


}
