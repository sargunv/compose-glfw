#!/usr/bin/env bash
set -euo pipefail

mode="${1:-auto}"

runtime_dir="${XDG_RUNTIME_DIR:-/tmp/xdg-runtime-$(id -u)}"
export XDG_RUNTIME_DIR="$runtime_dir"
mkdir -p "$XDG_RUNTIME_DIR"
chmod 700 "$XDG_RUNTIME_DIR"

run_gradle() {
  if [[ "$(uname -s)" == "Linux" && -z "${DISPLAY:-}" ]]; then
    xvfb-run -a ./gradlew "$@"
  else
    ./gradlew "$@"
  fi
}

case "$mode" in
  x11)
    run_gradle :compose-glfw-demo:runX11
    ;;
  *)
    run_gradle :compose-glfw-demo:run
    ;;
esac
