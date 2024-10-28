package io.github.olufemithompson.multibeanconfig;


import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindingPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MultiBeanConfig {
    @Bean
    public BeanDefinitionRegistryPostProcessor multiBeanDefinitionPostProcessor() {
        return new MultiBeanDefinitionPostProcessor();
    }

    @Bean
    public BeanPostProcessor beanPropertyPostProcessor() {
        return new BeanPropertyPostProcessor();
    }
    @Bean
    public ConfigurationPropertiesBindingPostProcessor configurationPropertyPostProcessor() {
        return new ConfigurationPropertyPostProcessor();
    }
}
