package com.olufemithompson;

import com.olufemithompson.multiplebean.ComponentOne;
import com.olufemithompson.multiplebean.Service;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class Main {
    public static void main(final String[] args) {
        ApplicationContext context = SpringApplication.run(Main.class, args);
//        String[] names = context.getBeanDefinitionNames();
//        for(int i = 0; i < names.length; i++){
//            System.out.println(names[i]);
//        }
//        Service service = context.getBean(Service.class);
//        service.doStuff();
        ComponentOne componentOne = context.getBean(ComponentOne.class);
        componentOne.go();
        System.exit(0);
    }

}

