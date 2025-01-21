package com.example.globalspeakclient;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;



/**
 * Handles the user login process.
 *
 * This activity allows users to:
 * - Enter their email and password to sign in.
 * - Navigate to the sign-up screen if they do not have an account.
 * - Automatically log in if the user is already signed in (unless testing).
 *
 * Calls `AuthenticationService` to authenticate users with Firebase.
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private AuthenticationService authenticationService;
    private EditText etUsername, etPassword;
    private Button btnLogin, btnSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Check if user is already signed in (only if NOT testing)
        boolean isTesting = true; // Set this to true during testing
        if (!isTesting) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                navigateToConnectActivity(currentUser.getUid());
                return;
            }
        }

        initializeUI();
        configureLoginButton();
        configureSignUpButton();
    }

    private void initializeUI() {
        authenticationService = new AuthenticationService();
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnSignUp = findViewById(R.id.btnSignup);
    }

    private void configureLoginButton() {
        btnLogin.setOnClickListener(view -> {
            String email = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter both email and password.", Toast.LENGTH_SHORT).show();
                return;
            }

            authenticationService.signIn(email, password, new AuthenticationService.OnAuthResultListener() {
                @Override
                public void onSuccess(FirebaseUser user) {
                    handleSuccessfulLogin(user);
                }

                @Override
                public void onFailure(Exception exception) {
                    handleFailedLogin(exception);
                }
            });
        });
    }

    private void configureSignUpButton() {
        btnSignUp.setOnClickListener(view -> {
            // Navigate to SignUpActivity
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });
    }

    private void navigateToConnectActivity(String uid) {
        Intent intent = new Intent(LoginActivity.this, ConnectActivity.class);
        intent.putExtra("uid", uid);
        startActivity(intent);
        finish();
    }

    private void handleSuccessfulLogin(FirebaseUser user) {
        if (user != null) {
            String uid = user.getUid(); // Get UID
            Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();

            navigateToConnectActivity(uid);
        }
    }

    private void handleFailedLogin(Exception exception) {
        Log.e(TAG, "Login failed", exception);
        Toast.makeText(LoginActivity.this, "Incorrect email or password. Please try again.", Toast.LENGTH_SHORT).show();
    }
}
