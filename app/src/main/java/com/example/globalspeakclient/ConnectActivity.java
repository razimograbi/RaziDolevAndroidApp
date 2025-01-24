package com.example.globalspeakclient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.Call;
import okhttp3.Callback;

/**
 * This Activity is responsible for connecting the user to the WebSocket.
 * Once connected, it navigates to MainActivity.
 */
public class ConnectActivity extends AppCompatActivity {
    private static final String TAG = "ConnectActivity";

    private static final String BASE_URL = "http://10.0.0.16:8000";
    private static final String WS_URL = "ws://10.0.0.16:8000/ws";

    private OkHttpClient okHttpClient;
    private RecyclerView recyclerView;
    private ActiveUsersAdapter usersAdapter;
    private List<ActiveUser> activeUsers = new ArrayList<>();
    private ActiveUser selectedUser;
    private WebSocket webSocket;
    private String uid;
    private User user;
    FirestoreService firestoreService = new FirestoreService();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        uid = getIntent().getStringExtra("uid");
        Button btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> logOutUser());
        Button btnProceed = findViewById(R.id.btnProceed);
        Button btnRefresh = findViewById(R.id.btnRefresh);

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerViewUsers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        okHttpClient = new OkHttpClient();
        // Call method to fetch user details
        fetchUserDetails(uid);
        btnProceed.setOnClickListener(v -> proceedToMainActivity());
        btnRefresh.setOnClickListener(v -> {
            fetchActiveUsers(); // Refreshes the test users for now
        });
        // Initialize adapter
        usersAdapter = new ActiveUsersAdapter(activeUsers, user -> {
            if (user == null) {
                Log.d(TAG, "No user selected. Waiting for a call.");
                selectedUser = null;
            } else {
                Log.d(TAG, "Selected user: " + user.getProfileName());
                selectedUser = user;
            }
        });
        recyclerView.setAdapter(usersAdapter);

        recyclerView.setAdapter(usersAdapter);
    }

    private void proceedToMainActivity() {
        Intent intent = new Intent(ConnectActivity.this, MainActivity.class);

        // Always pass my own details from the fetched user object
        intent.putExtra("userId", uid);
        intent.putExtra("language", user.getLanguage());

        if (selectedUser != null) {
            // If calling someone, also pass their details
            intent.putExtra("target_user_id", selectedUser.getUserId());
            intent.putExtra("target_profile_name", selectedUser.getProfileName());
            intent.putExtra("target_email", selectedUser.getEmail());
            Log.d(TAG, "Proceeding with call to: " + selectedUser.getProfileName());
        } else {
            Log.d(TAG, "Proceeding without selecting a user (waiting for call).");
        }

        startActivity(intent);
    }


    /**
     * Fetch user details from Firestore.
     */
    private void fetchUserDetails(String uid) {

        if (uid == null || uid.isEmpty()) {
            handleFetchError(null);
            return;
        }

        firestoreService.getUserByUid(uid, new FirestoreService.OnUserFetchedListener() {
            @Override
            public void onUserFetched(User fetchedUser) {
                if (fetchedUser != null) {
                    user = fetchedUser;
                    Log.d(TAG, "User details retrieved successfully.");
                    runOnUiThread(() -> {
                        TextView tvWelcome = findViewById(R.id.tvWelcome);
                        if (tvWelcome != null) {
                            tvWelcome.setText("Welcome, " + user.getProfileName());
                        }
                    });
                    connectWebSocket(user, uid); // Connect to WebSocket immediately
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Failed to fetch user", e);
                handleFetchError(e);
            }
        });
    }

    private void connectWebSocket(User user, String uid) {
        if (user == null) {
            Log.e(TAG, "Cannot connect to WebSocket: User details are null.");
            return;
        }
        Request request = new Request.Builder().url(WS_URL).build();
        webSocket = okHttpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.d(TAG, "WebSocket opened for userId: " + uid);
                // Identify ourselves with userId
                try {
                    JSONArray embeddingArray = new JSONArray(user.getEmbedding());
                    JSONArray gptCondLatentArray = new JSONArray(user.getGptCondLatent());
                    JSONObject initMsg = new JSONObject();
                    initMsg.put("user_id", uid);
                    initMsg.put("language", user.getLanguage());
                    initMsg.put("profile_name", user.getProfileName());
                    initMsg.put("email", user.getEmail());
                    initMsg.put("embedding", embeddingArray);
                    initMsg.put("gpt_cond_latent", gptCondLatentArray);
                    webSocket.send(initMsg.toString());
                    Log.d(TAG, "Sent user details to WebSocket: " + initMsg);
                    fetchActiveUsers();
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to convert user embedding/gpt_cond_latent", e);
                }

            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                Log.d(TAG, "onMessage: " + text);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e(TAG, "WebSocket Failure: " + t.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(ConnectActivity.this, "Connection error. Please log in again.", Toast.LENGTH_LONG).show();
                    logOutUser();
                });
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket closed: " + reason);
                runOnUiThread(() -> {
                    Toast.makeText(ConnectActivity.this, "Connection closed. Please log in again.", Toast.LENGTH_LONG).show();
                    logOutUser();
                });
            }
        });
    }


    /**
     * Handles Firestore fetch failure: Shows error, logs out user, redirects to login.
     */
    private void handleFetchError(Exception e) {
        String errorMessage = ErrorHandler.getFirestoreErrorMessage(e);
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        logOutUser(); // Log the user out
    }

    /**
     * Logs the user out and redirects to LoginActivity.
     */
    private void logOutUser() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    /**
     * Fetch active users from the Python server.
     */
    private void fetchActiveUsers() {
        String url = BASE_URL + "/active_users";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to fetch active users: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Server returned error: " + response.code());
                    return;
                }
                String responseData = response.body().string();
                try {
                    JSONArray jsonArray = new JSONArray(responseData);
                    List<ActiveUser> users = new ArrayList<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonUser = jsonArray.getJSONObject(i);
                        users.add(new ActiveUser(
                                jsonUser.getString("user_id"),
                                jsonUser.getString("full_name"),
                                jsonUser.getString("email")
                        ));
                    }

                    runOnUiThread(() -> {
                        activeUsers.clear();
                        activeUsers.addAll(users);
                        usersAdapter.updateUserList(activeUsers);
                    });

                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing JSON", e);
                }
            }
        });
    }
}



