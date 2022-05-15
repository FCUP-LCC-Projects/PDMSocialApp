package com.example.socialapp;

import static com.example.socialapp.R.string.main_page;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.socialapp.databinding.ActivityTimeLineBinding;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Comment;
import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;


public class TimeLineActivity extends AppCompatActivity implements PostFragment.OnPostCreatedListener, NavigationView.OnNavigationItemSelectedListener, CommentFragment.onCommentPostedListener {
    private Context context;
    private ActivityTimeLineBinding binding;
    private RecyclerView timeLine;
    private PostViewAdapter postViewAdapter;
    private FirebaseAuth mAuth;
    public String currentUsername;
    private NavigationView navigationView;
    private DrawerLayout drawerLayout;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_time_line);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(main_page);

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


    private void init(){
        drawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.open_drawer, R.string.close_drawer);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();


        navigationView = (NavigationView) findViewById(R.id.timeline_navigation_drawer);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setItemIconTintList(null);
        try{
            TextView usernameInView = findViewById(R.id.drawer_username);
            usernameInView.setText(currentUsername);
        }catch(Exception e){}

        postViewAdapter = new PostViewAdapter(this, new LinkedList<PostItem>());

        timeLine = findViewById(R.id.timeline_view);
        timeLine.setAdapter(postViewAdapter);
        timeLine.setLayoutManager(new LinearLayoutManager(this));
        mAuth = FirebaseAuth.getInstance();
    }


    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        switch(itemId){
            case R.id.action_search:
                return true;
            case R.id.settings_profile:
                    startActivity(new Intent(TimeLineActivity.this, CreateProfileActivity.class));
                    drawerLayout.closeDrawer(GravityCompat.START);
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
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            case R.id.action_chat:
                checkPermissions();
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if(drawerLayout.isDrawerOpen(GravityCompat.START)){
          drawerLayout.closeDrawer(GravityCompat.START);
        } else{
            super.onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == CommCodes.REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(this, ListDevicesActivity.class);
                startActivity(intent);
            } else {
                new AlertDialog.Builder(context)
                        .setCancelable(false)
                        .setMessage("Access to Location is necessary to use the app")
                        .setPositiveButton("Access",  (dialogInterface, i) -> checkPermissions())
                        .setNegativeButton("Deny", (dialogInterface, i) -> this.finish()).show();
            }
        }
        else{
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void checkPermissions() {

            Intent intent = new Intent(this, ListDevicesActivity.class);
            startActivity(intent);

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
        postViewAdapter.notifyItemInserted(0);
   }

    @Override
    public void onCommentPosted(PostItem oldPost, PostItem newPost) {
        postViewAdapter.getPostList().remove(oldPost);
        postViewAdapter.getPostList().push(newPost);
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }


    public class PostViewHolder extends RecyclerView.ViewHolder{
        LinearLayout container;
        TextView postDate, postUser, postText;
        Button replyButton;
        final PostViewAdapter postViewAdapter;


        public PostViewHolder(@NonNull View itemView, PostViewAdapter postViewAdapter) {
            super(itemView);
            container = itemView.findViewById(R.id.timeline_recycleview_item);
            postDate = itemView.findViewById(R.id.timeline_item_date);
            postUser = itemView.findViewById(R.id.timeline_item_user);
            postText = itemView.findViewById(R.id.timeline_item_text);
            replyButton = itemView.findViewById(R.id.timeline_item_reply);
            this.postViewAdapter = postViewAdapter;
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

            holder.replyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Bundle bundle = new Bundle();
                    bundle.putString("username", currentUsername);
                    bundle.putSerializable("post", current);

                    CommentFragment commentFragment = new CommentFragment();
                    commentFragment.setArguments(bundle);
                    getSupportFragmentManager().beginTransaction()
                            .setReorderingAllowed(true)
                            .add(commentFragment, "ShowPost")
                            .commit();
                }
            });
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