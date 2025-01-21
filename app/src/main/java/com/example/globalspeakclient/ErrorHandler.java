package com.example.globalspeakclient;

public class ErrorHandler {
    public static String getFirebaseErrorMessage(Exception exception) {
        if (exception != null && exception.getMessage() != null) {
            String errorMessage = exception.getMessage();
            if (errorMessage.contains("PASSWORD_DOES_NOT_MEET_REQUIREMENTS")) {
                return "Password must be 6+ characters, with a number and an uppercase letter.";
            } else if (exception instanceof com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                return "This email is already in use.";
            } else if (exception instanceof com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
                return "Invalid email format.";
            }
        }
        return "An error occurred: " + (exception != null ? exception.getMessage() : "Unknown error.");
    }
}
