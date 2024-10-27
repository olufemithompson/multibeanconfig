package com.olufemithompson.multiplebean;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;

@MultipleBean
public class Adapter {

    private final ComponentTwo componentTwo;

    public Adapter(ComponentTwo componentTwo){
        this.componentTwo = componentTwo;
    }

    @Value("${app.nice}")
    private String me;


    @PostConstruct
    public void init() {
        System.out.println("Adapter initialized with me = " + me);
        componentTwo.go();
    }
}
