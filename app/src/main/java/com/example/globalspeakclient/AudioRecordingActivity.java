package com.example.globalspeakclient;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

public class AudioRecordingActivity extends AppCompatActivity {

    private Button btnSimulateAudio;
    private Button btnCompleteSignUp;

    private String email, profileName, password, language;

    private AuthHelper authHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_recording);

        // Get user details from intent
        Intent intent = getIntent();
        email = intent.getStringExtra("email");
        profileName = intent.getStringExtra("profileName");
        password = intent.getStringExtra("password");
        language = intent.getStringExtra("language");

        // Initialize AuthHelper
        authHelper = new AuthHelper();

        // Initialize UI components
        btnSimulateAudio = findViewById(R.id.btnSimulateAudio);
        btnCompleteSignUp = findViewById(R.id.btnCompleteSignUp);

        // Simulate audio recording button
        btnSimulateAudio.setOnClickListener(view -> simulateAudioRecording());

        // Complete sign-up button
        btnCompleteSignUp.setOnClickListener(view -> finalizeSignUp());
    }

    private void simulateAudioRecording() {
        // Simulate audio recording logic (for testing purposes)
        Toast.makeText(this, "Audio simulated successfully!", Toast.LENGTH_SHORT).show();
    }

    private void finalizeSignUp() {
        String placeholderAudioUrl = "audio_placeholder_url"; // Placeholder for audio URL

        authHelper.completeSignUp(email, password, profileName, language, placeholderAudioUrl, new AuthHelper.OnAuthResultListener() {
            @Override
            public void onSuccess(com.google.firebase.auth.FirebaseUser user) {
                Toast.makeText(AudioRecordingActivity.this, "Sign-Up completed successfully!", Toast.LENGTH_SHORT).show();
                finish(); // Close the activity
            }

            @Override
            public void onFailure(Exception exception) {
                Toast.makeText(AudioRecordingActivity.this, "Sign-Up failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
