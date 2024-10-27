package com.olufemithompson.app;

import com.olufemithompson.multibeanconfig.MultipleBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.stream.Collectors;

@MultipleBean
public class HttpClientService {

    @Autowired
    private HttpConfig httpConfig;

    public void doStuff(){
        System.out.println("client id :  "+ httpConfig.getClientId());
        System.out.println("client secret :  "+ httpConfig.getClientSecret());
        System.out.println("scopes :  "+ httpConfig.getScopes().stream().collect(Collectors.joining(", ")));
    }

}
