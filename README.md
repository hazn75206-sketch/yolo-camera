# YOLO Camera

Kamera Android (CameraX + Compose) dengan object detection YOLOv8n via ONNX Runtime.

## Teknologi
Kotlin 2.0.21, AGP 8.5.2, Compose BOM 2026.06.01, CameraX 1.6.1, `onnxruntime-android:1.22.0`.

## Cara jalan
1. Model otomatis di-download saat CI (`scripts/download-model.sh`), atau manual lihat `MODEL_README.md`.
2. Build lokal: `./gradlew assembleDebug`.
3. Build CI: push ke `main`, ambil APK di Artifacts `yolo-camera-debug`.

## Fitur
Kamera belakang langsung tampil, toolbar overlay auto-hide, toggle Detection, mode Low 320 / Medium 480 / High 640, class filter, settings DataStore, bbox + label + confidence + FPS + count.
