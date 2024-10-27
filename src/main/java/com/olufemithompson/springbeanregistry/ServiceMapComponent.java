/*
 * Copyright 2024 <Your Name or Your Organization>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.olufemithompson.springbeanregistry;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;


import java.util.HashMap;
import java.util.Map;

//@Component
public class ServiceMapComponent implements SmartLifecycle {

    private final Map<Class<?>, Map<String, Object>> serviceMaps = new HashMap<>();
    private final BeanDefinitionRegistry registry;
    private boolean running = false;

    public ServiceMapComponent(BeanDefinitionRegistry registry) {
        this.registry = registry;
    }

    @PostConstruct
    public void init() {
        scanAndRegisterServices();
    }
    @Override
    public void start() {
        running = true;
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return 0; // Change this to set the startup phase if needed
    }

    @Override
    public boolean isAutoStartup() {
        return true; // Indicates if this lifecycle component should start automatically
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    private void scanAndRegisterServices() {
        AnnotationConfigApplicationContext context = (AnnotationConfigApplicationContext) registry;
        String[] beanNames = context.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Object bean = context.getBean(beanName);
            Class<?> beanClass = bean.getClass();

            if (beanClass.isAnnotationPresent(ServiceIdentifier.class)) {
                ServiceIdentifier serviceIdentifier = beanClass.getAnnotation(ServiceIdentifier.class);
                String serviceName = serviceIdentifier.value();

                // Register the abstract class or interface type along with the service name and implementation
                if(beanClass.getSuperclass() != null ){
                    registerService(beanClass.getSuperclass(), serviceName, bean);
                }
                if(beanClass.getInterfaces() != null && beanClass.getInterfaces().length > 0){
                    for(int i = 0; i < beanClass.getInterfaces().length; i++){
                        registerService(beanClass.getInterfaces()[i], serviceName, bean);
                    }
                }
            }
        }
    }

    protected  <T> T getService(Class<T> category, String serviceName) {
        Map<String, Object> serviceMap = this.serviceMaps.get(category);
        if (serviceMap != null && serviceMap.containsKey(serviceName)) {
            return category.cast(serviceMap.get(serviceName));
        }
        return null; // or throw an exception if you prefer
    }

    private void registerService(Class<?> category, String name, Object service) {
        serviceMaps.computeIfAbsent(category, k -> new HashMap<>()).put(name, service);
    }
}

