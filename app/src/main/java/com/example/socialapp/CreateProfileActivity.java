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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_profile);
        getSupportActionBar().hide();
        setupUIViews();
        final DatabaseReference myRef = mData.getReference(mAuth.getUid());     //precisas de uma instancia do storage para depois mandar a informação toda


        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {       //floating action button que se for clicado aparece as opções de logout ou upload a informação
                //o booleano touched serve porque se n tiver cá, quando clicas no butão, ele fica sempre lá com as opções presentes
                if(!touched) {        //se ainda n se tiver tocado no butão, aparecem as opçoes
                    upload.setVisibility(View.VISIBLE);
                    logout.setVisibility(View.VISIBLE);
                    touched = true;
                }
                else{           //se ja se tiver tocado, esconde os butoes
                    upload.setVisibility(View.INVISIBLE);
                    logout.setVisibility(View.INVISIBLE);
                    touched = false;
                }
            }
        });

        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {       //upload da informaçao pro storage
                int selectId = radioGender.getCheckedRadioButtonId();
                genderId = (RadioButton) findViewById(selectId);
                gender = (String) genderId.getText();
                birthdate = picker.getYear() +"-" +picker.getMonth()+"-"+picker.getDayOfMonth();
                age = getAge(picker.getYear(), picker.getMonth(), picker.getDayOfMonth());

                //tudo o que está aqui acima é pra obter a informação que foi metida
                myRef.addValueEventListener(new ValueEventListener() {
                    //vai agora criar um novo perfil de utilizador, meter toda a informação e isto substitui a informação anterior com a nova
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        UserProfile user = dataSnapshot.getValue(UserProfile.class);
                        user.setUserAge(age);
                        user.setGender(gender);
                        user.setBirthdate(birthdate);
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
        radioGender = (RadioGroup) findViewById(R.id.radioGender);
        upload = (Button) findViewById(R.id.upload_data);
        logout = (Button) findViewById(R.id.logout);
        picker = (DatePicker) findViewById(R.id.datePicker);
        mAuth = FirebaseAuth.getInstance();
        mData = FirebaseDatabase.getInstance();
        fab = (FloatingActionButton) findViewById(R.id.floatingActionButton);
        TextView title = findViewById(R.id.create_profile_title);
        title.setText(R.string.create_profile);
    }

    private String getAge(int year, int month, int day){    //receber ano, mes e dia de nascimento
        Date today = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(today);     //mete o tempo de cal, um objeto que é um calendario, para devolver o tempo de hoje

        int current_year = cal.get(Calendar.YEAR);  //recebes o ano atual, em baixo o mes e abaixo o dia atual
        int current_month = cal.get(Calendar.MONTH);
        int current_day = cal.get(Calendar.DAY_OF_MONTH);


        int age = current_year - year;  //idade é ano atual menos ano de nascimento excepto se ainda n se tiver passado o aniversário - então é age-1
        if(current_month<month){
            age--;
        }
        else if(current_month==month){
            if(current_day<day)
                age --;
        }

        return Integer.toString(age);   //devolve como string
    }
}
