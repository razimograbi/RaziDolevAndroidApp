package com.example.globalspeakclient;

import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;


import java.util.List;

/**
 * Handles interactions with Firebase Firestore.
 *
 * Responsibilities:
 * - Stores user details and embeddings.
 * - Retrieves user information by UID.
 * - Manages friend lists.
 * - Checks user existence by email.
 */
public class FirestoreService {

    private static final String TAG = "FirestoreService";
    private final FirebaseFirestore firestore;

    public FirestoreService() {
        firestore = FirebaseFirestore.getInstance();
    }


    /**
     * Saves the user details along with the generated embedding in Firestore.
     *
     * @param userId    The unique user ID.
     * @param user      The user object containing user details.
     * @param callback  Callback to handle success or failure.
     */
    public void saveUser(String userId, User user, FirestoreCallback callback) {

        firestore.collection("Users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User details and embedding saved successfully for: " + userId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save user details: ", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Retrieves user details using their UID.
     *
     * @param uid      User's unique ID.
     * @param listener Callback to return user data or an error.
     */
    public void getUserByUid(String uid, OnUserFetchedListener listener) {
        DocumentReference userRef = firestore.collection("Users").document(uid);
        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                DocumentSnapshot document = task.getResult();
                User user = document.toObject(User.class);
                listener.onUserFetched(user);
            } else {
                listener.onError(task.getException());
            }
        });
    }

    /**
     * Updates the user's friend list in Firestore.
     *
     * @param userId         Unique user ID.
     * @param updatedFriends List of updated friend UIDs.
     * @param callback       Callback to handle success or failure.
     */
    public void updateFriendList(String userId, List<String> updatedFriends, FirestoreCallback callback) {
        DocumentReference userRef = firestore.collection("Users").document(userId);

        userRef.update("friends", updatedFriends) //  Directly update Firestore
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirestoreService", "Friends list updated successfully.");
                    callback.onSuccess();
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Checks if a user exists in Firestore by their email.
     *
     * @param friendEmail User's email to check.
     * @param listener    Callback for existence check result.
     */
    public void checkUserExists(String friendEmail, OnUserCheckListener listener) {
        firestore.collection("Users")
                .whereEqualTo("email", friendEmail)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        listener.onUserExists();
                    } else {
                        listener.onUserNotFound();
                    }
                })
                .addOnFailureListener(listener::onFailure);
    }

    public interface OnUserCheckListener {
        void onUserExists();
        void onUserNotFound();
        void onFailure(Exception e);
    }

    public interface FirestoreCallback {
        void onSuccess();
        void onFailure(Exception e);
    }
    public interface OnUserFetchedListener {
        void onUserFetched(User user);
        void onError(Exception e);
    }
}
