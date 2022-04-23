package com.example.myapplication;

public class UserProfile {  //perfil do utilizador, com username, email, idade, genero, data de nascimento.
    public boolean profileCreated;  //isto serve porque no menu da atividade há um butão chamado Create Profile - garante que a primeira cena que fazes quando entras pela primeira vez na atividade é criar o teu perfil
                                    //porque senão não tens detalhes para alem de email e username
                                    //se ja tiveres o perfil criado o butao n aparece entao é pra isso que serve este bool - PODE VIR A SER REPENSADO
    public String username;
    public String userEmail;
    public String userAge;
    public String gender;
    public String birthdate;

    UserProfile(String username, String email){
        this.username = username;
        this.userEmail = email;
        profileCreated = false;
        this.userAge = "";
        this.gender = "";
        this.birthdate = "";
    }

    public void setUserAge(String userAge) {
        this.userAge = userAge;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }
    public void setBirthdate(String birthdate) {
        this.birthdate = birthdate;
    }
    public void profileCreated(){ profileCreated = true; }

    public UserProfile(){}
}
