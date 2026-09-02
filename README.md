## VideoPlayerX

A modern, fast, and lightweight video player designed for Android.

## How It Works

The application runs a player consisting of a single HTML/CSS/JS file ("app/src/main/assets/video_player.html") inside a lightweight Android WebView shell. Video selection and persistent storage are managed on the native Android side ("MainActivity.java", "VideoLibrary.java") through a JavaScript bridge.

```
VideoPlayerX/
├── app/
│   ├── src/main/
│   │   ├── assets/video_player.html   # Oynatıcının tamamı (UI + mantık)
│   │   ├── java/com/example/videoplayer/
│   │   │   ├── MainActivity.java      # WebView kabuğu + JS köprüsü
│   │   │   └── VideoLibrary.java      # Kalıcı JSON tabanlı depolama
│   │   ├── res/                       # Tema/string kaynakları
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
└── settings.gradle
```

## Building for Your Own Device (Getting the APK)

1. Install [Android Studio] (https://developer.android.com/studio).
2. Open this project in Android Studio using Open.
3. Wait for Gradle synchronization to complete. An internet connection is required during the first setup.
4. Go to Build > Build Bundle(s)/APK(s) > Build APK(s).
5. Transfer the generated `app-debug.apk` file to your Android device and install it.
   

## License

[MIT](LICENSE)
