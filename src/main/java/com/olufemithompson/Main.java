package com.olufemithompson;

import com.olufemithompson.app.AppConfig;
import com.olufemithompson.app.ComponentOne;
import com.olufemithompson.app.ComponentTwo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class Main {
    public static void main(final String[] args) {
        ApplicationContext context = SpringApplication.run(Main.class, args);
        String[] names = context.getBeanDefinitionNames();
        ComponentTwo component2 = context.getBean(ComponentTwo.class);
        component2.go();

        ComponentOne component1= context.getBean(ComponentOne.class);
        component1.go();
        System.exit(0);
    }

}

