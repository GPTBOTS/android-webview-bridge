package com.example.webviewapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

public class WebViewActivity extends AppCompatActivity {

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private static final int REQUEST_FILE_CHOOSER = 101;
    private static final String TAG = "WebViewActivity";
    
    // Store the pending permission request
    private PermissionRequest pendingPermissionRequest;
    
    // WebView communication bridge
    private WebViewBridge webViewBridge;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        webView = findViewById(R.id.webView);

        // Get URL passed from MainActivity
        String url = getIntent().getStringExtra("url");
        if (url == null || url.isEmpty()) {
            Toast.makeText(this, "Invalid URL", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Configure WebView settings
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true); // Enable JavaScript
        webSettings.setDomStorageEnabled(true); // Enable DOM Storage API
        webSettings.setAllowFileAccess(true); // Allow file access
        webSettings.setAllowContentAccess(true); // Allow content URL access
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW); // Allow mixed content
        webSettings.setMediaPlaybackRequiresUserGesture(false); // No user gesture required to play media
        
        // Additional settings for audio recording
        webSettings.setAllowFileAccessFromFileURLs(true); // Allow file URL to access files
        webSettings.setAllowUniversalAccessFromFileURLs(true); // Allow universal access
        webSettings.setDatabaseEnabled(true); // Enable database
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT); // Set cache mode
        
        // Set User-Agent, some H5 pages require specific User-Agent to work properly
        String userAgent = webSettings.getUserAgentString();
        webSettings.setUserAgentString(userAgent + " WebViewApp/1.0");
        
        Log.d(TAG, "WebView settings configured");

        // Initialize WebView communication bridge
        initWebViewBridge();

        // Set WebViewClient to prevent navigation to external browser
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });

        // Set WebChromeClient to handle file uploads and permission requests
        webView.setWebChromeClient(new WebChromeClient() {
            // Handle HTML file input tag <input type="file">
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                WebViewActivity.this.filePathCallback = filePathCallback;
                
                // Create file selection Intent
                Intent intent = fileChooserParams.createIntent();
                try {
                    startActivityForResult(intent, REQUEST_FILE_CHOOSER);
                } catch (Exception e) {
                    filePathCallback.onReceiveValue(null);
                    WebViewActivity.this.filePathCallback = null;
                    Toast.makeText(WebViewActivity.this, "Cannot open file chooser", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            }

            // Handle permission requests from the webpage (such as microphone access)
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                Log.d(TAG, "Permission request received: " + java.util.Arrays.toString(request.getResources()));
                
                runOnUiThread(() -> {
                    // Check the type of requested permissions
                    String[] requestedResources = request.getResources();
                    boolean hasAudioPermission = false;
                    
                    for (String resource : requestedResources) {
                        Log.d(TAG, "Requested resource: " + resource);
                        if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) {
                            hasAudioPermission = true;
                            break;
                        }
                    }
                    
                    if (hasAudioPermission) {
                        // Check if the app already has audio recording permission
                        if (ContextCompat.checkSelfPermission(WebViewActivity.this, Manifest.permission.RECORD_AUDIO) 
                                == PackageManager.PERMISSION_GRANTED) {
                            Log.d(TAG, "Granting audio recording permission to WebView");
                            request.grant(requestedResources);
                        } else {
                            Log.d(TAG, "App doesn't have audio permission, denying WebView permission request");
                            // Store permission request to process after system permission is granted
                            pendingPermissionRequest = request;
                            // Request system audio recording permission
                            ActivityCompat.requestPermissions(WebViewActivity.this, 
                                new String[]{Manifest.permission.RECORD_AUDIO}, 200);
                        }
                    } else {
                        // For other types of permissions, grant directly
                        Log.d(TAG, "Granting other permissions to WebView");
                        request.grant(requestedResources);
                    }
                });
            }
        });

        // Load URL
        webView.loadUrl(url);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == 200) { // Audio recording permission request
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "User granted audio recording permission");
                // If there is a pending WebView permission request, it can now be granted
                if (pendingPermissionRequest != null) {
                    Log.d(TAG, "Granting previously denied WebView audio permission");
                    pendingPermissionRequest.grant(pendingPermissionRequest.getResources());
                    pendingPermissionRequest = null;
                }
            } else {
                Log.d(TAG, "User denied audio recording permission");
                // User denied audio permission, deny the WebView permission request
                if (pendingPermissionRequest != null) {
                    pendingPermissionRequest.deny();
                    pendingPermissionRequest = null;
                }
                Toast.makeText(this, "Audio permission denied, recording function unavailable", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_FILE_CHOOSER) {
            // Handle WebView's file selection callback
            if (filePathCallback != null) {
                Uri[] results = null;
                
                // Check response
                if (resultCode == RESULT_OK && data != null) {
                    String dataString = data.getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    } else if (data.getClipData() != null) {
                        // Handle multiple file selection
                        int count = data.getClipData().getItemCount();
                        results = new Uri[count];
                        for (int i = 0; i < count; i++) {
                            results[i] = data.getClipData().getItemAt(i).getUri();
                        }
                    }
                }
                
                filePathCallback.onReceiveValue(results);
                filePathCallback = null;
            }
        }
    }


    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
    
    /**
     * Initialize WebView communication bridge
     */
    private void initWebViewBridge() {
        webViewBridge = new WebViewBridge(this, webView, new WebViewBridge.BridgeCallback() {

            @Override
            public void onClick(JSONObject data) {
                Log.d(TAG, "H5 click event request, data: " + data.toString());
                String value = data.optString("value");

                if (TextUtils.equals(value, "close")){
                    closeWeb(data);
                }
            }

            @Override
            public void onMessage(JSONObject data) {
                Log.d(TAG, "H5 message event request, data: " + data.toString());
            }


            @Override
            public void onUnhandledEvent(String eventType, JSONObject data) {
                Log.w(TAG, "Unhandled event type: " + eventType + ", data: " + data.toString());
                runOnUiThread(() -> {
                    Toast.makeText(WebViewActivity.this, "Unsupported feature: " + eventType, Toast.LENGTH_SHORT).show();
                });
            }
        });
        
        // Register JavaScript interface
        webViewBridge.registerJSInterface();
        
        Log.d(TAG, "WebView communication bridge initialization completed");
    }

    public void closeWeb(JSONObject data) {
        runOnUiThread(() -> {
            // Can notify H5 about imminent closure
            JSONObject willCloseData = new JSONObject();
            try {
                willCloseData.put("value", data.optString("value"));
                willCloseData.put("reason", "user_request");
                willCloseData.put("delay", 1000);
                willCloseData.put("timestamp", System.currentTimeMillis());
            } catch (JSONException e) {
                Log.e(TAG, "Failed to construct willClose data", e);
            }

            webViewBridge.callH5(WebViewBridge.EVENT_CLICK, willCloseData);

            // Delayed Activity closure
            webView.postDelayed(() -> {
                finish();
            }, 1000);
        });
    }
}
