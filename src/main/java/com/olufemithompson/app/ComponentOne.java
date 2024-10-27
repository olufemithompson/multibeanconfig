package com.olufemithompson.app;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ComponentOne {
    @Autowired
    private HttpClientService defaultClient;

    @Autowired
    private HttpClientService failOverClient;

    public void go(){
        defaultClient.doStuff();
        failOverClient.doStuff();
    }

}
