package com.example.globalspeakclient;

import android.content.Context;
import android.content.DialogInterface;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import androidx.appcompat.app.AlertDialog;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;


import org.json.JSONException;
import org.json.JSONObject;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "VOIPClient";

    private EditText etReceiverId;
    private TextView tvStatus;
    private Button btnCall, btnHangUp;

    // WebSocket
    private OkHttpClient okHttpClient;
    private WebSocket webSocket;

    // Audio
    private static final int SAMPLE_RATE = 16000; // Todo : This must be changed to 24KH and must test it.
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord audioRecord;
    private AudioTrack audioTrack;
    private int minRecBufferSize;
    private int minPlayBufferSize;

    private AtomicBoolean isRecording = new AtomicBoolean(false);
    private AtomicBoolean isPlaying = new AtomicBoolean(false);

    // call management
    private String userId; // We get it from Intent
    private String language; // We get it from Intent
    private String otherUserId;
    private String activeCallId; // store the call_id from the server

    // Permission code
    private static final int REQ_AUDIO_PERMISSION = 123;

    // ================= We must be careful when selecting these ==============================
    // The server IP: for emulator, 10.0.2.2
    // The server is listening on port 8000
    private static final String BASE_URL = "http://10.0.0.16:8000";
    private static final String WS_URL = "ws://10.0.0.16:8000/ws";

    // ========================================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        userId = getIntent().getStringExtra("userId");
        language = getIntent().getStringExtra("language");
        if (userId == null) {
            Toast.makeText(this, "No userId found, please go back.", Toast.LENGTH_SHORT).show();
        }
        if(language == null){
            Toast.makeText(this, "No language found, please go back.", Toast.LENGTH_SHORT).show();
        }

        etReceiverId = findViewById(R.id.etReceiverId);
        tvStatus = findViewById(R.id.tvStatus);
        btnCall = findViewById(R.id.btnCall);
        btnHangUp = findViewById(R.id.btnHangUp);

        // Check microphone permission
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO_PERMISSION);
        }

        okHttpClient = new OkHttpClient();

        // "Call" button for outgoing calls
        btnCall.setOnClickListener(view -> {
            otherUserId = etReceiverId.getText().toString().trim();
            if (otherUserId.isEmpty()) {
                Toast.makeText(MainActivity.this,
                        "Enter a receiver ID",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            // 1) Initiate call (REST) => get call_id
            // 2) If not already connected, connect WebSocket => send userId
            // 3) Wait for call_accepted/call_started => start streaming
            initiateCall(userId, language, otherUserId);
        });

        btnHangUp.setOnClickListener(view -> hangUp());

        if(webSocket == null){
            connectWebSocket(userId, language);
        }
    }

    /**
     * 1) Initiate the call via /calls/initiate
     * 2) If successful, we parse JSON to get call_id
     * 3) Connect WebSocket
     */
    private void initiateCall(String callerId, String caller_language, String receiverId) {
        tvStatus.setText(getString(R.string.initiating_call));

        if (webSocket == null) {
            connectWebSocket(callerId, caller_language);
        }

        // Use ExecutorService to perform the task in a background thread
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            String errorMessage = null;
            String callId = null;

            try {
                // Create JSON request body
                MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                JSONObject bodyJson = new JSONObject();
                bodyJson.put("caller_id", callerId);
                bodyJson.put("receiver_id", receiverId);

                RequestBody body = RequestBody.create(JSON, bodyJson.toString());
                Request request = new Request.Builder()
                        .url(BASE_URL + "/calls/initiate")
                        .post(body)
                        .build();

                // Execute the request
                Response response = okHttpClient.newCall(request).execute();
                if (!response.isSuccessful()) {
                    errorMessage = "Initiate call failed: " + response.message();
                } else {
                    String respBody = response.body().string();
                    // Parse the response JSON
                    JsonObject json = JsonParser.parseString(respBody).getAsJsonObject();
                    if (json.has("call_id")) {
                        callId = json.get("call_id").getAsString();
                    } else {
                        errorMessage = "No call_id in response";
                    }
                }
            } catch (Exception e) {
                errorMessage = e.getMessage();
            }

            // Post the result back to the main thread
            final String finalCallId = callId;
            final String finalErrorMessage = errorMessage;
            handler.post(() -> {
                if (finalCallId != null) {
                    activeCallId = finalCallId;
                    tvStatus.setText("Call initiated. call_id=" + finalCallId);
                    // Connect WebSocket now
                    connectWebSocket(userId, language);
                } else {
                    tvStatus.setText("Error: " + finalErrorMessage);
                }
            });
        });
    }


    /**
     *  Connect the WebSocket and identify ourselves with userId.
     */
    private void connectWebSocket(String userId, String language) {
        if (webSocket != null) {
            Log.d(TAG, "WebSocket is already connected or connecting.");
            return;
        }
        tvStatus.setText("Connecting WebSocket...");

        Request request = new Request.Builder().url(WS_URL).build();
        webSocket = okHttpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, okhttp3.Response response) {
                Log.d(TAG, "WebSocket opened");
                runOnUiThread(() -> {
                    tvStatus.setText("WebSocket connected");
                });

                // Send initial JSON: {"user_id":"<userId>"}
                try {
                    JSONObject initMsg = new JSONObject();
                    initMsg.put("user_id", userId);
                    initMsg.put("language", language);
                    webSocket.send(initMsg.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                Log.d(TAG, "onMessage: " + text);
                // JSON: could be call_accepted, call_started, call_rejected, etc.
                handleJsonMessage(text);
            }

            @Override
            public void onMessage(WebSocket webSocket, okio.ByteString bytes) {
                // This is binary data => PCM audio
                byte[] audioData = bytes.toByteArray();
                playIncomingAudio(audioData);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
                Log.e(TAG, "WebSocket Failure: " + t.getMessage());
                runOnUiThread(() -> {
                    tvStatus.setText("WS Failure: " + t.getMessage());
                    stopAudioStreaming();
                });
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket closed: " + reason);
                runOnUiThread(() -> {
                    tvStatus.setText("WebSocket closed");
                    stopAudioStreaming();
                });
            }
        });
    }

    /**
     * Parse server-sent JSON messages
     */
    private void handleJsonMessage(String text) {
        try {
            JSONObject json = new JSONObject(text);
            if (json.has("type")) {
                String type = json.getString("type");
                switch (type) {
                    case "incoming_call":
                        // e.g. { "type":"incoming_call", "call_id":"...", "from":"someUser" }
                        String incomingCallId = json.optString("call_id", null);
                        String fromUser = json.optString("from", "unknown");
                        if (incomingCallId != null) {
                            runOnUiThread(() -> showIncomingCallDialog(incomingCallId, fromUser));
                        }
                        break;

                    case "call_accepted":
                    case "call_started":
                        // The call is active => start audio
                        runOnUiThread(() -> {
                            tvStatus.setText("Call Active with call_id=" + activeCallId);
                            startAudioStreaming();
                        });
                        break;

                    case "call_rejected":
                        runOnUiThread(() -> {
                            tvStatus.setText("Call Rejected");
                        });
                        stopAudioStreaming();
                        // closeWebSocket();
                        break;

                    case "call_ended":
                        runOnUiThread(() -> {
                            String endedBy = json.optString("ended_by", "unknown");
                            tvStatus.setText("Call Ended by " + endedBy);
                        });
                        stopAudioStreaming();
                        // closeWebSocket();
                        break;

                    default:
                        // Maybe will implement a "call_id"/"seq_num"
                        break;
                }
            } else if (json.has("call_id") && json.has("seq_num")) {
                // Audio metadata from the other participant, handled by the next binary frame
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Show a dialog for an incoming call with two buttons: Accept or Reject
     */
    private void showIncomingCallDialog(String callId, String fromUser) {
        // We can store callId in activeCallId if we accept
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Incoming Call")
                .setMessage("Call from " + fromUser)
                .setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        activeCallId = callId; // store it so we can reference later
                        respondToCall(callId, "accept");
                    }
                })
                .setNegativeButton("Reject", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        respondToCall(callId, "reject");
                    }
                })
                .setCancelable(false)
                .show();
    }

    /**
     * Send a POST /calls/response with {"call_id":"<callId>", "response":"accept" or "reject"}
     */
    private void respondToCall(String callId, String responseType) {
        tvStatus.setText("Responding to call... " + responseType);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            String errorMessage = null;
            boolean success = false;
            try {
                MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                JSONObject bodyJson = new JSONObject();
                bodyJson.put("call_id", callId);
                bodyJson.put("response", responseType);

                RequestBody body = RequestBody.create(JSON, bodyJson.toString());
                Request request = new Request.Builder()
                        .url(BASE_URL + "/calls/response")
                        .post(body)
                        .build();

                Response response = okHttpClient.newCall(request).execute();
                if (!response.isSuccessful()) {
                    errorMessage = "Call response failed: " + response.message();
                } else {
                    success = true;
                }
            } catch (Exception e) {
                errorMessage = e.getMessage();
            }

            final boolean finalSuccess = success;
            final String finalErrorMessage = errorMessage;
            handler.post(() -> {
                if (!finalSuccess) {
                    tvStatus.setText("Error responding to call: " + finalErrorMessage);
                } else {
                    tvStatus.setText("Responded: " + responseType);
                    // If accepted, we wait for "call_started" or "call_accepted" from server
                    // If rejected, the server notifies caller with "call_rejected"
                }
            });
        });
    }


    /**
     * Hang up by calling /calls/disconnect with call_id + user_id
     * Then stop audio + close WebSocket
     */
    private void hangUp() {
        if (activeCallId == null || userId == null) {
            tvStatus.setText(getString(R.string.hang_up_no_active_call));
            stopAudioStreaming();
            // closeWebSocket();
            return;
        }

        // Create a background thread using ExecutorService
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            String errorMessage = null;
            boolean success;

            try {
                MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                JSONObject bodyJson = new JSONObject();
                bodyJson.put("call_id", activeCallId);
                bodyJson.put("user_id", userId);

                RequestBody body = RequestBody.create(JSON, bodyJson.toString());
                Request request = new Request.Builder()
                        .url(BASE_URL + "/calls/disconnect")
                        .post(body)
                        .build();

                Response response = okHttpClient.newCall(request).execute();
                if (!response.isSuccessful()) {
                    errorMessage = "Disconnect failed: " + response.message();
                    success = false;
                } else {
                    success = true;
                }
            } catch (Exception e) {
                errorMessage = e.getMessage();
                success = false;
            }

            // Post the result back to the main thread
            final boolean finalSuccess = success;
            final String finalErrorMessage = errorMessage;
            handler.post(() -> {
                if (finalSuccess) {
                    tvStatus.setText("Call disconnected.");
                } else {
                    tvStatus.setText("Error disconnecting: " + finalErrorMessage);
                }

                // Either way, stop audio and close WebSocket
                stopAudioStreaming();
                // closeWebSocket();
            });
        });
    }


    private void closeWebSocket() {
        if (webSocket != null) {
            webSocket.close(1000, "Hang Up");
            webSocket = null;
        }
    }

    //region Audio
    private void startAudioStreaming() {
        if(!initAudio()){
            hangUp();
            return;
        }
        isRecording.set(true);
        isPlaying.set(true);

        // Recording thread
        new Thread(() -> {
            audioRecord.startRecording();
            int seqNum = 0;
            int bufferSize = minRecBufferSize; // bigger buffer maybe (will test it)
            byte[] buffer = new byte[bufferSize];

            while (isRecording.get()) {
                int readBytes = audioRecord.read(buffer, 0, buffer.length);
                if (readBytes > 0 && webSocket != null) {
                    seqNum++;
                    // Send metadata JSON
                    try {
                        JSONObject meta = new JSONObject();
                        meta.put("call_id", activeCallId);
                        meta.put("seq_num", seqNum);
                        meta.put("sample_rate", SAMPLE_RATE);
                        meta.put("chunk_size", readBytes);
                        webSocket.send(meta.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    // Then send raw audio
                    webSocket.send(okio.ByteString.of(buffer, 0, readBytes));
                }
            }
            audioRecord.stop();
            audioRecord.release();
        }).start();

        // Playback thread
        new Thread(() -> {
            audioTrack.play();
            while (isPlaying.get()) {
                // We do not "pull" data from anywhere;
                // the data is pushed in onMessage(...)
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            audioTrack.stop();
            audioTrack.release();
        }).start();
    }

    private void stopAudioStreaming() {
        isRecording.set(false);
        isPlaying.set(false);
    }

    private boolean initAudio() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            finish();
            return false;
        }
        // Get buffer sizes for AudioRecord and AudioTrack
        // 16kHz, 16-bit PCM, mono
        minRecBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        minPlayBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AUDIO_FORMAT);

        // validate the buffer sizes
        if (minRecBufferSize == AudioRecord.ERROR || minRecBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Toast.makeText(this, "Invalid buffer size for AudioRecord", Toast.LENGTH_SHORT).show();
            finish();
            return false;
        }
        Log.d(TAG, "This is the buffer Sizeee :  " + String.valueOf(minRecBufferSize));

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                minRecBufferSize
        );

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Toast.makeText(this, "Failed to initialize AudioRecord", Toast.LENGTH_SHORT).show();
            finish();
            return false;
        }

        audioTrack = new AudioTrack.Builder()
                .setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                                .build())
                .setAudioFormat(
                        new AudioFormat.Builder()
                                .setSampleRate(SAMPLE_RATE)
                                .setEncoding(AUDIO_FORMAT)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build())
                .setBufferSizeInBytes(minPlayBufferSize)
                .build();

        if (audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
            Toast.makeText(this, "Failed to initialize AudioTrack", Toast.LENGTH_SHORT).show();
            finish();
            return false;
        }

        // ===========================
        // 5) Lets setup a  ECHO CANCELLATION / NOISE SUPPRESSION in order not to hear echo
        // ===========================

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            System.out.println("DEBUG : Trying to reach inside");
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            // Lets also set the speaker to false to avoid echo
            audioManager.setSpeakerphoneOn(false);
        }

        // Lets : Enable AcousticEchoCanceler if available
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            System.out.println("DEBUG : I am here !!!");
            if (AcousticEchoCanceler.isAvailable()) {
                AcousticEchoCanceler aec =
                        AcousticEchoCanceler.create(audioRecord.getAudioSessionId());
                if (aec != null) {
                    aec.setEnabled(true);
                    Log.d(TAG, "Acoustic Echo Canceler enabled!!");
                }
            }
            // also  NoiseSuppressor
            if (NoiseSuppressor.isAvailable()) {
                NoiseSuppressor ns =
                        NoiseSuppressor.create(audioRecord.getAudioSessionId());
                if (ns != null) {
                    ns.setEnabled(true);
                    Log.d(TAG, "NoiseSuppressor enabled!! woooow");
                }
            }

            // I found this on the internet : AutomaticGainControl
            // I think it normalizes the clients voice( so when it's high it lowes it and vese versa)

            if (AutomaticGainControl.isAvailable()) {
                AutomaticGainControl agc =
                        AutomaticGainControl.create(audioRecord.getAudioSessionId());
                if (agc != null) {
                    agc.setEnabled(true);
                    Log.d(TAG, "AutomaticGainControl enabled");
                }
            }


        }
        Toast.makeText(this, "Audio Init successfully ", Toast.LENGTH_SHORT).show();
        return true;
    }

    private void playIncomingAudio(byte[] pcmData) {
        if (isPlaying.get()) {
            audioTrack.write(pcmData, 0, pcmData.length);
        }
    }
    //endregion

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // If user leaves the app => hang up
        hangUp();
        closeWebSocket();
    }

    //region Permissions
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Mic permission granted
                Toast.makeText(this, "Mic permission granted", Toast.LENGTH_SHORT).show();
            } else {
                // Mic permission denied
                Toast.makeText(this, "Mic permission denied", Toast.LENGTH_SHORT).show();
                finish(); // Exit the app
            }
        }
    }

    //endregion
}
