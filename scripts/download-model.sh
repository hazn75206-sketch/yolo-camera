#!/bin/bash
set -e
MODEL_DIR="app/src/main/assets/models"
MODEL_FILE="$MODEL_DIR/yolov8n.onnx"
mkdir -p "$MODEL_DIR"
ok() { [ -f "$MODEL_FILE" ] && [ "$(stat -c%s "$MODEL_FILE" 2>/dev/null || stat -f%z "$MODEL_FILE")" -gt 1000000 ]; }
if ok; then
  echo "Model sudah ada."
  exit 0
fi
echo "Download yolov8n.onnx dari release apk-build..."
curl -sL -o "$MODEL_FILE" "https://github.com/hazn75206-sketch/yolo-camera/releases/download/apk-build/yolov8n.onnx" || true
if ok; then
  ls -lh "$MODEL_FILE"
  exit 0
fi
rm -f "$MODEL_FILE"
if command -v yolo >/dev/null 2>&1; then
  echo "Export dari .pt resmi Ultralytics..."
  curl -sL -o yolov8n.pt https://github.com/ultralytics/assets/releases/download/v8.3.0/yolov8n.pt
  yolo export model=yolov8n.pt format=onnx imgsz=640
  mv yolov8n.onnx "$MODEL_FILE"
  ls -lh "$MODEL_FILE"
  exit 0
fi
echo "GAGAL. Cara manual: pip install ultralytics, lalu yolo export model=yolov8n.pt format=onnx, taruh hasilnya di $MODEL_FILE"
exit 1
