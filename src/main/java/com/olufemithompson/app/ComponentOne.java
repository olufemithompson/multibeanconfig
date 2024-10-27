package com.olufemithompson.app;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ComponentOne {
    @Autowired
    private  Service beana;

    public void go(){
        System.out.println("component 1");
        beana.doStuff();
    }

}
