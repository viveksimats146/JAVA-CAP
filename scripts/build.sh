#!/usr/bin/env bash
set -e

echo "=========================================="
echo "Building Federated Platform Projects..."
echo "=========================================="

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

cd "${ROOT_DIR}"

echo "[1/4] Building federation-common..."
./gradlew :federation-common:build

echo "[2/4] Building dataset-provider-service..."
./gradlew :dataset-provider-service:build

echo "[3/4] Building hospital-node-service..."
./gradlew :hospital-node-service:build

echo "[4/4] Compiling Android app (:app:assembleDebug)..."
./gradlew :app:assembleDebug

echo "=========================================="
echo "All modules built successfully!"
echo "=========================================="
