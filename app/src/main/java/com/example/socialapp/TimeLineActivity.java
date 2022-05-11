package com.example.socialapp;

import static com.example.socialapp.R.string.main_page;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.socialapp.databinding.ActivityTimeLineBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;

public class TimeLineActivity extends AppCompatActivity implements PostFragment.OnPostCreatedListener {
    private Context context;
    private ActivityTimeLineBinding binding;
    private RecyclerView timeLine;
    private PostViewAdapter postViewAdapter;
    private FirebaseAuth mAuth;
    public String currentUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;

        binding = ActivityTimeLineBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        TimeLineActivity.this.setTitle(main_page);

        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        CollapsingToolbarLayout toolBarLayout = binding.toolbarLayout;
        toolBarLayout.setTitle(getTitle());

        init();

    }

    @Override
    protected void onStart() {
        super.onStart();
        ListDevicesActivity.getUsername().addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                currentUsername = task.getResult();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_time_line, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void init(){
        mAuth = FirebaseAuth.getInstance();
        LinkedList<PostItem> tempList = new LinkedList<>();

        tempList.add(new PostItem("Me", "Right Now", "This is a long text\n" +
                "    with multiple lines to test if this works\n" +
                "    with long texts like this "));
        //postViewAdapter = new PostViewAdapter(this, new LinkedList<PostItem>());
        postViewAdapter = new PostViewAdapter(this, tempList);

        timeLine = findViewById(R.id.timeline_view);
        timeLine.setAdapter(postViewAdapter);
        timeLine.setLayoutManager(new LinearLayoutManager(this));

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        switch(itemId){
            case R.id.action_search:
                return true;
            case R.id.settings_profile:
                    startActivity(new Intent(TimeLineActivity.this, CreateProfileActivity.class));
                    return true;
            case R.id.settings_manage:
                    return true;
            case R.id.settings_logout:
                    mAuth.signOut();
                    finish();
                    startActivity(new Intent(TimeLineActivity.this, SignInActivity.class));
                    return true;
            case R.id.action_post:
                createPostFragment();
                return true;
            case R.id.action_chat:
                startActivity(new Intent(TimeLineActivity.this, ChatActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void createPostFragment(){
        Bundle bundle = new Bundle();
        bundle.putString("username", currentUsername);
        PostFragment postFragment = new PostFragment();
        postFragment.setArguments(bundle);
        postFragment.show(getSupportFragmentManager(), "CreatePost");

    }

    @Override
    public void onPostCreated(PostItem postItem) {
        PostFragment postFragment = (PostFragment) getSupportFragmentManager().findFragmentById(R.id.post_fragment);

        postViewAdapter.getPostList().push(postItem);
        postViewAdapter.notifyItemInserted(postViewAdapter.getItemCount()-1);
        Log.d("View Adapter Count", String.valueOf(postViewAdapter.getItemCount()));

    }


    public class PostViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        LinearLayout container;
        TextView postDate, postUser, postText;
        final PostViewAdapter postViewAdapter;


        public PostViewHolder(@NonNull View itemView, PostViewAdapter postViewAdapter) {
            super(itemView);
            container = itemView.findViewById(R.id.timeline_recycleview_item);
            postDate = itemView.findViewById(R.id.timeline_item_date);
            postUser = itemView.findViewById(R.id.timeline_item_user);
            postText = itemView.findViewById(R.id.timeline_item_text);
            this.postViewAdapter = postViewAdapter;
        }

        @Override
        public void onClick(View view) {
            //POR FAZER ON CLICK NOS POSTS;
        }
    }

    public class PostViewAdapter extends RecyclerView.Adapter<PostViewHolder> {
        private LinkedList<PostItem> postList;
        private LayoutInflater layoutInflater;

        public PostViewAdapter(Context context, LinkedList<PostItem> postList) {
            this.postList = postList;
            layoutInflater = LayoutInflater.from(context);
        }

        @NonNull
        @Override
        public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = layoutInflater.inflate(R.layout.timeline_item, parent, false);

            return new PostViewHolder(itemView, this);
        }


        @Override
        public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {

            PostItem current = postList.get(position);
            holder.postUser.setText(current.getUsername());
            holder.postDate.setText(current.getDate());
            holder.postText.setText(current.getContents());
        }

        @Override
        public int getItemCount() {
            return postList.size();
        }

        public LinkedList<PostItem> getPostList() {
            return postList;
        }
    }
}