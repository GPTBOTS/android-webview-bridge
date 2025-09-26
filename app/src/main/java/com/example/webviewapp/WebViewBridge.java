package com.example.webviewapp;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * WebView and H5 page communication bridge class
 * Implements a bidirectional communication protocol, supporting H5 calling native and native calling H5
 */
public class WebViewBridge {
    private static final String TAG = "WebViewBridge";
    
    // Event type constants - H5 calling native
    public static final String EVENT_CLICK = "click";
    public static final String EVENT_MESSAGE = "message";
    
    // Event type constants - Native calling H5
//    public static final String EVENT_WILL_CLOSE = "willClose";
    
    private final Context context;
    private final WebView webView;
    private final BridgeCallback callback;
    
    public WebViewBridge(Context context, WebView webView, BridgeCallback callback) {
        this.context = context;
        this.webView = webView;
        this.callback = callback;
    }
    
    /**
     * Register JavaScript interface
     * Injects agentWebBridge object into WebView
     */
    public void registerJSInterface() {
        webView.addJavascriptInterface(new JSBridge(), "agentWebBridge");
        Log.d(TAG, "JavaScript interface registered: agentWebBridge");
    }
    
    /**
     * Native calls H5 method
     * @param eventType Event type
     * @param data JSON data object
     */
    public void callH5(String eventType, JSONObject data) {
        try {
            JSONObject message = new JSONObject();
            message.put("eventType", eventType);
            message.put("data", data != null ? data : new JSONObject());
            
            String jsonString = message.toString();
            // Escape single quotes
            String escapedJson = jsonString.replace("'", "\\'").replace("\"", "\\\"");
            String jsCode = "javascript:window.onCallH5Message('" + escapedJson + "')";
            
            Log.d(TAG, "Calling H5 method: " + eventType + ", data: " + jsonString);
            
            webView.post(() -> {
                webView.evaluateJavascript(jsCode, result -> {
                    Log.d(TAG, "H5 return result: " + result);
                });
            });
        } catch (JSONException e) {
            Log.e(TAG, "Failed to construct H5 call message", e);
        }
    }
    
    /**
     * JavaScript interface class
     * Provides native methods for H5 to call
     */
    public class JSBridge {
        @JavascriptInterface
        public void callNative(String jsonMessage) {
            Log.d(TAG, "H5 calling native: " + jsonMessage);
            
            try {
                JSONObject jsonObject = new JSONObject(jsonMessage);
                String eventType = jsonObject.getString("eventType");
                JSONObject data = jsonObject.optJSONObject("data");
                if (data == null) {
                    data = new JSONObject();
                }
                
                // Dispatch handling based on event type
                switch (eventType) {
                    case EVENT_CLICK:
                        if (callback != null) {
                            callback.onClick(data);
                        }
                        break;
                    case EVENT_MESSAGE:
                        if (callback != null) {
                            callback.onMessage(data);
                        }
                        break;
                        
                    default:
                        Log.w(TAG, "Unhandled event type: " + eventType);
                        if (callback != null) {
                            callback.onUnhandledEvent(eventType, data);
                        }
                        break;
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing H5 message", e);
            }
        }
    }
    
    /**
     * Bridge callback interface
     * Defines event handling methods that the native side needs to implement
     */
    public interface BridgeCallback {

        /**
         * Handle unknown event type
         * @param eventType Event type
         * @param data Event data
         */
        void onUnhandledEvent(String eventType, JSONObject data);

        /**
         * Click event
         * @param data Additional data
         */
        void onClick(JSONObject data);

        /**
         * Message event
         * @param data Additional data
         */
        void onMessage(JSONObject data);
    }
}


