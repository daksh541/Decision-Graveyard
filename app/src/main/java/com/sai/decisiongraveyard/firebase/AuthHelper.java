package com.sai.decisiongraveyard.firebase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuthHelper {

    public interface AuthCallback {
        void onSuccess(@Nullable FirebaseUser user);

        void onError(@NonNull String message);
    }

    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;

    public AuthHelper() {
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
    }

    @Nullable
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    public boolean isUserLoggedIn() {
        return getCurrentUser() != null;
    }

    public void login(String email, String password, @NonNull AuthCallback callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user == null) {
                        firebaseAuth.signOut();
                        callback.onError("Login failed. Please try again.");
                        return;
                    }
                    upsertUserDocument(user, callback);
                })
                .addOnFailureListener(exception -> handleLoginError(email, exception, callback));
    }

    public void register(String email, String password, @NonNull AuthCallback callback) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user == null) {
                        firebaseAuth.signOut();
                        callback.onError("Couldn't create your account. Please try again.");
                        return;
                    }
                    upsertUserDocument(user, callback);
                })
                .addOnFailureListener(exception -> callback.onError(mapAuthError(exception)));
    }

    public void logout() {
        firebaseAuth.signOut();
    }

    public void resetPassword(String email, @NonNull AuthCallback callback) {
        firebaseAuth.fetchSignInMethodsForEmail(email)
                .addOnSuccessListener(result -> {
                    List<String> methods = result.getSignInMethods();
                    boolean supportsPassword = methods != null && (
                            methods.contains(EmailAuthProvider.EMAIL_PASSWORD_SIGN_IN_METHOD)
                                    || methods.contains(EmailAuthProvider.EMAIL_LINK_SIGN_IN_METHOD)
                    );
                    boolean googleOnly = methods != null
                            && methods.contains(GoogleAuthProvider.GOOGLE_SIGN_IN_METHOD)
                            && !supportsPassword;

                    if (googleOnly) {
                        callback.onError("This account uses Google sign-in. Use the Google button to continue.");
                        return;
                    }

                    firebaseAuth.sendPasswordResetEmail(email)
                            .addOnSuccessListener(unused -> callback.onSuccess(null))
                            .addOnFailureListener(exception -> callback.onError(mapAuthError(exception)));
                })
                .addOnFailureListener(exception ->
                        firebaseAuth.sendPasswordResetEmail(email)
                                .addOnSuccessListener(unused -> callback.onSuccess(null))
                                .addOnFailureListener(resetException -> callback.onError(mapAuthError(resetException))));
    }

    public void signInWithGoogle(GoogleSignInAccount account, @NonNull AuthCallback callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user == null) {
                        firebaseAuth.signOut();
                        callback.onError("Google sign-in failed. Please try again.");
                        return;
                    }
                    upsertUserDocument(user, callback);
                })
                .addOnFailureListener(exception -> callback.onError(mapAuthError(exception)));
    }

    private void upsertUserDocument(@NonNull FirebaseUser user, @NonNull AuthCallback callback) {
        String email = user.getEmail();
        if (email == null || email.trim().isEmpty()) {
            firebaseAuth.signOut();
            callback.onError("Your account is missing an email address. Please try again.");
            return;
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", user.getUid());
        userData.put("email", email.trim());

        firestore.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        userData.put("createdAt", FieldValue.serverTimestamp());
                    }

                    firestore.collection("users")
                            .document(user.getUid())
                            .set(userData, SetOptions.merge())
                            .addOnSuccessListener(unused -> callback.onSuccess(user))
                            .addOnFailureListener(exception -> {
                                firebaseAuth.signOut();
                                callback.onError(mapFirestoreError(exception));
                            });
                })
                .addOnFailureListener(exception -> {
                    firebaseAuth.signOut();
                    callback.onError(mapFirestoreError(exception));
                });
    }

    private void handleLoginError(
            @NonNull String email,
            @NonNull Exception exception,
            @NonNull AuthCallback callback
    ) {
        if (!(exception instanceof FirebaseAuthException)) {
            callback.onError(mapAuthError(exception));
            return;
        }

        String errorCode = ((FirebaseAuthException) exception).getErrorCode();
        boolean shouldCheckProviders =
                "ERROR_INVALID_CREDENTIAL".equals(errorCode)
                        || "ERROR_INVALID_LOGIN_CREDENTIALS".equals(errorCode)
                        || "ERROR_WRONG_PASSWORD".equals(errorCode)
                        || "ERROR_USER_NOT_FOUND".equals(errorCode);

        if (!shouldCheckProviders) {
            callback.onError(mapAuthError(exception));
            return;
        }

        firebaseAuth.fetchSignInMethodsForEmail(email)
                .addOnSuccessListener(result -> {
                    List<String> methods = result.getSignInMethods();
                    if (methods == null || methods.isEmpty()) {
                        callback.onError("We couldn't verify password sign-in for this email. If this is your Google account, use the Google button to continue.");
                        return;
                    }

                    boolean supportsPassword = methods.contains(EmailAuthProvider.EMAIL_PASSWORD_SIGN_IN_METHOD)
                            || methods.contains(EmailAuthProvider.EMAIL_LINK_SIGN_IN_METHOD);
                    boolean googleOnly = methods.contains(GoogleAuthProvider.GOOGLE_SIGN_IN_METHOD) && !supportsPassword;

                    if (googleOnly) {
                        callback.onError("This account uses Google sign-in. Use the Google button to continue.");
                        return;
                    }

                    if (!supportsPassword) {
                        callback.onError("This email is not set up for password login. Try Google sign-in or reset your password.");
                        return;
                    }

                    callback.onError("Incorrect email or password. If you created this account with Google, use the Google button.");
                })
                .addOnFailureListener(fetchException ->
                        callback.onError("We couldn't verify the sign-in method for this email. If you usually use Google, continue with the Google button."));
    }

    private String mapAuthError(@NonNull Exception exception) {
        if (exception instanceof FirebaseAuthWeakPasswordException) {
            return "Password must be at least 6 characters.";
        }
        if (exception instanceof FirebaseAuthUserCollisionException) {
            return "An account already exists with this email.";
        }
        if (exception instanceof FirebaseNetworkException) {
            return "Network error. Check your connection and try again.";
        }
        if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            String errorCode = ((FirebaseAuthInvalidCredentialsException) exception).getErrorCode();
            if ("ERROR_INVALID_EMAIL".equals(errorCode)) {
                return "Enter a valid email address.";
            }
            if ("ERROR_WRONG_PASSWORD".equals(errorCode)
                    || "ERROR_INVALID_CREDENTIAL".equals(errorCode)
                    || "ERROR_INVALID_LOGIN_CREDENTIALS".equals(errorCode)) {
                return "Incorrect email or password.";
            }
        }
        if (exception instanceof FirebaseAuthException) {
            String errorCode = ((FirebaseAuthException) exception).getErrorCode();
            if ("ERROR_USER_NOT_FOUND".equals(errorCode)) {
                return "No account found for this email.";
            }
            if ("ERROR_EMAIL_ALREADY_IN_USE".equals(errorCode)) {
                return "An account already exists with this email.";
            }
        }
        return exception.getMessage() == null
                ? "Authentication failed. Please try again."
                : exception.getMessage();
    }

    private String mapFirestoreError(@NonNull Exception exception) {
        if (exception instanceof FirebaseNetworkException) {
            return "Network error. Check your connection and try again.";
        }
        return exception.getMessage() == null
                ? "We couldn't finish setting up your account."
                : exception.getMessage();
    }
}
