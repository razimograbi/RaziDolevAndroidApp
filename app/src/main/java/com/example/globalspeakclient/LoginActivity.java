package com.example.globalspeakclient;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText etUsername, etPassword;
    private Button btnLogin, btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize UI components
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        // Login button click logic
        btnLogin.setOnClickListener(view -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter both username and password", Toast.LENGTH_SHORT).show();
                return;
            }

            // Placeholder for server logic
            // Example:
            // loginToServer(username, password);
            Log.d(TAG, "Username: " + username + ", Password: " + password);

            // Navigate to ConnectActivity (simulating successful login)
            Intent intent = new Intent(LoginActivity.this, ConnectActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
            finish();
        });

        // Register button click logic
        btnRegister.setOnClickListener(view -> {
            Toast.makeText(LoginActivity.this, "Registration feature is under development", Toast.LENGTH_SHORT).show();

            // Placeholder for server logic for registration
            // Example:
            // registerUser(username, password);
        });
    }

    // Placeholder methods for server logic
    private void loginToServer(String username, String password) {
        // Example:
        // Send HTTP POST request to the server with username and password
        // On success, navigate to ConnectActivity
        // On failure, show an error message
    }

    private void registerUser(String username, String password) {
        // Example:
        // Send HTTP POST request to the server to create a new account
        // On success, log in the user or show a success message
        // On failure, show an error message
    }
}
