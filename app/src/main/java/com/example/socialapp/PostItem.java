package com.example.socialapp;

import androidx.annotation.Nullable;

import java.io.Serializable;
import java.sql.Date;
import java.util.LinkedList;

public class PostItem implements Serializable {
    private String username, date, contents;
    private LinkedList<PostItem> comments;

    public PostItem(String username, String date, String contents) {
        this.username = username;
        this.date = date;
        this.contents = contents;
        comments = new LinkedList<>();
    }

    public String getContents() {
        return contents;
    }

    public void setContents(String contents) {
        this.contents = contents;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LinkedList<PostItem> getComments() {
        return comments;
    }

    public void setComments(LinkedList<PostItem> comments) {
        this.comments = comments;
    }

    public boolean equals(@Nullable PostItem obj) {
        super.equals(obj);
        if(this.username.equals(obj.getUsername())) return date.equals(obj.getDate());
        return false;
    }
}
