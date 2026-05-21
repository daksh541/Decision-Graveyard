package com.sai.decisiongraveyard.firebase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.sai.decisiongraveyard.model.UserPreferences;
import com.sai.decisiongraveyard.model.UserProfile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AuthHelper {

    public interface AuthCallback {
        void onSuccess(@Nullable FirebaseUser user);

        void onError(@NonNull String message);
    }

    private static final String USERS_COLLECTION = "users";
    private static final String PROFILES_COLLECTION = "userProfiles";
    private static final String PREFERENCES_COLLECTION = "userPreferences";

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

    public void login(@NonNull String email, @NonNull String password, @NonNull AuthCallback callback) {
        String normalizedEmail = normalizeEmail(email);
        resolveProviderStatus(normalizedEmail, new ProviderStatusCallback() {
            @Override
            public void onResolved(@NonNull ProviderStatus status) {
                if (status.googleOnly) {
                    callback.onError("This account uses Google sign-in. Continue with Google instead of password.");
                    return;
                }
                if (status.hasKnownProviders && !status.supportsPassword) {
                    callback.onError("This email is not set up for password login yet. Use Google sign-in or reset your password.");
                    return;
                }
                signInWithEmail(normalizedEmail, password, callback);
            }

            @Override
            public void onError() {
                signInWithEmail(normalizedEmail, password, callback);
            }
        });
    }

    public void register(@NonNull String email, @NonNull String password, @NonNull AuthCallback callback) {
        String normalizedEmail = normalizeEmail(email);
        resolveProviderStatus(normalizedEmail, new ProviderStatusCallback() {
            @Override
            public void onResolved(@NonNull ProviderStatus status) {
                if (status.supportsPassword) {
                    callback.onError("An account already exists with this email. Sign in instead.");
                    return;
                }
                if (status.googleOnly) {
                    callback.onError("This email already uses Google sign-in. Continue with Google to avoid duplicate accounts.");
                    return;
                }
                firebaseAuth.createUserWithEmailAndPassword(normalizedEmail, password)
                        .addOnSuccessListener(authResult -> bootstrapAuthenticatedUser(authResult.getUser(), true, callback))
                        .addOnFailureListener(exception -> callback.onError(mapAuthError(exception)));
            }

            @Override
            public void onError() {
                firebaseAuth.createUserWithEmailAndPassword(normalizedEmail, password)
                        .addOnSuccessListener(authResult -> bootstrapAuthenticatedUser(authResult.getUser(), true, callback))
                        .addOnFailureListener(exception -> callback.onError(mapAuthError(exception)));
            }
        });
    }

    public void signInWithGoogle(@NonNull GoogleSignInAccount account, @NonNull AuthCallback callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> bootstrapAuthenticatedUser(authResult.getUser(), authResult.getAdditionalUserInfo() != null && authResult.getAdditionalUserInfo().isNewUser(), callback))
                .addOnFailureListener(exception -> handleGoogleSignInFailure(exception, callback));
    }

    public void resetPassword(@NonNull String email, @NonNull AuthCallback callback) {
        String normalizedEmail = normalizeEmail(email);
        resolveProviderStatus(normalizedEmail, new ProviderStatusCallback() {
            @Override
            public void onResolved(@NonNull ProviderStatus status) {
                if (status.googleOnly) {
                    callback.onError("This account uses Google sign-in. Password reset is only available for password accounts.");
                    return;
                }
                firebaseAuth.sendPasswordResetEmail(normalizedEmail)
                        .addOnSuccessListener(unused -> callback.onSuccess(null))
                        .addOnFailureListener(exception -> callback.onError(mapAuthError(exception)));
            }

            @Override
            public void onError() {
                firebaseAuth.sendPasswordResetEmail(normalizedEmail)
                        .addOnSuccessListener(unused -> callback.onSuccess(null))
                        .addOnFailureListener(exception -> callback.onError(mapAuthError(exception)));
            }
        });
    }

    public void logout() {
        firebaseAuth.signOut();
    }

    private void signInWithEmail(@NonNull String email, @NonNull String password, @NonNull AuthCallback callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> bootstrapAuthenticatedUser(authResult.getUser(), false, callback))
                .addOnFailureListener(exception -> handlePasswordLoginFailure(email, exception, callback));
    }

    private void bootstrapAuthenticatedUser(@Nullable FirebaseUser user, boolean isNewUser, @NonNull AuthCallback callback) {
        if (user == null) {
            callback.onError("Authentication completed, but the account could not be loaded. Try again.");
            return;
        }

        ensureUserDocuments(user, isNewUser, new BootstrapCallback() {
            @Override
            public void onSuccess() {
                callback.onSuccess(user);
            }

            @Override
            public void onError(@NonNull String message) {
                // The account is already authenticated. Let the app continue and allow repositories
                // to lazily create missing docs instead of trapping the user in auth.
                callback.onSuccess(user);
            }
        });
    }

    private void ensureUserDocuments(@NonNull FirebaseUser user, boolean isNewUser, @NonNull BootstrapCallback callback) {
        String email = normalizeEmail(user.getEmail());
        if (email.isEmpty()) {
            callback.onError("Authenticated account is missing an email address.");
            return;
        }

        Map<String, Object> baseUser = new HashMap<>();
        baseUser.put("userId", user.getUid());
        baseUser.put("email", email);
        baseUser.put("displayName", user.getDisplayName() == null ? "" : user.getDisplayName());
        baseUser.put("photoUrl", user.getPhotoUrl() == null ? "" : String.valueOf(user.getPhotoUrl()));
        baseUser.put("providers", getProviderIds(user));
        baseUser.put("lastSeenAt", FieldValue.serverTimestamp());
        if (isNewUser) {
            baseUser.put("createdAt", FieldValue.serverTimestamp());
        }

        firestore.collection(USERS_COLLECTION)
                .document(user.getUid())
                .set(baseUser, SetOptions.merge())
                .addOnSuccessListener(unused -> ensureProfileDocument(user, callback))
                .addOnFailureListener(exception -> callback.onError(mapFirestoreError(exception)));
    }

    private void ensureProfileDocument(@NonNull FirebaseUser user, @NonNull BootstrapCallback callback) {
        firestore.collection(PROFILES_COLLECTION)
                .document(user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        ensurePreferencesDocument(user, callback);
                        return;
                    }
                    UserProfile profile = new UserProfile(user.getUid());
                    firestore.collection(PROFILES_COLLECTION)
                            .document(user.getUid())
                            .set(profile)
                            .addOnSuccessListener(unused -> ensurePreferencesDocument(user, callback))
                            .addOnFailureListener(exception -> callback.onError(mapFirestoreError(exception)));
                })
                .addOnFailureListener(exception -> callback.onError(mapFirestoreError(exception)));
    }

    private void ensurePreferencesDocument(@NonNull FirebaseUser user, @NonNull BootstrapCallback callback) {
        firestore.collection(PREFERENCES_COLLECTION)
                .document(user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        callback.onSuccess();
                        return;
                    }
                    UserPreferences preferences = new UserPreferences(user.getUid());
                    firestore.collection(PREFERENCES_COLLECTION)
                            .document(user.getUid())
                            .set(preferences)
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(exception -> callback.onError(mapFirestoreError(exception)));
                })
                .addOnFailureListener(exception -> callback.onError(mapFirestoreError(exception)));
    }

    private void handlePasswordLoginFailure(@NonNull String email, @NonNull Exception exception, @NonNull AuthCallback callback) {
        if (!(exception instanceof FirebaseAuthException)) {
            callback.onError(mapAuthError(exception));
            return;
        }

        String errorCode = ((FirebaseAuthException) exception).getErrorCode();
        boolean shouldRecheckProviders =
                "ERROR_INVALID_CREDENTIAL".equals(errorCode)
                        || "ERROR_INVALID_LOGIN_CREDENTIALS".equals(errorCode)
                        || "ERROR_WRONG_PASSWORD".equals(errorCode)
                        || "ERROR_USER_NOT_FOUND".equals(errorCode);

        if (!shouldRecheckProviders) {
            callback.onError(mapAuthError(exception));
            return;
        }

        resolveProviderStatus(email, new ProviderStatusCallback() {
            @Override
            public void onResolved(@NonNull ProviderStatus status) {
                if (status.googleOnly) {
                    callback.onError("This account uses Google sign-in. Continue with Google instead of password.");
                    return;
                }
                if (!status.supportsPassword && status.hasKnownProviders) {
                    callback.onError("Password login is not enabled for this email. Use Google sign-in or create a password later.");
                    return;
                }
                if (!status.hasKnownProviders) {
                    callback.onError("No account was found for this email. Check the spelling or create a new account.");
                    return;
                }
                callback.onError("Incorrect email or password. If this email usually uses Google, continue with Google instead.");
            }

            @Override
            public void onError() {
                callback.onError(mapAuthError(exception));
            }
        });
    }

    private void handleGoogleSignInFailure(@NonNull Exception exception, @NonNull AuthCallback callback) {
        if (exception instanceof FirebaseAuthUserCollisionException) {
            String email = ((FirebaseAuthUserCollisionException) exception).getEmail();
            if (email != null && !email.trim().isEmpty()) {
                resolveProviderStatus(email, new ProviderStatusCallback() {
                    @Override
                    public void onResolved(@NonNull ProviderStatus status) {
                        if (status.supportsPassword && !status.googleOnly) {
                            callback.onError("This email already has a password account. Sign in with email first, then use the same account going forward.");
                            return;
                        }
                        callback.onError("This email is already attached to another sign-in method. Use the original provider first.");
                    }

                    @Override
                    public void onError() {
                        callback.onError("This email is already linked to another sign-in method.");
                    }
                });
                return;
            }
        }
        callback.onError(mapAuthError(exception));
    }

    private void resolveProviderStatus(@NonNull String email, @NonNull ProviderStatusCallback callback) {
        firebaseAuth.fetchSignInMethodsForEmail(email)
                .addOnSuccessListener(result -> callback.onResolved(new ProviderStatus(result.getSignInMethods())))
                .addOnFailureListener(exception -> callback.onError());
    }

    private String normalizeEmail(@Nullable String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.US);
    }

    private List<String> getProviderIds(@NonNull FirebaseUser user) {
        List<String> providers = new ArrayList<>();
        for (UserInfo info : user.getProviderData()) {
            if (info.getProviderId() == null || "firebase".equals(info.getProviderId())) {
                continue;
            }
            providers.add(info.getProviderId());
        }
        return providers;
    }

    private String mapAuthError(@NonNull Exception exception) {
        if (exception instanceof FirebaseAuthWeakPasswordException) {
            return "Password must be at least 6 characters.";
        }
        if (exception instanceof FirebaseAuthUserCollisionException) {
            return "An account already exists with this email. Try signing in instead.";
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
            if ("ERROR_TOO_MANY_REQUESTS".equals(errorCode)) {
                return "Too many attempts. Wait a moment before trying again.";
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
            return "Your account is ready, but profile sync failed because the network is unavailable.";
        }
        return exception.getMessage() == null
                ? "Your account is ready, but profile sync could not finish."
                : exception.getMessage();
    }

    private interface ProviderStatusCallback {
        void onResolved(@NonNull ProviderStatus status);

        void onError();
    }

    private interface BootstrapCallback {
        void onSuccess();

        void onError(@NonNull String message);
    }

    private static final class ProviderStatus {
        private final boolean supportsPassword;
        private final boolean supportsGoogle;
        private final boolean googleOnly;
        private final boolean hasKnownProviders;

        private ProviderStatus(@Nullable List<String> signInMethods) {
            List<String> methods = signInMethods == null ? new ArrayList<>() : signInMethods;
            this.supportsPassword = methods.contains(EmailAuthProvider.EMAIL_PASSWORD_SIGN_IN_METHOD)
                    || methods.contains(EmailAuthProvider.EMAIL_LINK_SIGN_IN_METHOD);
            this.supportsGoogle = methods.contains(GoogleAuthProvider.GOOGLE_SIGN_IN_METHOD);
            this.googleOnly = supportsGoogle && !supportsPassword;
            this.hasKnownProviders = !methods.isEmpty();
        }
    }
}
