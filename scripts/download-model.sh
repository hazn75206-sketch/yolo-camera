#!/bin/bash
set -e
MODEL_DIR="app/src/main/assets/models"
MODEL_FILE="$MODEL_DIR/yolov8n.onnx"
mkdir -p "$MODEL_DIR"
if [ -f "$MODEL_FILE" ] && [ "$(stat -c%s "$MODEL_FILE" 2>/dev/null || stat -f%z "$MODEL_FILE")" -gt 1000000 ]; then
  echo "Model already exists, skipping download."
  exit 0
fi
echo "Downloading yolov8n.onnx (~12MB)..."
curl -sL -o "$MODEL_FILE" "https://huggingface.co/Ultralytics/YOLOv8/resolve/main/yolov8n.onnx?download=true" || true
if [ ! -f "$MODEL_FILE" ] || [ "$(stat -c%s "$MODEL_FILE" 2>/dev/null || stat -f%z "$MODEL_FILE")" -lt 1000000 ]; then
  echo "Primary mirror failed, trying AXERA mirror..."
  curl -sL -o "$MODEL_FILE" "https://huggingface.co/AXERA-TECH/YOLOv8/resolve/main/yolov8n_640x640.onnx?download=true" || true
fi
ls -lh "$MODEL_FILE" || echo "WARNING: model download failed. App will show missing-model error at runtime. See MODEL_README.md"
