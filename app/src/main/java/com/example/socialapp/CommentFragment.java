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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;

public class CommentFragment extends DialogFragment {
    private TextView username, date, textpost;
    private EditText editText;
    private Button postComment;
    private PostItem originalPost;
    private LinkedList<PostItem> commentList;
    private CommentViewAdapter commentViewAdapter;
    private RecyclerView recyclerView;
    private String currentUsername;
    private Boolean emptyList = false;


    public interface onCommentPostedListener{
        public void onCommentPosted(PostItem oldPost, PostItem newPost);
    }


    onCommentPostedListener commentPostedListener;

    public static CommentFragment newInstance() {
        return new CommentFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Activity activity = getActivity();
        if(context instanceof onCommentPostedListener)
            commentPostedListener = (onCommentPostedListener) activity;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        currentUsername = getArguments().getString("username");
        originalPost = (PostItem) getArguments().getSerializable("post");
        return inflater.inflate(R.layout.leave_comment_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        editText = getView().findViewById(R.id.comments_edittext);
        username = getView().findViewById(R.id.comments_post_user);
        date = getView().findViewById(R.id.comments_post_date);
        textpost = getView().findViewById(R.id.comments_post_text);


        Log.d("Username from Post", originalPost.getUsername());

        username.setText(originalPost.getUsername());
        date.setText(originalPost.getDate());
        textpost.setText(originalPost.getContents());

        commentList = originalPost.getComments();
        if(commentList.isEmpty()) {
            commentList.add(new PostItem("", "", "")); //makes sure view actually works
            emptyList = true;
        }
        commentViewAdapter = new CommentViewAdapter(getActivity(), commentList);

        recyclerView = getView().findViewById(R.id.comments_listview);
        recyclerView.setAdapter(commentViewAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        postComment = getView().findViewById(R.id.comments_send);
        postComment.setOnClickListener(view1 -> {
            String commentText = editText.getText().toString();
            editText.setText("");
            if (commentText != null) {
                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                Date date = new Date();
                String time = dateFormat.format(date);
                PostItem postItem = new PostItem(currentUsername, time, commentText);

                commentViewAdapter.getPostList().push(postItem);
                if(emptyList) {
                    commentViewAdapter.getPostList().removeLast();
                    emptyList = false;
                }
                commentViewAdapter.notifyDataSetChanged();
            }
        });
    }

    public class CommentViewHolder extends RecyclerView.ViewHolder{
        LinearLayout container;
        TextView commentDate, commentUser, commentText;
        final CommentViewAdapter commentViewAdapter;


        public CommentViewHolder(@NonNull View itemView, CommentViewAdapter commentViewAdapter) {
            super(itemView);
            container = itemView.findViewById(R.id.comments_new_container);
            commentDate = itemView.findViewById(R.id.comments_new_date);
            commentUser = itemView.findViewById(R.id.comments_new_user);
            commentText = itemView.findViewById(R.id.comments_new_text);
            this.commentViewAdapter = commentViewAdapter;
        }

    }

    public class CommentViewAdapter extends RecyclerView.Adapter<CommentViewHolder> {
        private LinkedList<PostItem> commentList;
        private LayoutInflater layoutInflater;

        public CommentViewAdapter(Context context, LinkedList<PostItem> postList) {
            this.commentList = postList;
            layoutInflater = LayoutInflater.from(context);
        }

        @NonNull
        @Override
        public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = layoutInflater.inflate(R.layout.comment_item, parent, false);

            return new CommentViewHolder(itemView, this);
        }


        @Override
        public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {

            PostItem current = commentList.get(position);
            holder.commentUser.setText(current.getUsername());
            holder.commentDate.setText(current.getDate());
            holder.commentText.setText(current.getContents());
        }

        @Override
        public int getItemCount() {
            return commentList.size();
        }

        public LinkedList<PostItem> getPostList() {
            return commentList;
        }
    }
}
