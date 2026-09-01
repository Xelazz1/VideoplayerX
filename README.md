# VideoPlayerX

Basit, hızlı ve tamamen çevrimdışı çalışan bir Android video oynatıcı. Reklam yok, internet gerektirmiyor, videolarını cihazında seçip anında izlemene yarıyor.

## Özellikler

- 🌑 Koyu tema
- ➕ Dokunmatik "ekle" baloncuğu ile video seçme (galeri/dosya seçici)
- ✏️ Videoları yeniden adlandırma / silme
- ▶️ Anlaşılır oynatıcı: oynat/duraklat, 10sn ileri-geri, hız ayarı (0.5x–2x)
- 🔒 Ekran kilidi (yanlışlıkla dokunmayı engeller, 5sn sonra ikon otomatik gizlenir)
- ⏯️ Kaldığı yerden devam etme
- ⏭️ Sonraki / önceki videoya geçme, video bitince otomatik sıradakine geçme
- 📱 Video otomatik olarak ekranı tam dolduracak şekilde yatay moda geçebiliyor
- 💾 Kalıcı depolama: eklediğin videolar, isimler ve kaldığın yer uygulamayı kapatıp açsan bile duruyor

## Nasıl çalışıyor

Uygulama, tek bir HTML/CSS/JS dosyasından oluşan oynatıcıyı (`app/src/main/assets/video_player.html`) hafif bir Android WebView kabuğu içinde çalıştırıyor. Video seçimi ve kalıcı depolama, native Android tarafında (`MainActivity.java`, `VideoLibrary.java`) bir JavaScript köprüsü üzerinden yönetiliyor.

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

## Kendi cihazın için derleme (APK almak)

1. [Android Studio](https://developer.android.com/studio) kur
2. Bu projeyi Android Studio'da **Open** ile aç
3. Gradle senkronizasyonunun bitmesini bekle (ilk açılışta internet ister)
4. **Build > Build Bundle(s)/APK(s) > Build APK(s)**
5. Oluşan `app-debug.apk` dosyasını telefonuna aktarıp kur

## Lisans

[MIT](LICENSE)
