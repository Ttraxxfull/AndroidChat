package com.example.ttraxxfull.testchat_2.Entities;

/**
 * Created by ttraxxfull on 09/02/2018.
 */

public class User {
    private String username;
    private String id;

    public User() {

    }

    public User(String username, String id) {
        this.username = username;
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public String getId() {
        return id;
    }
}
