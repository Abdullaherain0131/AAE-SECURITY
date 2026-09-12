<div align="center">
  <img src="logo.jpg" alt="AAE Security Logo" width="120" />
  <h1>AAE Security</h1>
  <p><b>Next-Generation Autonomous Cybersecurity Engine</b></p>
  <p><i>Developed entirely by Abdullah Asım Ersin</i></p>
  
  [![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
  [![Android](https://img.shields.io/badge/Platform-Android-green.svg)]()
  [![TFLite](https://img.shields.io/badge/Powered_by-TFLite-orange.svg)]()
</div>

<hr />

[🇹🇷 Türkçe Oku (Read in Turkish)](README-tr.md)

AAE Security is an advanced, offline-first mobile security solution powered by on-device AI. It leverages localized TFLite models (`Singularity Turbo` & `Titan`) to detect, analyze, and neutralize threats autonomously without relying on cloud connectivity.

## ✨ Key Features

- **🛡️ 100% Offline AI Engine:** All scanning and threat detection is performed locally using the `Singularity` and `Titan` models. No data leaves your device.
- **⚡ Shizuku Integration:** Employs advanced system-level capabilities via Shizuku for deep application management, including cache clearing, permission management, and uninstallation.
- **🌀 Cyber Globe Scanner:** Features a highly responsive, spring-physics-driven UI with a 3D-like scanning laser ring.
- **🧠 Auto-Improvement Loop:** Dynamically adjusts scanning thresholds based on historical threat data, allowing the engine to learn and adapt over time.
- **🌑 Premium Dark Theme:** A completely neon-free, minimalist dark aesthetic for maximum visual comfort and a professional feel.

## 🚀 Getting Started

### Prerequisites
- Android device running Android 8.0 (API 26) or higher.
- [Shizuku](https://shizuku.rikka.app/) installed and configured on your device (Required for advanced actions).

### Installation
1. Open the project in Android Studio.
3. Sync the Gradle files.
4. Build and run the app on your device.

## 🛠️ Architecture

AAE Security utilizes a modern Android tech stack:
- **UI:** Jetpack Compose with custom Spring animations.
- **AI/ML:** TensorFlow Lite (`.tflite` models) for on-device inference.
- **Concurrency:** Kotlin Coroutines & Flow.
- **System Ops:** Shizuku API for rootless system-level operations.

## 🤝 Contributing

We welcome contributions! Please check our [Issues](../../issues) page to see what needs work.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.
