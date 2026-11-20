#!/usr/bin/env bash
set -e

echo "=========================================="
echo "Running All Unit Tests Across Modules..."
echo "=========================================="

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

cd "${ROOT_DIR}"

echo "[1/3] Testing federation-common..."
./gradlew :federation-common:test

echo "[2/3] Testing dataset-provider-service..."
./gradlew :dataset-provider-service:test

echo "[3/3] Testing hospital-node-service..."
./gradlew :hospital-node-service:test

echo "=========================================="
echo "All unit tests passed!"
echo "=========================================="
