package com.example.socialapp;

import static com.example.socialapp.R.string.main_page;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;


import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.socialapp.databinding.ActivityTimeLineBinding;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;


import java.sql.Time;
import java.util.LinkedList;


public class TimeLineActivity extends AppCompatActivity implements PostFragment.OnPostCreatedListener, NavigationView.OnNavigationItemSelectedListener, CommentFragment.onCommentPostedListener {
    private Context context;
    private RecyclerView timeLine;
    private PostViewAdapter postViewAdapter;
    private FirebaseAuth mAuth;
    public String currentUsername;
    private NavigationView navigationView;
    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_time_line);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(main_page);

        sharedPreferences = getSharedPreferences("myPref", MODE_PRIVATE);

        boolean nightMode = sharedPreferences.getBoolean(CommCodes.KEY_PREF_MODE, false);
        if (nightMode)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        else {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
            else
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
        
        currentUsername = sharedPreferences.getString(CommCodes.KEY_PREF_USER, "");
        Log.d("Username", "Current username is: "+currentUsername);
        init();

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean timelineToggle = sharedPreferences.getBoolean(CommCodes.KEY_PREF_TIMELINE, false);
        if(!timelineToggle) {
            IOUtils.writeFiletToIStorage(this, postViewAdapter.getPostList());
            Log.d("IO", "Call to write files to storage");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        boolean timelineToggle = sharedPreferences.getBoolean(CommCodes.KEY_PREF_TIMELINE, false);
        if(!timelineToggle) {
            IOUtils.writeFiletToIStorage(this, postViewAdapter.getPostList());
            Log.d("IO", "Call to write files to storage");
        }
    }

    @SuppressLint("NewApi")
    private void init(){
        drawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.open_drawer, R.string.close_drawer);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();


        navigationView = findViewById(R.id.timeline_navigation_drawer);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setItemIconTintList(null);
        View headerView = navigationView.getHeaderView(0);
        try{
            TextView usernameInView = (TextView) headerView.findViewById(R.id.drawer_username);
            usernameInView.setText(currentUsername);
        }catch(Exception e){}

        boolean timelineToggle = sharedPreferences.getBoolean(CommCodes.KEY_PREF_TIMELINE, false);
        LinkedList<PostItem> postList = null;
        if(!timelineToggle) {
            postList = IOUtils.readFileFromIStorage(this);
        }
        if(postList == null) postList = new LinkedList<>();
        postViewAdapter = new PostViewAdapter(this, postList);

        timeLine = findViewById(R.id.timeline_view);
        timeLine.setAdapter(postViewAdapter);
        timeLine.setLayoutManager(new LinearLayoutManager(this));
        mAuth = FirebaseAuth.getInstance();
    }


    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        switch(itemId){
            case R.id.action_search:
                searchPost();
                return true;
            case R.id.settings_profile:
                    startActivity(new Intent(TimeLineActivity.this, CreateProfileActivity.class));
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
            case R.id.settings_manage:
                    getSupportFragmentManager().beginTransaction().setReorderingAllowed(true)
                            .add(R.id.timeline_fragment_container,
                                    SettingsFragment.newInstance(), "Settings")
                            .addToBackStack("Settings")
                            .commit();
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
            case R.id.settings_password:
                    getSupportFragmentManager().beginTransaction().setReorderingAllowed(true)
                            .add(R.id.timeline_fragment_container,
                                    PasswordFragment.newInstance(), "Password")
                            .addToBackStack("Password")
                            .commit();
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
            case R.id.settings_logout:
                    mAuth.signOut();
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

    private void searchPost() {
        startActivity(new Intent(TimeLineActivity.this, PostCommActivity.class));
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
                Intent intent = new Intent(this, ChatActivity.class);
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
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(TimeLineActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, CommCodes.REQUEST_LOCATION_PERMISSION);
        } else {
            Intent intent = new Intent(this, ChatActivity.class);
            startActivity(intent);
        }
    }

    private void createPostFragment(){
        Bundle bundle = new Bundle();
        bundle.putString("username", currentUsername);
        PostFragment postFragment = PostFragment.newInstance();
        postFragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .add(postFragment, "CreatePost")
                .commit();

    }

    @Override
    public void onPostCreated(PostItem postItem) {
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
        private final LinkedList<PostItem> postList;
        private final LayoutInflater layoutInflater;

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

            holder.replyButton.setOnClickListener(view -> {
                Bundle bundle = new Bundle();
                bundle.putString("username", currentUsername);
                bundle.putSerializable("post", current);

                CommentFragment commentFragment = CommentFragment.newInstance();
                commentFragment.setArguments(bundle);
                getSupportFragmentManager().beginTransaction()
                        .setReorderingAllowed(true)
                        .add(commentFragment, "ShowPost")
                        .commit();
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

