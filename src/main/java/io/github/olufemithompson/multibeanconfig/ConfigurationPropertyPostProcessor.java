package io.github.olufemithompson.multibeanconfig;

import org.springframework.beans.BeansException;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindingPostProcessor;
import org.springframework.stereotype.Component;

/**
 * A {@link ConfigurationPropertiesBindingPostProcessor} to support custom configurations
 * for beans in multi-bean scenarios.
 * <br>
 * <br>
 * This post-processor enables dynamic binding of configuration properties
 * to beans managed by {@link MultiBeanConfigRegistry}. When a custom configuration
 * for a bean exists in the registry, it binds the properties to this custom
 * instance, ensuring that beans with distinct configurations are initialized
 * correctly within a single application context.
 *
 */
@Component
class ConfigurationPropertyPostProcessor
        extends ConfigurationPropertiesBindingPostProcessor{

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Object customBean = MultiBeanConfigRegistry.getConfig(beanName);
        if(customBean != null){
            return super.postProcessAfterInitialization(customBean, beanName);
        }
        return super.postProcessAfterInitialization(bean, beanName);
    }

}

