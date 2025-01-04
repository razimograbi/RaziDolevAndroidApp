package com.example.globalspeakclient;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
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
    private static final String WS_URL = "ws://10.0.0.15:8000/ws";

    private EditText etUserId;
    private Spinner spinnerLanguages;
    private Button btnConnect;
    private TextView tvStatus;

    private OkHttpClient okHttpClient;
    private WebSocket webSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        etUserId = findViewById(R.id.etUserId);
        spinnerLanguages = findViewById(R.id.spinner_languages);
        btnConnect = findViewById(R.id.btnConnect);
        tvStatus = findViewById(R.id.tvStatus);

        okHttpClient = new OkHttpClient();

        // =================== Populate Spinner and add protective listeners ===============================

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.languages_array, android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguages.setAdapter(adapter);

        // Watch for changes
        etUserId.addTextChangedListener(textWatcher);
        spinnerLanguages.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(
                    AdapterView<?> parent, View view, int position, long id
            ) {
                checkFormValid();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        btnConnect.setOnClickListener(view -> {
            String userId = etUserId.getText().toString().trim();
            String language = spinnerLanguages.getSelectedItem().toString();
            if (userId.isEmpty()) {
                tvStatus.setText("Please enter a userId.");
                return;
            }
            tvStatus.setText("connecting as " + userId + " with language " + language + "...");
            tvStatus.setVisibility(View.VISIBLE);
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


    // Enable the button only if both User ID and language are selected
    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            checkFormValid();
        }

        @Override
        public void afterTextChanged(Editable s) { }
    };

    private void checkFormValid() {
        String userIdText = etUserId.getText().toString().trim();
        // Check if user entered something and selected a non-default language
        boolean isUserIdNotEmpty = !userIdText.isEmpty();
        boolean isLanguageSelected = spinnerLanguages.getSelectedItemPosition() != AdapterView.INVALID_POSITION;
        btnConnect.setEnabled(isUserIdNotEmpty && isLanguageSelected);
    }
}


