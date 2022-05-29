package com.example.socialapp;

import android.annotation.SuppressLint;
import android.content.Context;


import org.json.simple.JSONArray;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;

public class IOUtils {

    public static void writeFiletToIStorage(Context context, LinkedList<PostItem> postList){
        File dir = new File(context.getFilesDir(), "data-logs");
        if(!dir.exists())
            dir.mkdir();
        System.out.println(dir.getPath());

        try{
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM-dd-yyyy");
            String filename = simpleDateFormat.format(new Date());
            File logFile = new File(dir, filename);
            FileWriter fileWriter = new FileWriter(logFile);

            fileWriter.append(writePostToArray(postList, fileWriter).toJSONString());
            fileWriter.flush();
            fileWriter.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private static JSONArray writePostToArray(LinkedList<PostItem> postItems, FileWriter fileWriter){
        JSONArray newPosts = new JSONArray();
        for(PostItem p : postItems){
            newPosts.add(writePostToJSON(p)); //check if doesnt need to be add instead
        }
        return newPosts;
    }

    private static JSONObject writePostToJSON(PostItem post){
        JSONObject newPost = new JSONObject();
        try {
            newPost.put("username", post.getUsername());
            newPost.put("date", post.getDate());
            newPost.put("contents", post.getContents());
            newPost.put("comments", (JSONArray) writeCommentsToJSON(post.getComments()));
        }catch(Exception e){
            e.printStackTrace();
        }

        return newPost;
    }

    private static JSONArray writeCommentsToJSON(LinkedList<PostItem> comments){
        JSONArray commentsArray = new JSONArray();
        for(PostItem c: comments){
            JSONObject commentPost = new JSONObject();
            commentPost.put("username", c.getUsername());
            commentPost.put("date", c.getDate());
            commentPost.put("contents", c.getContents());
            commentsArray.add(commentPost);
        }
        return commentsArray;
    }

    @SuppressLint("NewApi")
    public static LinkedList<PostItem> readFileFromIStorage(Context context){
        File dir = new File(context.getFilesDir(), "data-logs");
        if(!dir.exists()) return null;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM-dd-yyyy");
        String filename = simpleDateFormat.format(new Date());
        File file = new File(dir, filename);
        if(!file.exists()) return null;

        JSONParser jsonParser = new JSONParser();
        LinkedList<PostItem> posts = new LinkedList<>();

        try(FileReader fileReader = new FileReader(file)){
            JSONArray postArray = (JSONArray) jsonParser.parse(fileReader);
            Iterator iterator = postArray.iterator();
            while (iterator.hasNext()){
                JSONObject postJSON = (JSONObject) iterator.next();
                posts.add(parsePostFromJSON(postJSON));
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return posts;
    }

    private static PostItem parsePostFromJSON(JSONObject jsonObject){
        String username, date, contents;
        username = date = contents = "";
        JSONArray commentsJSON = new JSONArray();

        username = (String) jsonObject.get("username");
        date = (String) jsonObject.get("date");
        contents = (String) jsonObject.get("contents");
        commentsJSON = (JSONArray) jsonObject.get("comments");

        PostItem recoveredPost = new PostItem(username,date,contents);
        recoveredPost.setComments(parseCommentsFromJSON(commentsJSON));
        return recoveredPost;
    }

    private static LinkedList<PostItem> parseCommentsFromJSON(JSONArray jsonArray){
        LinkedList<PostItem> comments = new LinkedList<>();

        Iterator iterator = jsonArray.iterator();
        while (iterator.hasNext()){
            JSONObject object = (JSONObject) iterator.next();
            try {
                String username = (String) object.get("username");
                String date = (String) object.get("date");
                String contents = (String) object.get("contents");
                PostItem postItem = new PostItem(username, date, contents);
                comments.add(postItem);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return comments;
    }
}
