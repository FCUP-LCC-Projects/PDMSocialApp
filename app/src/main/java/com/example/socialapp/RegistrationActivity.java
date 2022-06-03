package com.example.socialapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;
import android.content.Intent;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class RegistrationActivity extends AppCompatActivity {

    private EditText in_username, in_email, in_password;
    private String email, username, password;
    private Button signUp;
    private TextView signedUp;
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);
        getSupportActionBar().hide();
        setupUIViews();

        if(savedInstanceState!=null){
            in_username.setText(savedInstanceState.getString("user"));
            in_email.setText(savedInstanceState.getString("email"));
            in_password.setText(savedInstanceState.getString("password"));
        }

        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getValues();
                if(validate()){

                    mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                sendData();
                                Toast.makeText(RegistrationActivity.this, "Registration Complete", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(RegistrationActivity.this, "Registration Failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }

        });

        signedUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {       //texto que manda de voltar para o login
                finish();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        getValues();
        if(!email.isEmpty())
            outState.putString("email", email);
        if(!password.isEmpty())
            outState.putString("password", password);
        if(!username.isEmpty())
            outState.putString("user", username);
    }

    private void setupUIViews(){
        mAuth = FirebaseAuth.getInstance();
        in_username = findViewById(R.id.username);
        in_password = findViewById(R.id.password);
        in_email = findViewById(R.id.email);
        signUp = findViewById(R.id.button);
        signedUp = findViewById(R.id.already_sign_up);
    }

    private void getValues(){
        username = in_username.getText().toString().trim();
        password = in_password.getText().toString().trim();
        email = in_email.getText().toString().trim();
    }

    private void sendData(){
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference myRef = firebaseDatabase.getReference(mAuth.getUid());
        UserProfile userProfile = new UserProfile(username, email);
        myRef.setValue(userProfile);
    }

    private Boolean validate(){
        Boolean result = false;

        if(username.isEmpty() || password.isEmpty() || email.isEmpty()){
            Toast.makeText(this, "Please enter all the information.", Toast.LENGTH_SHORT).show();
        }
        else{
            result = true;
        }
        return result;
    }
}
