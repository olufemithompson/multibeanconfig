package com.olufemithompson.app;

import com.olufemithompson.multiplebean.MultipleBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.stream.Collectors;

@MultipleBean
public class Service {

    @Autowired
    private  AppConfig appConfig;

    private  final String name;

    public Service(
            final @Value("${app.name}") String name
    ){
        this.name = name;
    }


    public void doStuff(){
        System.out.println("nice :  "+ appConfig.getNice());
        System.out.println("name :  "+ appConfig.getName());
        System.out.println("books :  "+ appConfig.getBooks().stream().collect(Collectors.joining(", ")));
    }

}
