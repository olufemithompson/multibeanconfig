package io.github.olufemithompson.multibeanconfig;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * A {@link BeanPostProcessor} that injects specific field values and dependencies
 * for beans annotated with  {@link MultiBean}, supporting multi-bean configurations
 * within a Spring application context.
 * <br>
 * <br>
 * This class processes fields annotated with {@link Value} and {@link Autowired} within
 * beans marked with {@link MultiBean}, enabling property and dependency injection
 * based on custom configurations stored in {@link MultiBeanConfigRegistry}.
 * <br>
 * <br>
 * Key responsibilities:
 * <br>
 * <ul>
 *  <li>
 *      Injecting custom values into fields annotated with {@link Value} by fetching
 *      property values stored in {@link MultiBeanConfigRegistry}.
 *  </li>
 *  <li>
 *      Injecting dependencies into fields annotated with {@link Autowired} by referencing
 *      bean instances registered in the application context, enabling each bean
 *      to access its configured dependencies.
 *   </li>
 * </ul>
 *
 */
@Component
class BeanPropertyPostProcessor implements BeanPostProcessor, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class<?> clazz = bean.getClass();
        if (clazz.isAnnotationPresent(MultiBean.class)) {
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
        Object customValue = MultiBeanConfigRegistry.getValue(
                beanName+field.getName()
        );
        if(customValue != null){
            try {
                Object convertedValue = ((OriginTrackedValue)customValue).getValue();
                field.setAccessible(true);
                field.set(bean, convertedValue);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to inject property with @Value annotation: " + field.getName(), e);
            }
        }
    }

    private void injectAutowiredField(Object bean, Field field, String beanName) {
        String propertyReference = MultiBeanConfigRegistry.getBeanConfigReference(
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
                    throw new RuntimeException("Failed to inject property with @Autowired annotation: " + field.getName(), e);
                }
            }
        }

    }


}
