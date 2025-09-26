package com.example.webviewapp;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText editTextBaseUrl;
    private EditText editTextAiToken;
    private Button buttonGo;
    private static final int REQUEST_PERMISSIONS_CODE = 100;
    
    // Default URL address
    private static final String DEFAULT_BASE_URL = "https://gptbots-auto.qa.jpushoa.com/space/h5/home";
    
    // Default AiToken value
    private static final String DEFAULT_AI_TOKEN = "YOUR_AI_TOKEN";

    // Permissions for Android 13 (API 33) and above
    private final String[] permissionsForAndroid13 = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_VIDEO
    };

    // Permissions for below Android 13
    private final String[] permissionsForBelowAndroid13 = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    // Using ActivityResultLauncher to handle permission request results
    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                boolean allGranted = true;
                for (Boolean granted : permissions.values()) {
                    if (!granted) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted) {
                    Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Some permissions denied, certain features may not work properly", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextBaseUrl = findViewById(R.id.editTextBaseUrl);
        editTextAiToken = findViewById(R.id.editTextUrl);
        buttonGo = findViewById(R.id.buttonGo);

        buttonGo.setOnClickListener(v -> {
            String baseUrl = editTextBaseUrl.getText().toString().trim();
            String aiToken = editTextAiToken.getText().toString().trim();
            
            // Validate and get a valid baseURL
            String validBaseUrl = getValidBaseUrl(baseUrl);
            
            // If AiToken input is empty, use the default AiToken
            if (aiToken.isEmpty()) {
                aiToken = DEFAULT_AI_TOKEN;
            }
            
            // Build the complete URL
            String fullUrl = validBaseUrl + "?AiToken=" + aiToken;
            
            // Launch WebView activity and pass the URL
            Intent intent = new Intent(MainActivity.this, WebViewActivity.class);
            intent.putExtra("url", fullUrl);
            startActivity(intent);
        });

        // Request necessary permissions
        checkAndRequestPermissions();
    }

    private void checkAndRequestPermissions() {
        // Choose different permission sets based on Android version
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 (API 33)
            permissions = permissionsForAndroid13;
        } else {
            permissions = permissionsForBelowAndroid13;
        }

        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toArray(new String[0]));
        }
    }
    
    /**
     * Validate and get a valid baseURL
     * @param inputUrl URL input by user
     * @return Valid baseURL
     */
    private String getValidBaseUrl(String inputUrl) {
        // If input is empty, use the default URL
        if (inputUrl.isEmpty()) {
            return DEFAULT_BASE_URL;
        }
        
        // Validate URL format
        if (isValidUrl(inputUrl)) {
            return inputUrl;
        } else {
            // URL format is incorrect, show a message and use default URL
            Toast.makeText(this, "URL format incorrect, using default URL", Toast.LENGTH_SHORT).show();
            return DEFAULT_BASE_URL;
        }
    }
    
    /**
     * Validate if a URL has valid http or https format
     * @param url URL to validate
     * @return true for valid, false for invalid
     */
    private boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        // Convert to lowercase for checking
        String lowerUrl = url.toLowerCase().trim();
        
        // Check if it starts with http:// or https://
        if (!lowerUrl.startsWith("http://") && !lowerUrl.startsWith("https://")) {
            return false;
        }
        
        try {
            // Use Android's Uri class for stricter validation
            android.net.Uri uri = android.net.Uri.parse(url);
            
            // Check if scheme is http or https
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                return false;
            }
            
            // Check if host exists
            String host = uri.getHost();
            if (host == null || host.trim().isEmpty()) {
                return false;
            }
            
            // Basic host format check (contains at least one dot, unless it's localhost)
            if (!host.equalsIgnoreCase("localhost") && !host.contains(".")) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            // Parsing exception, consider URL invalid
            return false;
        }
    }
}
