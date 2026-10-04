package org.example;

import Crawler.Crawler;

import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        Crawler crawler = new Crawler("http://localhost:8080/");
        List<String> messages = crawler.crawl("/");

        for (String message : messages) {
            System.out.println(message);
        }
    }
}