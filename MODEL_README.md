# Model YOLOv8n ONNX

File `app/src/main/assets/models/yolov8n.onnx` tidak di-commit ke git karena ukuran 12MB.

## Otomatis (CI)
`scripts/download-model.sh` dijalankan oleh GitHub Actions sebelum build.
Sumber utama: `https://huggingface.co/Ultralytics/YOLOv8` (yolov8n.onnx).
Mirror: `https://huggingface.co/AXERA-TECH/YOLOv8` (yolov8n_640x640.onnx).

## Manual
1. Download `yolov8n.onnx` dari salah satu link di atas.
2. Taruh di `app/src/main/assets/models/yolov8n.onnx`.
3. Build: `./gradlew assembleDebug`.

## Catatan lisensi
Model YOLOv8 dari Ultralytics. Cek lisensi Ultralytics sebelum distribusi komersial.
Input 640x640, output `(1, 84, 8400)`, 80 class COCO.
