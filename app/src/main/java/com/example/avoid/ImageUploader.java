package com.example.avoid;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

import java.util.HashMap;
import java.util.Map;

/**
 * Thin wrapper around Cloudinary's MediaManager.
 *
 * <p>Credentials come from {@code BuildConfig} (read from {@code local.properties} at build
 * time — the file is gitignored). Initialization is idempotent and lazy.
 */
public final class ImageUploader {

    private static final String TAG = "ImageUploader";
    private static volatile boolean initialized = false;

    private ImageUploader() {}

    public interface Callback {
        void onSuccess(@NonNull String secureUrl);
        void onFailure(@NonNull String message);
    }

    public static synchronized void init(@NonNull Context context) {
        if (initialized) return;
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", BuildConfig.CLOUDINARY_CLOUD_NAME);
        config.put("api_key",    BuildConfig.CLOUDINARY_API_KEY);
        config.put("api_secret", BuildConfig.CLOUDINARY_API_SECRET);
        try {
            MediaManager.init(context.getApplicationContext(), config);
        } catch (IllegalStateException alreadyInit) {
            // Already initialized in this process — fine.
        }
        initialized = true;
    }

    public static void uploadImage(@NonNull Context context, @NonNull Uri uri,
                                   @NonNull String folder, @NonNull Callback callback) {
        init(context);
        MediaManager.get().upload(uri)
                .option("folder", folder)
                .option("resource_type", "image")
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        Object url = resultData.get("secure_url");
                        if (url == null) url = resultData.get("url");
                        if (url == null) {
                            callback.onFailure("Upload completed but no URL returned");
                            return;
                        }
                        callback.onSuccess(url.toString());
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Log.e(TAG, "Cloudinary upload error: " + error.getDescription());
                        callback.onFailure(error.getDescription() != null ? error.getDescription() : "Upload failed");
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        Log.w(TAG, "Cloudinary upload rescheduled: " + error.getDescription());
                    }
                })
                .dispatch();
    }
}
