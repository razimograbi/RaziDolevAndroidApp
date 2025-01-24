package com.example.globalspeakclient;

import android.net.Uri;
import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import com.google.firebase.storage.StorageReference;

import java.io.File;

/**
 * Handles interactions with Firebase Firestore and Firebase Storage.
 * This service is responsible for storing user details and embeddings.
 */
public class FirestoreService {

    private static final String TAG = "FirestoreService";
    private final FirebaseFirestore firestore;
    private final FirebaseStorage storage;

    public FirestoreService() {
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    /**
     * Saves the user details along with the uploaded audio file in Firebase.
     * - Uploads the audio file to Firebase Storage.
     * - Retrieves the storage URL and saves user details in Firestore.
     *
     * @param userId    The unique user ID.
     * @param user      The user object containing user details.
     * @param audioFile The recorded audio file to be uploaded.
     * @param callback  Callback to handle success or failure.
     */
   /* public void saveUser(String userId, User user, File audioFile, FirestoreCallback callback) {
        StorageReference audioRef = storage.getReference("user_audio/" + userId + "/" + audioFile.getName());

        audioRef.putFile(Uri.fromFile(audioFile))
                .addOnSuccessListener(taskSnapshot -> {
                    audioRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                user.setAudioFileUrl(uri.toString());
                                firestore.collection("Users").document(userId)
                                        .set(user)
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "User details saved successfully for: " + userId);
                                            callback.onSuccess();
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Failed to save user details: ", e);
                                            callback.onFailure(e);
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to retrieve audio URL: ", e);
                                callback.onFailure(e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to upload audio file: ", e);
                    callback.onFailure(e);
                });
    }*/

    /**
     * Saves the user details along with the generated embedding in Firestore.
     * - Does not upload audio, only stores the embedding.
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
     * Fetch user details from Firestore using UID.
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

    public interface FirestoreCallback {
        void onSuccess();
        void onFailure(Exception e);
    }
    public interface OnUserFetchedListener {
        void onUserFetched(User user);
        void onError(Exception e);
    }
}
