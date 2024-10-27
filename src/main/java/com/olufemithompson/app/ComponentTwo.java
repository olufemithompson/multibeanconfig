package com.olufemithompson.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ComponentTwo {
    @Autowired
    private  Service beanb;

    public void go(){
        System.out.println("component 2");
        beanb.doStuff();
    }
}
