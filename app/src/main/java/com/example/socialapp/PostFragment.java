package com.example.socialapp;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class PostFragment extends DialogFragment {
    EditText postText;
    Button sendButton;
    private Button backButton;
    private String username;

    public interface OnPostCreatedListener{
        public void onPostCreated(PostItem postItem);
    }

    OnPostCreatedListener postCreatedListener;

    public static PostFragment newInstance(){
        return new PostFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Activity activity = getActivity();
        if(context instanceof OnPostCreatedListener)
            postCreatedListener = (OnPostCreatedListener) activity;

    }



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        username = getArguments().getString("username");
        return inflater.inflate(R.layout.create_post_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        postText = getView().findViewById(R.id.fragment_post_text);
        backButton = getView().findViewById(R.id.fragment_post_back);

        sendButton = getView().findViewById(R.id.fragment_post_send);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                Date date = new Date();
                String time = dateFormat.format(date);
                String contents = postText.getText().toString();

                PostItem postItem = new PostItem(username, time, contents);

                postCreatedListener.onPostCreated(postItem);
                getParentFragmentManager().beginTransaction().remove(PostFragment.this).commit();
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getParentFragmentManager().beginTransaction().remove(PostFragment.this).commit();
            }
        });
    }
}
