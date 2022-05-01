package com.example.socialapp;

import static com.example.socialapp.R.string.main_page;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

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

import java.util.LinkedList;

public class TimeLineActivity extends AppCompatActivity {
    private Context context;
    private ActivityTimeLineBinding binding;
    private LinkedList<LinearLayout> postList;
    private RecyclerView timeLine;
    private PostViewAdapter postViewAdapter;
    private FirebaseAuth mAuth;

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



    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_time_line, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void init(){
        mAuth = FirebaseAuth.getInstance();
        postList = new LinkedList<>();
        postViewAdapter = new PostViewAdapter(this, postList);

        timeLine = findViewById(R.id.timeline_view);
        timeLine.setAdapter(postViewAdapter);
        timeLine.setLayoutManager(new LinearLayoutManager(this));

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        switch(itemId){
            case R.id.settings_profile:
                    startActivity(new Intent(TimeLineActivity.this, CreateProfileActivity.class));
                    return true;
            case R.id.settings_manage:
                    return true;
            case R.id.settings_logout:
                    mAuth.signOut();
                    return true;
            case R.id.action_post: return true;
            case R.id.action_chat:
                startActivity(new Intent(TimeLineActivity.this, ChatActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }



    public class PostViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        public LinearLayout viewPost;
        final PostViewAdapter postViewAdapter;


        public PostViewHolder(@NonNull View itemView, PostViewAdapter postViewAdapter) {
            super(itemView);
            viewPost = itemView.findViewById(R.id.timeline_recycleview_item);
            this.postViewAdapter = postViewAdapter;
        }

        @Override
        public void onClick(View view) {
            //POR FAZER ON CLICK NOS POSTS;
        }
    }

    public class PostViewAdapter extends RecyclerView.Adapter<PostViewHolder> {
        private LinkedList<LinearLayout> postList;
        private LayoutInflater layoutInflater;

        public PostViewAdapter(Context context, LinkedList<LinearLayout> postList) {
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
            LinearLayout current = postList.get(position);
            holder.viewPost = current;
        }

        @Override
        public int getItemCount() {
            return postList.size();
        }

        public LinkedList<LinearLayout> getPostList() {
            return postList;
        }
    }
}