package com.example.globalspeakclient;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class SignUpActivity extends AppCompatActivity {

    private EditText etEmail, etProfileName, etPassword, etConfirmPassword;
    private Spinner spinnerLanguage;
    private Button btnContinue;

    private AuthHelper authHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        initializeUI();
        configureContinueButton();
    }

    private void initializeUI() {
        etEmail = findViewById(R.id.etEmail);
        etProfileName = findViewById(R.id.etProfileName);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
        btnContinue = findViewById(R.id.btnContinue);

        authHelper = new AuthHelper();

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.languages, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(adapter);
    }


    private void configureContinueButton() {
        btnContinue.setOnClickListener(view -> {
            String email = etEmail.getText().toString().trim();
            String profileName = etProfileName.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();
            String language = spinnerLanguage.getSelectedItem().toString();

            validateInputsAndProceed(email, profileName, password, confirmPassword, language);
        });
    }

    private void validateInputsAndProceed(String email, String profileName, String password, String confirmPassword, String language) {
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(profileName) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show();
            return;
        }

        authHelper.validateUser(email, password, new AuthHelper.OnAuthResultListener() {
            @Override
            public void onSuccess(com.google.firebase.auth.FirebaseUser user) {
                proceedToAudioScreen(email, profileName, password, language);
            }

            @Override
            public void onFailure(Exception exception) {
                Toast.makeText(SignUpActivity.this, exception.getMessage(), Toast.LENGTH_SHORT).show();
            }


        });
    }

    private void proceedToAudioScreen(String email, String profileName, String password, String language) {
        Intent intent = new Intent(SignUpActivity.this, AudioRecordingActivity.class);
        intent.putExtra("email", email);
        intent.putExtra("profileName", profileName);
        intent.putExtra("password", password);
        intent.putExtra("language", language);
        startActivity(intent);
        finish();
    }
}
