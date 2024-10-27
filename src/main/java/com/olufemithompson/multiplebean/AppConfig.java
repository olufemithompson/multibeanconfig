package com.olufemithompson.multiplebean;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties("app")
@Configuration
public class AppConfig {
    private String name;
    private int nice;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNice() {
        return nice;
    }

    public void setNice(int nice) {
        this.nice = nice;
    }
}
