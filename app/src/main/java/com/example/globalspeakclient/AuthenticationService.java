package com.example.globalspeakclient;

import android.util.Log;
import java.io.File;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


/**
 * Handles user authentication using Firebase Authentication.
 * This service provides:
 * - User sign-up with audio file or embedding.
 * - User sign-in.
 * - User validation by creating a temporary account.
 * Calls `FirestoreService` to store user details after successful authentication.
 */
public class AuthenticationService {

    private static final String TAG = "AuthenticationService";
    private final FirebaseAuth mAuth;

    public AuthenticationService() {
        mAuth = FirebaseAuth.getInstance();
    }

    // Validate user by creating and deleting a temporary account
    public void validateUser(String email, String password, OnAuthResultListener listener) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser tempUser = mAuth.getCurrentUser();
                        if (tempUser != null) {
                            tempUser.delete().addOnCompleteListener(deleteTask -> {
                                if (deleteTask.isSuccessful()) {
                                    listener.onSuccess(tempUser);
                                } else {
                                    Log.e(TAG, "Error deleting temporary user: ", deleteTask.getException());
                                    listener.onFailure(deleteTask.getException());
                                }
                            });
                        }
                    } else {
                        Log.e(TAG, "Validation error: ", task.getException());
                        listener.onFailure(task.getException());
                    }
                });
    }
    // signUp method
   /* public void signUp(User user, File audioFile, OnAuthResultListener listener) {
        mAuth.createUserWithEmailAndPassword(user.getEmail(), user.getPassword())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String userId = firebaseUser.getUid();

                            // Save user and audio in FirestoreService
                            FirestoreService firestoreService = new FirestoreService();
                            firestoreService.saveUser(userId, user, audioFile, new FirestoreService.FirestoreCallback() {
                                @Override
                                public void onSuccess() {
                                    listener.onSuccess(firebaseUser); // Pass FirebaseUser on success
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    Log.e(TAG, "Failed to save user with audio for userID: " + userId, e);
                                    listener.onFailure(e); // Handle failure
                                }
                            });
                        } else {
                            listener.onFailure(new Exception("User creation failed: FirebaseUser is null."));
                        }
                    } else {
                        Log.e(TAG, "Sign up error: ", task.getException());
                        listener.onFailure(task.getException()); // Handle Firebase Auth failure
                    }
                });
    }*/

    // signUp method
    public void signUp(User user, OnAuthResultListener listener) {
        mAuth.createUserWithEmailAndPassword(user.getEmail(), user.getPassword())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String userId = firebaseUser.getUid();

                            // Save user and embedding in Firestore
                            FirestoreService firestoreService = new FirestoreService();
                            firestoreService.saveUser(userId, user, new FirestoreService.FirestoreCallback() {
                                @Override
                                public void onSuccess() {
                                    listener.onSuccess(firebaseUser);
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    Log.e(TAG, "Failed to save user with embedding for userID: " + userId, e);
                                    listener.onFailure(e);
                                }
                            });
                        } else {
                            listener.onFailure(new Exception("User creation failed: FirebaseUser is null."));
                        }
                    } else {
                        Log.e(TAG, "Sign up error: ", task.getException());
                        listener.onFailure(task.getException());
                    }
                });
    }


    // Sign-in method
    public void signIn(String email, String password, OnAuthResultListener listener) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        listener.onSuccess(user);
                    } else {
                        Log.e(TAG, "Sign-in error: ", task.getException());
                        listener.onFailure(task.getException());
                    }
                });
    }

    // Listener interface for Firebase Authentication results
    public interface OnAuthResultListener {
        void onSuccess(FirebaseUser user);
        void onFailure(Exception exception);
    }
}
