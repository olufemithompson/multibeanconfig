package io.github.olufemithompson.multibeanconfig;

import org.springframework.beans.BeansException;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindingPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class ConfigurationPropertyPostProcessor
        extends ConfigurationPropertiesBindingPostProcessor{

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Object customBean = MultipleBeanConfigRegistry.getConfig(beanName);
        if(customBean != null){
            return super.postProcessAfterInitialization(customBean, beanName);
        }
        return super.postProcessAfterInitialization(bean, beanName);
    }

}

