package com.example.globalspeakclient;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class AuthHelper {

    private static final String TAG = "AuthHelper";
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    public AuthHelper() {
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    // Sign-In Method
    public void signIn(String email, String password, OnAuthResultListener listener) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        listener.onSuccess(user);
                    } else {
                        Exception exception = task.getException();
                        listener.onFailure(exception);
                        Log.e(TAG, "Sign-In Error: ", exception);
                    }
                });
    }

    // Validate Firebase User Method (for temporary validation)
    // Validate Firebase User Method (for temporary validation)
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
                                    listener.onFailure(new Exception(getFirebaseErrorMessage(deleteTask.getException())));
                                    Log.e(TAG, "Error deleting temporary user: ", deleteTask.getException());
                                }
                            });
                        } else {
                            listener.onFailure(new Exception("Temporary user creation failed."));
                        }
                    } else {
                        listener.onFailure(new Exception(getFirebaseErrorMessage(task.getException())));
                        Log.e(TAG, "User Validation Error: ", task.getException());
                    }
                });
    }


    // Final Sign-Up Method (after all validations and steps)
    public void completeSignUp(String email, String password, String profileName, String language, String audioFileUrl, OnAuthResultListener listener) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String userId = firebaseUser.getUid();

                            // Create user data to store in Firestore
                            User user = new User(profileName, email, language, audioFileUrl);

                            // Store user data in Firestore
                            DocumentReference userRef = firestore.collection("Users").document(userId);
                            userRef.set(user)
                                    .addOnCompleteListener(dbTask -> {
                                        if (dbTask.isSuccessful()) {
                                            listener.onSuccess(firebaseUser);
                                        } else {
                                            listener.onFailure(dbTask.getException());
                                            Log.e(TAG, "Failed to save user details: ", dbTask.getException());
                                        }
                                    });
                        } else {
                            listener.onFailure(new Exception("FirebaseUser is null after final sign-up."));
                        }
                    } else {
                        listener.onFailure(task.getException());
                        Log.e(TAG, "Final Sign-Up Error: ", task.getException());
                    }
                });
    }
    // Method to parse Firebase exceptions and return user-friendly error messages
    // Method to parse Firebase exceptions and return user-friendly error messages
// Method to parse Firebase exceptions and return user-friendly error messages
    public String getFirebaseErrorMessage(Exception exception) {
        if (exception != null && exception.getMessage() != null) {
            String errorMessage = exception.getMessage();

            // Check for specific error cases
            if (errorMessage.contains("PASSWORD_DOES_NOT_MEET_REQUIREMENTS")) {
                return "Password must be 6+ characters, with a number and an uppercase letter.";
            } else if (exception instanceof com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                return "This email is already in use.";
            } else if (exception instanceof com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
                return "Invalid email format.";
            }
        }

        // Fallback for unknown errors
        return "An error occurred: " + (exception != null ? exception.getMessage() : "Unknown error.");
    }




    // Listener interface for authentication results
    public interface OnAuthResultListener {
        void onSuccess(FirebaseUser user);
        void onFailure(Exception exception);
    }

    // User class to store additional details
    public static class User {
        public String profileName;
        public String email;
        public String language;
        public String audioFileUrl;

        public User() {
            // Default constructor required for Firebase Firestore
        }

        public User(String profileName, String email, String language, String audioFileUrl) {
            this.profileName = profileName;
            this.email = email;
            this.language = language;
            this.audioFileUrl = audioFileUrl;
        }
    }
}
