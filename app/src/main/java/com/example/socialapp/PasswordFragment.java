package com.example.socialapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;

public class PasswordFragment extends Fragment {

    public static PasswordFragment newInstance() { return new PasswordFragment(); }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.password_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button resetPassword = getView().findViewById(R.id.reset_password_button);
        EditText editEmail = getView().findViewById(R.id.reset_email_input);
        EditText editPassword = getView().findViewById(R.id.reset_password_input);
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        resetPassword.setOnClickListener(view1 -> {
            String email = editEmail.getText().toString().trim();
            String password = editPassword.getText().toString();

            if(email.isEmpty())
                editEmail.setBackgroundColor(getResources().getColor(R.color.light_red));
            else{
                mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        getParentFragmentManager().beginTransaction().remove(PasswordFragment.this).commit();
                        Toast.makeText(getContext(), "Password reset email sent!", Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(getContext(), "Error in sending password reset email", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}