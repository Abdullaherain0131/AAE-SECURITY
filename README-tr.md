<div align="center">
  <img src="logo.jpg" alt="AAE Security Logo" width="120" />
  <h1>AAE Security</h1>
  <p><b>Yeni Nesil Otonom Siber Güvenlik Motoru</b></p>
  <p><i>Tamamen Abdullah Asım Ersin tarafından geliştirilmiştir</i></p>
  
  [![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
  [![Android](https://img.shields.io/badge/Platform-Android-green.svg)]()
  [![TFLite](https://img.shields.io/badge/Powered_by-TFLite-orange.svg)]()
</div>

<hr />

[🇬🇧 Read in English](README.md)

AAE Security, cihaz içi yapay zeka ile güçlendirilmiş, tamamen çevrimdışı (offline-first) çalışan gelişmiş bir mobil güvenlik çözümüdür. Bulut bağlantısına ihtiyaç duymadan tehditleri tespit etmek, analiz etmek ve etkisiz hale getirmek için yerelleştirilmiş TFLite modellerini (`Singularity Turbo` & `Titan`) kullanır.

## ✨ Temel Özellikler

- **🛡️ %100 Çevrimdışı Yapay Zeka:** Tüm tarama ve tehdit algılama işlemleri `Singularity` ve `Titan` modelleri kullanılarak yerel olarak gerçekleştirilir. Verileriniz asla cihaz dışına çıkmaz.
- **⚡ Shizuku Entegrasyonu:** Önbellek temizleme, izin yönetimi ve uygulama kaldırma gibi derin sistem yönetimi için Shizuku aracılığıyla gelişmiş sistem düzeyinde yetenekler kullanır.
- **🌀 Siber Küre Tarayıcı (Cyber Globe):** Yay (spring) fiziği ile çalışan, 3 boyutlu lazer tarama halkasına sahip yüksek tepkimeli bir kullanıcı arayüzü.
- **🧠 Otonom Gelişim Döngüsü:** Geçmiş tehdit verilerine dayanarak tarama eşiklerini dinamik olarak günceller, böylece motor zamanla öğrenir ve adapte olur.
- **🌑 Premium Koyu Tema:** Maksimum görsel konfor ve profesyonel bir his için neon içermeyen, minimalist, tamamen koyu temalı estetik.

## 🚀 Başlarken

### Gereksinimler
- Android 8.0 (API 26) veya daha yüksek bir sürümde çalışan Android cihaz.
- Gelişmiş eylemler için [Shizuku](https://shizuku.rikka.app/)'nun cihazınızda kurulu ve yapılandırılmış olması gerekir.

### Kurulum
1. Projeyi Android Studio'da açın.
3. Gradle dosyalarını senkronize edin.
4. Uygulamayı derleyip cihazınızda çalıştırın.

## 🛠️ Mimari

AAE Security, modern Android teknolojilerini kullanır:
- **Arayüz (UI):** Jetpack Compose ve özel Spring animasyonları.
- **Yapay Zeka (AI/ML):** Cihaz içi çıkarım (inference) için TensorFlow Lite (`.tflite` modelleri).
- **Eşzamanlılık (Concurrency):** Kotlin Coroutines & Flow.
- **Sistem İşlemleri:** Rootsuz sistem düzeyinde işlemler için Shizuku API.

## 🤝 Katkıda Bulunma

Katkılarınızı bekliyoruz! Lütfen üzerinde çalışılması gerekenleri görmek için [Issues](../../issues) sayfamızı kontrol edin.

1. Projeyi Fork'layın
2. Özellik Dalınızı (Feature Branch) Oluşturun (`git checkout -b feature/YeniOzellik`)
3. Değişikliklerinizi Commit'leyin (`git commit -m 'YeniOzellik eklendi'`)
4. Dalınıza Push'layın (`git push origin feature/YeniOzellik`)
5. Bir Pull Request açın.

## 📄 Lisans

MIT Lisansı altında dağıtılmaktadır. Daha fazla bilgi için `LICENSE` dosyasına bakın.
