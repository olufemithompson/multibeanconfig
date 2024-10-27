package com.olufemithompson.multiplebean;

import org.springframework.beans.factory.annotation.Value;

@MultipleBean
public class Service {

    private final ComponentOne componentOne;

    private final AppConfig appConfig;

    @Value("${app.name}")
    private String me;

    public Service(
            final ComponentOne componentOne,
            final AppConfig appConfig
    ){
        this.componentOne = componentOne;
        this.appConfig = appConfig;
    }

    public void doStuff(){
        componentOne.go();
//        System.out.println("System "+ appConfig.getName());
        System.out.println("System "+ appConfig.getNice());
    }


}
