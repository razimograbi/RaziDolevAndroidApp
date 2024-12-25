package com.example.globalspeakclient;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * This Activity is responsible for connecting the user to the WebSocket.
 * Once connected, it navigates to MainActivity.
 */
public class ConnectActivity extends AppCompatActivity {
    private static final String TAG = "ConnectActivity";
    private static final String WS_URL = "ws://10.0.2.2:8000/ws";

    private EditText etUserId;
    private Button btnConnect;
    private TextView tvStatus;

    private OkHttpClient okHttpClient;
    private WebSocket webSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        etUserId = findViewById(R.id.etUserId);
        btnConnect = findViewById(R.id.btnConnect);
        tvStatus = findViewById(R.id.tvStatus);

        okHttpClient = new OkHttpClient();

        btnConnect.setOnClickListener(view -> {
            String userId = etUserId.getText().toString().trim();
            if (userId.isEmpty()) {
                tvStatus.setText("Please enter a userId.");
                return;
            }
            tvStatus.setText("Connecting to server...");
            connectWebSocket(userId);
        });
    }

    private void connectWebSocket(String userId) {
        Request request = new Request.Builder().url(WS_URL).build();
        webSocket = okHttpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.d(TAG, "WebSocket opened for userId: " + userId);
                runOnUiThread(() -> tvStatus.setText("WebSocket connected!"));

                // Identify ourselves with userId
                try {
                    JSONObject initMsg = new JSONObject();
                    initMsg.put("user_id", userId);
                    webSocket.send(initMsg.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // Move to MainActivity after successful connection
                runOnUiThread(() -> {
                    Intent intent = new Intent(ConnectActivity.this, MainActivity.class);
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                Log.d(TAG, "onMessage: " + text);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e(TAG, "WebSocket Failure: " + t.getMessage());
                runOnUiThread(() -> tvStatus.setText("WS Failure: " + t.getMessage()));
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket closed: " + reason);
                runOnUiThread(() -> tvStatus.setText("WebSocket closed"));
            }
        });
    }
}
