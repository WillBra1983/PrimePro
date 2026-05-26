package com.seuprojeto.primeprofast;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Login alinhado ao Salvation: Firebase Auth (Google nativo + e-mail/senha) e índice {@code userSearch/{uid}}
 * no Realtime Database para listagem de usuários (qualquer usuário autenticado pode ler, como nas regras típicas do Salvation).
 */
public class PrimeProFirebaseAuth {

    public interface AuthListener {
        void onUserChanged(@Nullable FirebaseUser user);
    }

    public interface UserListCallback {
        void onResult(@NonNull List<UserRow> rows, @Nullable String errorMessage);
    }

    public static final class UserRow {
        public final String uid;
        public final String email;
        public final String displayName;

        UserRow(String uid, String email, String displayName) {
            this.uid = uid != null ? uid : "";
            this.email = email != null ? email : "";
            this.displayName = displayName != null ? displayName : "";
        }

        @NonNull
        @Override
        public String toString() {
            String dn = displayName.isEmpty() ? "(sem nome)" : displayName;
            String em = email.isEmpty() ? "(sem e-mail)" : email;
            return dn + " — " + em + "\nuid: " + uid;
        }
    }

    private final Activity activity;
    private final ActivityResultLauncher<Intent> googleLauncher;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private volatile GoogleSignInClient googleClient;
    @Nullable
    private AuthListener listener;
    private boolean authListenerRegistered;

    public PrimeProFirebaseAuth(
            @NonNull Activity activity,
            @NonNull ActivityResultLauncher<Intent> googleLauncher) {
        this.activity = activity;
        this.googleLauncher = googleLauncher;
    }

    public void setListener(@Nullable AuthListener listener) {
        this.listener = listener;
    }

    /** Regista o listener de sessão uma única vez (idempotente). */
    public void start() {
        if (authListenerRegistered) {
            return;
        }
        authListenerRegistered = true;
        auth.addAuthStateListener(fb -> {
            FirebaseUser user = fb.getCurrentUser();
            if (user != null) {
                publishUserSearchRow(user);
            }
            if (listener != null) {
                listener.onUserChanged(user);
            }
        });
    }

    @Nullable
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    /** Mesmo {@code userSearch} do Salvation — mantém lista pesquisável / listável. */
    public void publishUserSearchRow(@NonNull FirebaseUser user) {
        String dbUrl = activity.getString(R.string.firebase_database_url);
        if (TextUtils.isEmpty(dbUrl)) {
            return;
        }
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("userSearch")
                .child(user.getUid());
        Map<String, Object> patch = new HashMap<>();
        if (user.getEmail() != null) {
            patch.put("email", user.getEmail());
        }
        if (user.getDisplayName() != null) {
            patch.put("displayName", user.getDisplayName());
        }
        if (!patch.isEmpty()) {
            ref.updateChildren(patch);
        }
    }

    public void beginGoogleSignIn() {
        String webId = activity.getString(R.string.google_web_client_id).trim();
        if (TextUtils.isEmpty(webId)) {
            toast("Defina google_web_client_id em firebase_env.xml (cliente OAuth Web).");
            return;
        }
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webId)
                .requestEmail()
                .build();
        googleClient = GoogleSignIn.getClient(activity, gso);
        googleLauncher.launch(googleClient.getSignInIntent());
    }

    public void handleGoogleActivityResult(@Nullable Intent data) {
        if (data == null) {
            return;
        }
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account == null) {
                toast("Conta Google inválida.");
                return;
            }
            String idToken = account.getIdToken();
            if (TextUtils.isEmpty(idToken)) {
                toast("Google não devolveu idToken; confira o Web client ID no Firebase.");
                return;
            }
            AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
            auth.signInWithCredential(credential)
                    .addOnCompleteListener(activity, t -> {
                        if (t.isSuccessful()) {
                            toast("Conectado.");
                        } else {
                            toast("Falha Firebase: " +
                                    (t.getException() != null ? t.getException().getMessage() : "erro"));
                        }
                    });
        } catch (ApiException e) {
            if (e.getStatusCode() != 12501) { // cancelado pelo usuário
                toast("Google Sign-In: " + e.getMessage());
            }
        }
    }

    public void signInEmailPassword(@NonNull String email, @NonNull String password) {
        auth.signInWithEmailAndPassword(email.trim(), password)
                .addOnCompleteListener(activity, t -> {
                    if (t.isSuccessful()) {
                        toast("Conectado.");
                    } else {
                        toast("Login: " + (t.getException() != null ? t.getException().getMessage() : "erro"));
                    }
                });
    }

    public void registerEmailPassword(@NonNull String email, @NonNull String password) {
        if (password.length() < 6) {
            toast("Senha precisa de pelo menos 6 caracteres.");
            return;
        }
        auth.createUserWithEmailAndPassword(email.trim(), password)
                .addOnCompleteListener(activity, t -> {
                    if (t.isSuccessful()) {
                        toast("Conta criada; você já está conectado.");
                    } else {
                        toast("Cadastro: " + (t.getException() != null ? t.getException().getMessage() : "erro"));
                    }
                });
    }

    public void signOut() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build();
        GoogleSignInClient client = GoogleSignIn.getClient(activity, gso);
        client.signOut().addOnCompleteListener(activity, t -> auth.signOut());
        toast("Sessão encerrada.");
    }

    /** Lê {@code userSearch} (mesmo nó usado no Salvation). */
    public void fetchUserList(@NonNull UserListCallback callback) {
        String dbUrl = activity.getString(R.string.firebase_database_url);
        if (TextUtils.isEmpty(dbUrl)) {
            callback.onResult(new ArrayList<>(), "database_url vazio em firebase_env.xml.");
            return;
        }
        if (auth.getCurrentUser() == null) {
            callback.onResult(new ArrayList<>(), "Faça login para listar usuários.");
            return;
        }
        FirebaseDatabase.getInstance()
                .getReference("userSearch")
                .get()
                .addOnCompleteListener(activity, t -> {
                    if (!t.isSuccessful() || t.getResult() == null) {
                        callback.onResult(new ArrayList<>(), t.getException() != null
                                ? t.getException().getMessage()
                                : "Falha ao ler userSearch.");
                        return;
                    }
                    DataSnapshot snap = t.getResult();
                    List<UserRow> rows = new ArrayList<>();
                    for (DataSnapshot child : snap.getChildren()) {
                        String uid = child.getKey() != null ? child.getKey() : "";
                        String em = child.child("email").getValue(String.class);
                        String dn = child.child("displayName").getValue(String.class);
                        rows.add(new UserRow(uid, em, dn));
                    }
                    callback.onResult(rows, null);
                });
    }

    private void toast(String msg) {
        Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
    }
}
