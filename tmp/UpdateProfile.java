package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.renderscript.Sampler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.Date;

/*          ISTO É IGUAL AO CREATE PROFILE, VAI VER O CREATE PROFILE            */

public class UpdateProfile extends AppCompatActivity {
    private boolean touched = false;
    private FirebaseAuth mAuth;
    private FirebaseDatabase mData;
    private RadioButton genderId;
    private RadioGroup radioGender;
    private Button upload;
    private String gender, birthdate, age;
    private DatePicker picker;
    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.update_your_profile);
        setContentView(R.layout.activity_update_profile);
        getSupportActionBar().hide();
        setupUIViews();
        final DatabaseReference myRef = mData.getReference(mAuth.getUid());


        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(touched==false) {
                    upload.setVisibility(View.VISIBLE);
                    touched = true;
                }
                else{
                    upload.setVisibility(View.INVISIBLE);
                    touched = false;
                }
            }
        });

        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selectId = radioGender.getCheckedRadioButtonId();
                genderId = (RadioButton) findViewById(selectId);
                gender = (String) genderId.getText();
                birthdate = picker.getYear() +"-" +picker.getMonth()+"-"+picker.getDayOfMonth();
                age = getAge(picker.getYear(), picker.getMonth(), picker.getDayOfMonth());

                myRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        UserProfile user = dataSnapshot.getValue(UserProfile.class);
                        user.setUserAge(age);
                        user.setGender(gender);
                        user.setBirthdate(birthdate);
                        user.profileCreated();
                        myRef.setValue(user);
                        Toast.makeText(UpdateProfile.this, "Profile Updated!", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(UpdateProfile.this, "The read failed: " + databaseError.getCode(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

    }

    private void setupUIViews(){
        radioGender = (RadioGroup) findViewById(R.id.radioGender);
        upload = (Button) findViewById(R.id.upload_data);
        picker = (DatePicker) findViewById(R.id.datePicker);
        mAuth = FirebaseAuth.getInstance();
        mData = FirebaseDatabase.getInstance();
        fab = (FloatingActionButton) findViewById(R.id.floatingActionButton);
    }

    private String getAge(int year, int month, int day){
        Date today = new Date(); // Fri Jun 17 14:54:28 PDT 2016
        Calendar cal = Calendar.getInstance();
        cal.setTime(today);

        int current_year = cal.get(Calendar.YEAR);
        int current_month = cal.get(Calendar.MONTH);
        int current_day = cal.get(Calendar.DAY_OF_MONTH);


        int age = current_year - year;
        if(current_month<month){
            age--;
        }
        else if(current_month==month){
            if(current_day<day)
                age --;
        }

        return Integer.toString(age);
    }
}