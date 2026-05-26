package com.seuprojeto.primeprofast;

import android.content.Context;
import android.text.TextUtils;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

/**
 * Inicializa o Firebase a partir de {@code res/values/firebase_env.xml}, no mesmo espírito do Salvation
 * (variáveis de ambiente), sem obrigar {@code google-services.json} no repositório.
 */
public final class FirebaseBootstrap {

    private FirebaseBootstrap() {}

    /** {@code true} se as strings mínimas estiverem preenchidas e o app default tiver sido criado. */
    public static boolean ensureInitialized(Context context) {
        Context app = context.getApplicationContext();
        if (!FirebaseApp.getApps(app).isEmpty()) {
            return true;
        }
        String projectId = safeString(app, R.string.firebase_project_id);
        String appId = safeString(app, R.string.firebase_application_id);
        String apiKey = safeString(app, R.string.firebase_api_key);
        if (TextUtils.isEmpty(projectId) || TextUtils.isEmpty(appId) || TextUtils.isEmpty(apiKey)) {
            return false;
        }
        FirebaseOptions.Builder b = new FirebaseOptions.Builder()
                .setProjectId(projectId)
                .setApplicationId(appId)
                .setApiKey(apiKey);
        String dbUrl = safeString(app, R.string.firebase_database_url);
        if (!TextUtils.isEmpty(dbUrl)) {
            b.setDatabaseUrl(dbUrl);
        }
        String bucket = safeString(app, R.string.firebase_storage_bucket);
        if (!TextUtils.isEmpty(bucket)) {
            b.setStorageBucket(bucket);
        }
        FirebaseApp.initializeApp(app, b.build());
        return true;
    }

    public static boolean isConfigured(Context context) {
        String projectId = safeString(context, R.string.firebase_project_id);
        String appId = safeString(context, R.string.firebase_application_id);
        String apiKey = safeString(context, R.string.firebase_api_key);
        return !TextUtils.isEmpty(projectId) && !TextUtils.isEmpty(appId) && !TextUtils.isEmpty(apiKey);
    }

    private static String safeString(Context c, int resId) {
        String s = c.getString(resId);
        return s != null ? s.trim() : "";
    }
}
