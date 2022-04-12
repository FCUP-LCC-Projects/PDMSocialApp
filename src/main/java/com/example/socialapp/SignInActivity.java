package com.example.socialapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

public class SignInActivity extends AppCompatActivity {
    private EditText editSignInUsername;
    private EditText editSignInPassword;
    private Button signInButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
    }


    private void init(){
        editSignInUsername = findViewById(R.id.loginUser);
        editSignInPassword = findViewById(R.id.loginPass);
        signInButton = findViewById(R.id.loginButton);


    }
}