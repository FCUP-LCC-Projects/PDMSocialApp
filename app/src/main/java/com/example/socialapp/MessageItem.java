package com.example.socialapp;

import android.widget.ImageView;

import java.io.Serializable;

public class MessageItem implements Serializable {
    private String username, date, message;
    private ImageView image;

    public MessageItem(String username, String message){
        this.username = username;

        this.message = message;
    }


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }


    public ImageView getImage() {
        return image;
    }

    public void setImage(ImageView image) {
        this.image = image;
    }
}
