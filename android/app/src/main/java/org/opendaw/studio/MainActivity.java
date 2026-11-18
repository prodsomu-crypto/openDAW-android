package org.opendaw.studio;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.webkit.Profile;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Thin native host for the existing openDAW Studio web application.
 *
 * The DAW UI, project model, instruments, effects and WASM audio engine remain in
 * the original openDAW codebase. This class supplies Android's secure local-asset
 * origin, WebView compatibility checks, microphone permission flow and file picker.
 */
public final class MainActivity extends Activity {
    private static final String APP_ORIGIN = "https://appassets.androidplatform.net";
    private static final String PROFILE_NAME = "openDAW";
    private static final int FILE_CHOOSER_REQUEST = 4101;
    private static final int AUDIO_PERMISSION_REQUEST = 4102;

    private WebView webView;
    private ValueCallback<Uri[]> pendingFileCallback;
    private PermissionRequest pendingAudioRequest;

    private final WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
            .setDomain("appassets.androidplatform.net")
            .addPathHandler("/", new WebViewAssetLoader.AssetsPathHandler(this, "www"))
            .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(23, 25, 27));
        window.setNavigationBarColor(Color.rgb(23, 25, 27));

        webView = new WebView(this);

        // openDAW's WASM audio engine needs SharedArrayBuffer. The feature is
        // opt-in in modern Android System WebView versions; fail clearly if absent.
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)
                || !WebViewFeature.isFeatureSupported(WebViewFeature.CROSS_ORIGIN_ISOLATED_ALLOWLIST)) {
            showCompatibilityMessage();
            return;
        }

        try {
            // A profile must be selected before the WebView is attached or navigated.
            WebViewCompat.setProfile(webView, PROFILE_NAME);
            Profile profile = WebViewCompat.getProfile(webView);
            profile.setCrossOriginIsolatedAllowlist(Collections.singleton(APP_ORIGIN));
        } catch (UnsupportedOperationException | IllegalStateException error) {
            showCompatibilityMessage();
            return;
        }

        configureWebView();
        setContentView(webView);
        webView.loadUrl(APP_ORIGIN + "/index.html");
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setDefaultTextEncodingName("UTF-8");

        settings.setUserAgentString(settings.getUserAgentString() + " openDAWAndroid/0.1.0");
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setBackgroundColor(Color.rgb(23, 25, 27));

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                WebResourceResponse response = assetLoader.shouldInterceptRequest(request.getUrl());
                if (response == null) {
                    return null;
                }

                // This must be an HTTP response header, not a meta tag. Keep it on
                // local HTML only; the profile allowlist is restricted to this origin.
                if ("text/html".equalsIgnoreCase(response.getMimeType())) {
                    Map<String, String> headers = new HashMap<>();
                    if (response.getResponseHeaders() != null) {
                        headers.putAll(response.getResponseHeaders());
                    }
                    headers.put("Document-Isolation-Policy", "isolate-and-credentialless");
                    response.setResponseHeaders(headers);
                }
                return response;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String origin = uri.getScheme() + "://" + uri.getHost();
                if (APP_ORIGIN.equals(origin)) {
                    return false;
                }
                if ("https".equalsIgnoreCase(uri.getScheme())) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    } catch (Exception ignored) {
                        // No external browser is available; leave the DAW open.
                    }
                }
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(
                    WebView view,
                    ValueCallback<Uri[]> filePathCallback,
                    FileChooserParams fileChooserParams) {
                if (pendingFileCallback != null) {
                    pendingFileCallback.onReceiveValue(null);
                }
                pendingFileCallback = filePathCallback;

                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(
                        Intent.EXTRA_ALLOW_MULTIPLE,
                        fileChooserParams.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE
                );

                String[] acceptedTypes = fileChooserParams.getAcceptTypes();
                ArrayList<String> mimeTypes = new ArrayList<>();
                if (acceptedTypes != null) {
                    for (String acceptedType : acceptedTypes) {
                        if (acceptedType != null && !acceptedType.trim().isEmpty()
                                && acceptedType.contains("/")) {
                            mimeTypes.add(acceptedType.trim());
                        }
                    }
                }
                if (!mimeTypes.isEmpty()) {
                    intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes.toArray(new String[0]));
                }

                try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                    return true;
                } catch (Exception error) {
                    pendingFileCallback = null;
                    filePathCallback.onReceiveValue(null);
                    return false;
                }
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                String[] resources = request.getResources();
                if (resources.length != 1
                        || !PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resources[0])) {
                    request.deny();
                    return;
                }

                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED) {
                    request.grant(resources);
                } else {
                    pendingAudioRequest = request;
                    requestPermissions(
                            new String[]{Manifest.permission.RECORD_AUDIO},
                            AUDIO_PERMISSION_REQUEST
                    );
                }
            }

            @Override
            public void onPermissionRequestCanceled(PermissionRequest request) {
                if (pendingAudioRequest == request) {
                    pendingAudioRequest = null;
                }
            }
        });
    }

    private void showCompatibilityMessage() {
        TextView message = new TextView(this);
        message.setBackgroundColor(Color.rgb(23, 25, 27));
        message.setTextColor(Color.WHITE);
        message.setTextSize(18);
        message.setPadding(40, 48, 40, 48);
        message.setText(
                "This device's Android System WebView is missing a required audio feature.\n\n"
                        + "openDAW needs SharedArrayBuffer for its real-time audio engine. "
                        + "Please update Android System WebView (and Chrome, if offered) "
                        + "from the Play Store, then reopen openDAW.\n\n"
                        + "No project data has been changed."
        );
        setContentView(message);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != FILE_CHOOSER_REQUEST || pendingFileCallback == null) {
            return;
        }

        Uri[] results = null;
        if (resultCode == Activity.RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                results = new Uri[count];
                for (int i = 0; i < count; i++) {
                    results[i] = data.getClipData().getItemAt(i).getUri();
                }
            } else if (data.getData() != null) {
                results = new Uri[]{data.getData()};
            }
        }
        pendingFileCallback.onReceiveValue(results);
        pendingFileCallback = null;
    }

    @Override
    protected void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == AUDIO_PERMISSION_REQUEST && pendingAudioRequest != null) {
            PermissionRequest request = pendingAudioRequest;
            pendingAudioRequest = null;
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                request.grant(new String[]{PermissionRequest.RESOURCE_AUDIO_CAPTURE});
            } else {
                request.deny();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (pendingFileCallback != null) {
            pendingFileCallback.onReceiveValue(null);
            pendingFileCallback = null;
        }
        if (pendingAudioRequest != null) {
            pendingAudioRequest.deny();
            pendingAudioRequest = null;
        }
        if (webView != null) {
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
