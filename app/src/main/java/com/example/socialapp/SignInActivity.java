package com.example.socialapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.sql.Time;

public class SignInActivity extends AppCompatActivity {

    private EditText in_username, in_email, in_password;
    private String email, username, password;
    private Button signUp;
    private TextView signedUp;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        getSupportActionBar().hide();   //precisamos da Action Bar para o menu da atividade mas fica feio nas outras atividades entao isto está em quase todas sem ser na UserHub
        initUIViews(); //inicializa todos os objetos

        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {   //se carregares no botão signUp
                getValues();    //vai buscar os valores nas barras para as Strings
                if(validate()){ //certifica-se que as Strings estão inicializadas
                    //Upload data to database

                    mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {    //chama a função para meter os dados na base de dados
                            if (task.isSuccessful()) {  //se tiver metido bem
                                sendData();     //manda o username, que é um dado adicional para a storage (onde ficam dados adicionais como idade, ano de nascimento, username, etc)
                                Toast.makeText(SignInActivity.this, "Registration Complete", Toast.LENGTH_SHORT).show();
                                finish(); //vai voltar para a atividade de Login
                            } else {
                                Toast.makeText(SignInActivity.this, "Registration Failed", Toast.LENGTH_SHORT).show();
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
                Intent intent = new Intent(SignInActivity.this, ChatActivity.class);
                startActivity(intent);
            }
        });
    }


    private void initUIViews(){
        mAuth = FirebaseAuth.getInstance();
        in_username = (EditText) findViewById(R.id.username);
        in_password = (EditText) findViewById(R.id.password);
        in_email = (EditText) findViewById(R.id.email);
        signUp = (Button) findViewById(R.id.button);
        signedUp = (TextView) findViewById(R.id.already_sign_up);
    }

    private void getValues(){       //retira o texto para as Strings
        username = in_username.getText().toString().trim();
        password = in_password.getText().toString().trim();
        email = in_email.getText().toString().trim();
    }

    private void sendData(){        //inicializa o storage e manda o username, que é informação extra. Para isso cria um objeto UserProfile, que está num ficheiro de java à parte, mete a informação que foi inserida (email,
        // password e username) no perfil, deixa o restante não inicializado e manda para o storage do Firebase
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

    public void enterWithoutSignIn(View view) {
        Intent intent = new Intent(SignInActivity.this, TimeLineActivity.class);
        startActivity(intent);
    }
}