#!/usr/bin/env bash
set -e

echo "=========================================="
echo "Starting Backend Microservices via Docker..."
echo "=========================================="

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

cd "${ROOT_DIR}"

if [ ! -f .env ]; then
    echo "Creating .env from .env.example..."
    cp .env.example .env
fi

docker-compose up --build -d

echo "=========================================="
echo "Services started!"
echo "Hospital Node: http://localhost:8080"
echo "Dataset Provider: http://localhost:8081"
echo "=========================================="
