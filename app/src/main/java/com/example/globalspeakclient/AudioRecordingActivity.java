package com.example.globalspeakclient;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseUser;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This Activity is responsible for Recording user voice sample.
 * Once we submit it, user is sign up and saved in db and we move to login.
 */
public class AudioRecordingActivity extends AppCompatActivity {

    private static final int RECORDING_DURATION_MS = 8000; // 8 seconds
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final String BASE_URL = "http://10.0.0.16:8000";

    private TextView tvInstructions, tvTextToRead, tvTimer;
    private Button btnRecord, btnPlayRecording, btnSubmitRecording;
    private ProgressBar loadingProgressBar;
    private WavRecorder wavRecorder;
    private MediaPlayer mediaPlayer;
    private boolean isRecording = false;
    private boolean finishRecording = false;
    private boolean hasPlayedRecording = false;

    private Handler timerHandler = new Handler();
    private long startTime;

    private String email, profileName, password, language;
    private AuthenticationService authenticationService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_recording);

        // Initialize services
        authenticationService = new AuthenticationService();

        // Retrieve data from SignUpActivity
        email = getIntent().getStringExtra("email");
        profileName = getIntent().getStringExtra("profileName");
        password = getIntent().getStringExtra("password");
        language = getIntent().getStringExtra("language");

        loadingProgressBar = findViewById(R.id.loadingProgressBar);

        initializeUI();

        btnRecord.setOnClickListener(view -> startRecording());
        btnPlayRecording.setOnClickListener(view -> playRecording());

        btnSubmitRecording.setOnClickListener(view -> submitRecording());

        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_RECORD_AUDIO_PERMISSION);

    }

    private void initializeUI() {
        tvInstructions = findViewById(R.id.tvInstructions);
        tvTextToRead = findViewById(R.id.tvTextToRead);
        tvTimer = findViewById(R.id.tvTimer);
        btnRecord = findViewById(R.id.btnRecord);
        btnPlayRecording = findViewById(R.id.btnPlayRecording);
        btnSubmitRecording = findViewById(R.id.btnSubmitRecording);

        tvInstructions.setText(getString(R.string.audio_recording_instructions));
        tvTextToRead.setText(getString(R.string.audio_text_to_read));

    }

    /**
     * Starts audio recording using WavRecorder.
     * Grants necessary permissions and manages UI updates.
     */
    private void startRecording() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            btnRecord.setEnabled(false);
            finishRecording = false;
            hasPlayedRecording = false;
            File wavFile = new File(getExternalFilesDir(null), "audio.wav");
            wavRecorder = new WavRecorder(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, wavFile);

            wavRecorder.startRecording();
            isRecording = true;
            startTime = System.currentTimeMillis();
            timerHandler.postDelayed(updateTimerRunnable, 0);

            timerHandler.postDelayed(this::stopRecording, RECORDING_DURATION_MS);

            Toast.makeText(this, getString(R.string.recording_started), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("AudioRecording", "Error starting recording: ", e);
            Toast.makeText(this, "Failed to start recording.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Stops the audio recording process after 8 seconds.
     * no need for user to stop
     */
    private void stopRecording() {
        try {
            if (isRecording) {
                wavRecorder.stopRecording();

                isRecording = false;
                timerHandler.removeCallbacks(updateTimerRunnable);
                finishRecording = true;
                btnRecord.setEnabled(true);
                Toast.makeText(this, getString(R.string.recording_stopped), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("AudioRecording", "Error stopping recording: ", e);
        }
    }

    /**
     * Plays the recorded audio file.
     * Ensures that the user has completed recording before playback.
     */
    private void playRecording() {
        if (!finishRecording) {
            Toast.makeText(this, "Please record audio before playing.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (wavRecorder == null) {
            Toast.makeText(this, "No recording available to play.", Toast.LENGTH_SHORT).show();
            return;
        }

        File wavFile = wavRecorder.getWavFile();
        if (wavFile == null || !wavFile.exists()) {
            Toast.makeText(this, "No recording available to play.", Toast.LENGTH_SHORT).show();
            return;
        }
        // Always reset and restart playback
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(wavFile.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();
            Toast.makeText(this, getString(R.string.recording_playing), Toast.LENGTH_SHORT).show();

            // Mark playback as completed only when audio finishes
            mediaPlayer.setOnCompletionListener(mp -> {
                hasPlayedRecording = true;
                mediaPlayer.release();
                mediaPlayer = null;
            });
        } catch (IOException e) {
            Log.e("AudioRecording", "Error playing recording: ", e);
            Toast.makeText(this, "Failed to play recording.", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Submits the recorded audio file to the server for embedding generation.
     * After receiving the embedding, it calls AuthenticationService to sign up the user.
     */
    private void submitRecording() {
        if (!hasPlayedRecording) {
            Toast.makeText(this, "Please listen to the recording before submitting.", Toast.LENGTH_SHORT).show();
            return;
        }

        File audioFile = wavRecorder.getWavFile();
        if (audioFile == null || !audioFile.exists()) {
            Toast.makeText(this, "No recording file found!", Toast.LENGTH_SHORT).show();
            return;
        }

        runOnUiThread(() -> loadingProgressBar.setVisibility(View.VISIBLE));

        // Send audio file to the server and wait for embedding
        new Thread(() -> {
            JSONObject responseJson = sendWavAndGetResponse(audioFile);
            runOnUiThread(() -> {
                loadingProgressBar.setVisibility(View.GONE); // Hide the ProgressBar
                if (responseJson != null) {
                    try {
                        String embedding = "[]";
                        String gptCondLatent = "[]";

                        // Handle "embedding"
                        if (responseJson.has("embedding") && !responseJson.isNull("embedding")) {
                            Object embeddingObj = responseJson.get("embedding");
                            if (embeddingObj instanceof String) {
                                embedding = (String) embeddingObj;
                            } else if (embeddingObj instanceof JSONArray) {
                                embedding = ((JSONArray) embeddingObj).toString();
                            } else {
                                // Handle unexpected types
                                throw new JSONException("Unexpected type for 'embedding'");
                            }
                        }

                        // Handle "gpt_cond_latent"
                        if (responseJson.has("gpt_cond_latent") && !responseJson.isNull("gpt_cond_latent")) {
                            Object latentObj = responseJson.get("gpt_cond_latent");
                            if (latentObj instanceof String) {
                                gptCondLatent = (String) latentObj;
                            } else if (latentObj instanceof JSONArray) {
                                gptCondLatent = ((JSONArray) latentObj).toString();
                            } else {
                                // Handle unexpected types
                                throw new JSONException("Unexpected type for 'gpt_cond_latent'");
                            }
                        }

                        //String embedding = responseJson.getJSONArray("embedding").toString();
                        //String gptCondLatent = responseJson.getJSONArray("gpt_cond_latent").toString();

                        User user = new User(email, password, profileName, language, embedding, gptCondLatent);
                        authenticationService.signUp(user, new AuthenticationService.OnAuthResultListener() {
                            @Override
                            public void onSuccess(FirebaseUser user) {
                                Toast.makeText(AudioRecordingActivity.this, "Sign-up completed!", Toast.LENGTH_SHORT).show();
                                finish(); // Navigate to login
                            }

                            @Override
                            public void onFailure(Exception exception) {
                                Toast.makeText(AudioRecordingActivity.this, "Submission failed. Please try again.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (JSONException e) {
                        Log.e("AudioRecordingActivity", "Error parsing JSON response", e);
                        Toast.makeText(this, "Submission failed. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Submission failed. Please try again.", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }


    /**
     * Sends the audio file to the server and retrieves the embedding vector.
     * Uses OkHttp to perform an HTTP POST request.
     * Returns the embedding vector as a JSON-parsed string.
     */
    private JSONObject sendWavAndGetResponse(File audioFile) {
        OkHttpClient client = new OkHttpClient();
// Creating a multipart request to send the WAV file to the server.
// - `setType(MultipartBody.FORM)`: Specifies the request format as form-data.
// - `addFormDataPart("file", audioFile.getName(), ...)`:
//   - The first parameter "file" is the key expected by the server.
//   - The second parameter is the file name that will be sent.
//   - The third parameter contains the actual file data with the MIME type "audio/wav".
        RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("file", audioFile.getName(), RequestBody.create(audioFile, MediaType.parse("audio/wav"))).build();

        Request request = new Request.Builder().url(BASE_URL + "/process_audio").post(requestBody).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Log.e("AudioRecordingActivity", "Server error: " + response.code());
                return null;
            }
            if (response.body() == null) {
                Log.e("AudioRecordingActivity", "Response body is null");
                return null;
            }

            JSONObject jsonResponse = new JSONObject(response.body().string());

            //Ensure both `embedding` and `gpt_cond_latent` exist before returning
            if (jsonResponse.has("embedding") && jsonResponse.has("gpt_cond_latent")) {
                return jsonResponse;
            } else {
                Log.e("AudioRecordingActivity", "Response missing required fields.");
                return null;
            }

        } catch (IOException | JSONException e) {
            Log.e("AudioRecordingActivity", "Error processing response: ", e);
            return null;
        }
    }


    private final Runnable updateTimerRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;

            tvTimer.setText(String.format(Locale.ROOT, "%02d:%02d", minutes, seconds));

            if (isRecording) {
                timerHandler.postDelayed(this, 500);
            }
        }
    };

    /**
     * Handles permission requests for audio recording.
     * Displays appropriate toast messages based on user permission choices.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, getString(R.string.permission_granted), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show();
            }
        }
    }

}

