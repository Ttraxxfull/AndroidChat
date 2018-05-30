package com.example.ttraxxfull.testchat_2.Entities;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.ServerValue;

import java.util.Map;

/**
 * Created by ttraxxfull on 10/02/2018.
 */

public class Message {

    private String uid;
    private String username;
    private String userId;
    private String content;
    private String imgUrl;
    private Long date;

    public Message() {

    }

    public Message(String username, String userId, String content, String imgUrl) {
        this.username = username;
        this.userId = userId;
        this.content = content;
        this.imgUrl = imgUrl;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public Map<String, String> getDate() {
        return ServerValue.TIMESTAMP;
    }

    @Exclude
    public Long getLongDate() {
        return date;
    }

    public void setDate(Long date) {
        this.date = date;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof Message)) return false;

        Message message = (Message) obj;
        return this.getUid().equals(message.getUid());

    }
}
