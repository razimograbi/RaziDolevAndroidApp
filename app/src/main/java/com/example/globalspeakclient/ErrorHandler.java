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
    /**
     * Returns a general error message for Firestore failures.
     */
    public static String getFirestoreErrorMessage(Exception exception) {
        return "Error retrieving user data. Please log in again.";
    }

    /**
     * Returns a friend list-specific error message.
     */
    public static String getFriendListErrorMessage(Exception exception) {
        return "Error updating friend list. Please try again.";
    }
}
