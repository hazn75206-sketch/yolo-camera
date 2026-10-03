# Model YOLOv8n ONNX

File `app/src/main/assets/models/yolov8n.onnx` tidak di-commit ke git karena ukuran 12MB.

## Otomatis (CI)
Workflow mengekspor `yolov8n.onnx` standar langsung dari `.pt` resmi Ultralytics
(`yolo export model=yolov8n.pt format=onnx imgsz=640`), lalu file `yolov8n.onnx`
ikut dipublish ke release `apk-build`. Mirror AXERA-TECH tidak dipakai karena
outputnya feature map tanpa detection head.

## Manual
1. Download `yolov8n.onnx` dari release:
   `https://github.com/hazn75206-sketch/yolo-camera/releases/download/apk-build/yolov8n.onnx`
   atau export sendiri: `yolo export model=yolov8n.pt format=onnx imgsz=640`.
2. Taruh di `app/src/main/assets/models/yolov8n.onnx`.
3. Build: `./gradlew assembleDebug`.

## Manual
1. Download `yolov8n.onnx` dari salah satu link di atas.
2. Taruh di `app/src/main/assets/models/yolov8n.onnx`.
3. Build: `./gradlew assembleDebug`.

## Catatan lisensi
Model YOLOv8 dari Ultralytics. Cek lisensi Ultralytics sebelum distribusi komersial.
Input 640x640, output `(1, 84, 8400)`, 80 class COCO.
