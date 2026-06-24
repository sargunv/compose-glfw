#!/usr/bin/env bash
set -euo pipefail

if [[ "$(uname -s)" != "Linux" ]]; then
  echo "setup:graphics is only required on Linux."
  exit 0
fi

if command -v apt-get >/dev/null 2>&1; then
  sudo DEBIAN_FRONTEND=noninteractive apt-get install -y \
    xvfb \
    mesa-utils \
    libegl1 \
    libegl-mesa0 \
    libgl1 \
    libgl1-mesa-dri \
    libglx-mesa0 \
    libgbm1 \
    libx11-6 \
    libxrandr2 \
    libxinerama1 \
    libxcursor1 \
    libxi6 \
    libxxf86vm1
else
  echo "No supported package manager found. Install Mesa EGL, llvmpipe, and xvfb manually."
  exit 1
fi

runtime_dir="/tmp/xdg-runtime-$(id -u)"
mkdir -p "$runtime_dir"
chmod 700 "$runtime_dir"

echo "Graphics stack ready. Software EGL is available via Mesa llvmpipe."
echo "Use: mise run demo"
