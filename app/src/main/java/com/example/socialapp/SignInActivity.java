package com.example.socialapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class SignInActivity extends AppCompatActivity {

    private EditText in_email, in_password; //recebe o texto escrito na barra de email e de password
    private String email, password;         //o texto transformado das variaveis acima
    private Button logIn;                   //botao de login
    private TextView loggedIn, Info;        //texto em caso de ainda nao estar registado e informação sobre tentativas
    private int counter = 3;                //contador de tentativas
    private FirebaseAuth mAuth;             //cena de autenticação que liga à base de dados da Firebase

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        setupUIViews(); //inicializa todos os objetos
        getSupportActionBar().hide(); //precisamos da Action Bar para o menu da atividade mas fica feio nas outras atividades entao isto está em quase todas sem ser na UserHub

        Info.setText("No of attempts remaining: 3");    // Mete o texto como 3 tentativas iniciais de Login


        logIn.setOnClickListener(new View.OnClickListener() {    /* este textão, tudo é implementado automaticamente, praticamente adiciona uma opção de ação quando clicas no butão de login */
            @Override
            public void onClick(View v) {       // ao clicares ação abaixo
                email = in_email.getText().toString().trim();   //quando clicas mete o texto que estava na barrinha de escrita de email na String email
                password = in_password.getText().toString().trim(); //texto na barrinha de escrita password na String password
                if(inputValidate(email, password))          //passa pela função inputValidate que está abaixo aqui no código
                    loginValidate(email, password);         //e se inputValidate for verdade, passa pelo loginValidate e avança com a vida

            }
        });

        loggedIn.setOnClickListener(new View.OnClickListener() {    // caso ainda não teja o utilizador registado então passa para a atividade onde se pode registar
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignInActivity.this, RegistrationActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        FirebaseUser user = mAuth.getCurrentUser();


        if(user!=null){
            startActivity(new Intent(SignInActivity.this, TimeLineActivity.class));
            finish();
        }

        super.onResume();
    }

    private void setupUIViews(){ //isto basicamente liga só as variáveis de Objetos que estamos a criar aqui, com o seu código correspondente, as variaveis que estão no xml
        in_email = findViewById(R.id.username);
        in_password = (EditText) findViewById(R.id.password);
        logIn = (Button) findViewById(R.id.log_in);
        loggedIn = (TextView) findViewById(R.id.not_registered_sign_up);
        mAuth = FirebaseAuth.getInstance();         // esta é a unica variavel que nao está no xml, em que vamos buscar uma "instancia" de base de dados à firebase
        Info = (TextView) findViewById(R.id.attempts);
    }

    private Boolean inputValidate(String username, String password){        //esta função praticamente garante que nenhuma das barrinhas de texto está vazia. se tiverem, diz pra meter toda a informação
        Boolean result = false;

        if(username.isEmpty() || password.isEmpty()){
            Toast.makeText(this, "Please enter all the information.", Toast.LENGTH_SHORT).show();
        }
        else{
            result = true;
        }
        return result;
    }

    private void loginValidate(String username, String password){
        //valida o login

        mAuth.signInWithEmailAndPassword(username, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {   //este textão, tudo é implementado automaticamente então nao te assustes

            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {        //caso o sign in for completo, isto é, não há erros ao contactar a Firebase (NAO É SOBRE O USER TER METIDO BEM A INFORMAÇÃO, É SOBRE CONTACTO A BASE DE DADOS)
                if(task.isSuccessful()){        // se o user estiver registado e meter bem os dados
                    Toast.makeText(SignInActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();


                    checkToCreateProfile().addOnCompleteListener(new OnCompleteListener<Boolean>() {
                        @Override
                        public void onComplete(@NonNull Task<Boolean> task) {
                            Log.d("PROFILE", String.valueOf(task.getResult()));
                            if(task.getResult()) {
                                Intent intent = new Intent(SignInActivity.this, TimeLineActivity.class);

                                startActivity(intent);
                            }
                            else
                                startActivity(new Intent(SignInActivity.this, CreateProfileActivity.class));
                        }
                    });
                }
                else{       //se nao tiver registado ou nao tiver metido bem os dados
                    Toast.makeText(SignInActivity.this, "Login Failed", Toast.LENGTH_SHORT).show();
                    counter--;

                    String info = "No of attempts remaining: "+counter;
                    Info.setText(info);

                    if(counter==0){
                        logIn.setEnabled(false);
                    }
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                notifyUser(e.getLocalizedMessage());
            }
        });
    }

    private void notifyUser(String localizedMessage) {
        Toast.makeText(this, localizedMessage, Toast.LENGTH_SHORT).show();
    }

    private Task<Boolean> checkToCreateProfile(){
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference myRef = firebaseDatabase.getReference(mAuth.getUid());
        final TaskCompletionSource<Boolean> taskCompletionSource = new TaskCompletionSource<>();

        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UserProfile userProfile = snapshot.getValue(UserProfile.class);
                System.out.println(userProfile.getUsername());
                System.out.println(userProfile.getProfileCreated());
                taskCompletionSource.setResult(userProfile.profileCreated);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                System.out.println("The read failed: " + error.getCode());
                taskCompletionSource.setException(new IOException("Profile", error.toException()));
            }
        });
        return taskCompletionSource.getTask();
    }

    public void enterWithoutSignIn(View view) {
        Intent intent = new Intent(SignInActivity.this, TimeLineActivity.class);
        startActivity(intent);
    }
}
