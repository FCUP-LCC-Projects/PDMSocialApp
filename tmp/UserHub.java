package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class UserHub extends AppCompatActivity {
    private Button createProfile;
    private FirebaseAuth mAuth;
    private FirebaseDatabase mData;
    private Toolbar toolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_hub);
        setupUIViews();
        final DatabaseReference myRef = mData.getReference(mAuth.getUid());

        myRef.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                UserProfile user = dataSnapshot.getValue(UserProfile.class);
                if (user.profileCreated)
                    createProfile.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(UserHub.this, "The read failed: " + databaseError.getCode(), Toast.LENGTH_SHORT).show();
            }
        });

        //o botão para criar perfil
        createProfile.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(UserHub.this, CreateProfile.class));
                        createProfile.setVisibility(View.GONE);
                    }
        });

    }


    //O TEU CÓDIGO
    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()){
            case R.id.Definições:
                startActivity(new Intent(UserHub.this, SettingsActivity.class));


            case R.id.Profile: {
                //AINDA NAO TENHO ESTA OPÇÃO CRIA TU À VONTADE THANK YOU
                return true;
            }

            case R.id.TerminarSessão:
                finish();
                startActivity(new Intent(UserHub.this, MainActivity.class));

            //caso queiras adicionar mais opções, tens de ir a res-menu-settings_menu.xml
            //e criar mais icons, depois metes aqui mais cases e fazer o que queres c eles


            default: return super.onOptionsItemSelected(item);
        }
    }

    private void setupUIViews(){
        createProfile = (Button) findViewById(R.id.createprofile_button);
        mAuth = FirebaseAuth.getInstance();
        mData = FirebaseDatabase.getInstance();
    }
}
