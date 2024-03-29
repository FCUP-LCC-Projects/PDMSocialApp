package com.example.socialapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;


import org.json.simple.JSONArray;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;

public class IOUtils {

    /***************************** IO POSTS ***********************************/

    public static void writeFiletToIStorage(Context context, LinkedList<PostItem> postList){
        File dir = new File(context.getFilesDir(), "data-logs");
        if(!dir.exists())
            dir.mkdir();
        System.out.println(dir.getPath());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM-dd-yyyy");
        String filename = simpleDateFormat.format(new Date());
        File logFile = new File(dir, filename);
        try{
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
        if(!file.exists()){
            if (dir.list()[0] != null) {
                file = new File(dir, dir.list()[0]);
                Log.d("READ_FILE", file.getName());
            }
            else return null;
        }

        JSONParser jsonParser = new JSONParser();
        LinkedList<PostItem> posts = new LinkedList<>();

        try(FileReader fileReader = new FileReader(file)){
            JSONArray postArray = (JSONArray) jsonParser.parse(fileReader);
            posts = parsePostList(postArray);
        }catch(Exception e){
            e.printStackTrace();
        }
        return posts;
    }

    public static String readJSONFromIStorage(Context context) {
        File dir = new File(context.getFilesDir(), "data-logs");
        if (!dir.exists()) return null;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM-dd-yyyy");
        String filename = simpleDateFormat.format(new Date());
        File file = new File(dir, filename);
        if (!file.exists()) {
            if (dir.list()[0] != null) {
                file = new File(dir, dir.list()[0]);
                Log.d("READ_FILE", file.getName());
            }
            else return null;
        }

        JSONParser jsonParser = new JSONParser();
        JSONArray jsonArray = null;

        try (FileReader fileReader = new FileReader(file)) {
            jsonArray = (JSONArray) jsonParser.parse(fileReader);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException exception) {
            exception.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return jsonArray.toJSONString();
    }

    public static LinkedList<PostItem> parsePostList(JSONArray postArray){
        LinkedList<PostItem> posts = new LinkedList<>();
        Iterator iterator = postArray.iterator();
        while (iterator.hasNext()){
            JSONObject postJSON = (JSONObject) iterator.next();
            posts.add(parsePostFromJSON(postJSON));
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

    /****************************** IO CHAT ************************************/

    public static void writeFileToIStorage(Context context, LinkedList<MessageItem> messageList, String device){
        File dir = new File(context.getFilesDir(), "chat-logs");
        if(!dir.exists())
            dir.mkdir();
        Log.d("Path", dir.getPath());
        String filename = device;
        File logFile = new File(dir, filename);
        try{
            FileWriter fileWriter = new FileWriter(logFile);
            fileWriter.append(writeMessageToArray(messageList, fileWriter).toJSONString());
            fileWriter.flush();
            fileWriter.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private static JSONArray writeMessageToArray(LinkedList<MessageItem> messageItems, FileWriter fileWriter){
        JSONArray newMessages = new JSONArray();
        for(MessageItem m : messageItems){
            newMessages.add(writeMessageToJSON(m));
        }
        return newMessages;
    }

    private static JSONObject writeMessageToJSON(MessageItem message){
        JSONObject newMessage = new JSONObject();
        try{
            newMessage.put("username", message.getUsername());
            newMessage.put("message", message.getMessage());
        }catch(Exception e){
            e.printStackTrace();
        }
        return newMessage;
    }

    public static LinkedList<MessageItem> readChatFromIStorage(Context context, String device){
        File dir = new File(context.getFilesDir(), "chat-logs");
        if(!dir.exists()) return null;

        String filename = device;
        File file = new File(dir, filename);
        if(!file.exists()){
            if (dir.list()[0] != null) {
                file = new File(dir, dir.list()[0]);
                Log.d("READ_FILE", file.getName());
            }
            else return null;
        }

        JSONParser jsonParser = new JSONParser();
        LinkedList<MessageItem> messages = new LinkedList<>();

        try(FileReader fileReader = new FileReader(file)){
            JSONArray chatArray = (JSONArray) jsonParser.parse(fileReader);
            Iterator iterator = chatArray.iterator();
            while (iterator.hasNext()){
                JSONObject chatJSON = (JSONObject) iterator.next();
                messages.add(parseChatFromJSON(chatJSON));
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return messages;
    }

    private static MessageItem parseChatFromJSON(JSONObject jsonObject){
        String username, message;

        username = (String) jsonObject.get("username");
        message = (String) jsonObject.get("message");

        MessageItem recoveredMessage = new MessageItem(username, message);
        return recoveredMessage;
    }

}
