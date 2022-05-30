package com.example.socialapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.renderscript.Sampler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.Month;
import java.time.MonthDay;
import java.time.Year;
import java.util.Calendar;
import java.util.Date;

public class CreateProfileActivity extends AppCompatActivity {
    private boolean touched = false;
    private FirebaseAuth mAuth;
    private FirebaseDatabase mData;
    private RadioButton genderId;
    private RadioGroup radioGender;
    private Button upload, logout;
    private String gender, birthdate, age;
    private DatePicker picker;
    private FloatingActionButton fab;
    private EditText usernameEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_profile);
        getSupportActionBar().hide();
        setupUIViews();
        final DatabaseReference myRef = mData.getReference(mAuth.getUid());

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {       //floating action button que se for clicado aparece as opções de logout ou upload a informação
                //o booleano touched serve porque se n tiver cá, quando clicas no butão, ele fica sempre lá com as opções presentes
                if(!touched) {
                    upload.setVisibility(View.VISIBLE);
                    logout.setVisibility(View.VISIBLE);
                    touched = true;
                }
                else{
                    upload.setVisibility(View.INVISIBLE);
                    logout.setVisibility(View.INVISIBLE);
                    touched = false;
                }
            }
        });

        upload.setOnClickListener(v -> {       //upload da informaçao pro storage
            int selectId = radioGender.getCheckedRadioButtonId();
            genderId = (RadioButton) findViewById(selectId);
            gender = (String) genderId.getText();
            birthdate = picker.getYear() +"-" +picker.getMonth()+"-"+picker.getDayOfMonth();
            age = getAge(picker.getYear(), picker.getMonth(), picker.getDayOfMonth());
            String username = usernameEdit.getText().toString().trim();
            if(username.isEmpty()){
                Toast.makeText(this, "Please enter all the information.", Toast.LENGTH_SHORT).show();
            }
            else {
                myRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        UserProfile user = dataSnapshot.getValue(UserProfile.class);
                        user.setUserAge(age);
                        user.setGender(gender);
                        user.setBirthdate(birthdate);
                        user.setUsername(username);
                        user.profileCreated();
                        myRef.setValue(user);
                        Toast.makeText(CreateProfileActivity.this, "Profile Created!", Toast.LENGTH_SHORT).show();
                        finish();
                        startActivity(new Intent(CreateProfileActivity.this, TimeLineActivity.class));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(CreateProfileActivity.this, "The read failed: " + databaseError.getCode(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void setupUIViews(){
        radioGender = findViewById(R.id.radioGender);
        upload = findViewById(R.id.upload_data);
        logout = findViewById(R.id.logout);
        picker = findViewById(R.id.datePicker);
        mAuth = FirebaseAuth.getInstance();
        mData = FirebaseDatabase.getInstance();
        fab = (FloatingActionButton) findViewById(R.id.floatingActionButton);
        usernameEdit = findViewById(R.id.change_username);
        TextView title = findViewById(R.id.create_profile_title);
        title.setText(R.string.create_profile);
    }

    private String getAge(int year, int month, int day){
        Date today = new Date();
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
