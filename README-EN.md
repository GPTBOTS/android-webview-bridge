# Android WebView Integration App

## Overview

This Android application provides a WebView container for seamlessly integrating and interacting with the web workspace at [https://gptbots-auto.qa.jpushoa.com/space/h5/home](https://gptbots-auto.qa.jpushoa.com/space/h5/home). The app features a robust bidirectional communication bridge between native Android components and H5 web pages, with support for various permissions and file operations.

## Features

- **Configurable Integration**: Customizable Base URL and AiToken input
- **Bidirectional Communication**: Native Android to H5 and H5 to Native messaging
- **Permission Management**: Handles runtime permissions for both Android and WebView
- **Media Support**: Audio recording capability with proper permission handling
- **File Access**: Full support for file selection and uploads from web pages

## Requirements

- Android Studio 4.0+
- Minimum SDK: API 21 (Android 5.0)
- Target SDK: API 33 (Android 13)
- Java 8+

## Project Structure

```
android-webview/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/webviewapp/
│   │   │   ├── MainActivity.java      # Entry point, configuration screen
│   │   │   ├── WebViewActivity.java   # WebView container and permission handler
│   │   │   ├── WebViewBridge.java     # Communication bridge implementation
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_main.xml  # Configuration UI
│   │   │   │   ├── activity_webview.xml # WebView UI
│   │   ├── AndroidManifest.xml        # Permission declarations
├── build.gradle                        # Project dependencies
```

## Setup Instructions

1. Clone this repository:
   ```bash
   git clone <repository-url>
   ```

2. Open the project in Android Studio

3. Build the project:
   ```bash
   ./gradlew build
   ```

4. Install on a device or emulator:
   ```bash
   ./gradlew installDebug
   ```

## Usage Guide

### Configuration

1. Launch the app
2. On the configuration screen:
   - Enter Base URL (optional, default is provided)
   - Enter AiToken (optional, default is provided)
3. Press "Enter App" to load the web workspace

### Permissions

The application requires and manages the following permissions:

- **Network**: Internet access and network state
- **Audio**: Recording and audio settings modification
- **Storage**:
  - For Android 13+: READ_MEDIA_IMAGES, READ_MEDIA_AUDIO, READ_MEDIA_VIDEO
  - For Android <13: READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE
- **Other**: Wake lock for maintaining screen activity

Permissions are requested at runtime as needed.

## Integration with Web Pages

### JavaScript to Native Communication

Web pages can communicate with native components using the following JavaScript:

```javascript
var message = {
    eventType: "click", // or "message"
    data: {
        value: "close" // or other custom data
    }
};

// Call native method
if (window.agentWebBridge) {
    agentWebBridge.callNative(JSON.stringify(message));
}
```

### Native to JavaScript Communication

The app can send events to web pages:

```java
JSONObject data = new JSONObject();
data.put("key", "value");
webViewBridge.callH5(WebViewBridge.EVENT_CLICK, data);
```

Web pages should implement:

```javascript
window.onCallH5Message = function(message) {
    var msgObj = typeof message === 'string' ? JSON.parse(message) : message;
    // Handle message based on eventType
}
```

