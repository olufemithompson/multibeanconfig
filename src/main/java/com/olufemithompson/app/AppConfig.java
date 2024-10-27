package com.olufemithompson.app;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@ConfigurationProperties("app")
@Configuration
public class AppConfig {
    private String name;
    private int nice;

    private List<String> books;

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

    public List<String> getBooks() {
        return books;
    }

    public void setBooks(List<String> books) {
        this.books = books;
    }
}
