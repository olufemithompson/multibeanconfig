package com.olufemithompson.multiplebean;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ComponentOne {

    @Autowired
    private AppConfig appConfig;

    @Value("${app.name}")
    private String me;

    public void go(){
        System.out.println("ComponentOne initialized with me = " + me);
    }

}
