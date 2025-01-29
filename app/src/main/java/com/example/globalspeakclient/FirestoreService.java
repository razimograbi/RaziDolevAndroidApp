package com.example.globalspeakclient;

import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;


import java.util.List;

/**
 * Handles interactions with Firebase Firestore and Firebase Storage.
 * This service is responsible for storing user details and embeddings.
 */
public class FirestoreService {

    private static final String TAG = "FirestoreService";
    private final FirebaseFirestore firestore;

    public FirestoreService() {
        firestore = FirebaseFirestore.getInstance();
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

    /**
     * Updates the friend list for the current user in Firestore.
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
     * Adds a friend to the user's friend list in Firestore.
     */
    /*public void addFriend(String userId, String friendEmail, FirestoreCallback callback) {
        DocumentReference userRef = firestore.collection("Users").document(userId);
        DocumentReference friendRef = firestore.collection("Users").document(friendEmail);

        // Check if the friend exists in Firestore
        friendRef.get().addOnCompleteListener(friendTask -> {
            if (friendTask.isSuccessful() && friendTask.getResult().exists()) {
                // Friend exists, now update user's friend list
                userRef.get().addOnCompleteListener(userTask -> {
                    if (userTask.isSuccessful() && userTask.getResult().exists()) {
                        User user = userTask.getResult().toObject(User.class);
                        if (user != null) {
                            List<String> friends = user.getFriends();
                            if (!friends.contains(friendEmail)) {
                                friends.add(friendEmail);
                                userRef.update("friends", friends)
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "Friend added successfully.");
                                            callback.onSuccess();
                                        })
                                        .addOnFailureListener(callback::onFailure);
                            } else {
                                Log.d(TAG, "Friend already added.");
                                callback.onFailure(new Exception("Friend is already in the list."));
                            }
                        }
                    } else {
                        callback.onFailure(userTask.getException());
                    }
                });
            } else {
                callback.onFailure(new Exception("Friend does not exist."));
            }
        });
    }*/

    /**
     * Removes a friend from the user's friend list in Firestore.
     */
    /*public void removeFriend(String userId, String friendEmail, FirestoreCallback callback) {
        DocumentReference userRef = firestore.collection("Users").document(userId);

        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                User user = task.getResult().toObject(User.class);
                if (user != null) {
                    List<String> friends = user.getFriends();
                    if (friends.contains(friendEmail)) {
                        friends.remove(friendEmail);
                        userRef.update("friends", friends)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Friend removed successfully.");
                                    callback.onSuccess();
                                })
                                .addOnFailureListener(callback::onFailure);
                    } else {
                        Log.d(TAG, "Friend not found in list.");
                        callback.onFailure(new Exception("Friend is not in your list."));
                    }
                }
            } else {
                callback.onFailure(task.getException());
            }
        });
    }*/
    /**
     * Checks if a user exists in Firestore based on their email.
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
