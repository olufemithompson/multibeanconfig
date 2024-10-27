package com.olufemithompson.multiplebean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ComponentTwo {

    @Autowired
    private  Service service;

    @Value("${app.name}")
    private String me;

    public void go(){
        System.out.println("ComponentTwo initialized with me = " + me);
        service.doStuff();
    }
}
