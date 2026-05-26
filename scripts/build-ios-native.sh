#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
IOS_BUILD="$ROOT/build/ios-native"
GMP_LIB="$ROOT/app/src/main/cpp/gmp/lib/ios-arm64"

if [[ ! -f "$GMP_LIB/libgmp.a" ]]; then
  echo "GMP iOS ausente — executando build-gmp-ios.sh"
  bash "$ROOT/scripts/build-gmp-ios.sh"
fi

cmake -S "$ROOT/ios" -B "$IOS_BUILD" \
  -G Xcode \
  -DCMAKE_SYSTEM_NAME=iOS \
  -DCMAKE_OSX_DEPLOYMENT_TARGET=15.0 \
  -DCMAKE_OSX_ARCHITECTURES=arm64 \
  -DCMAKE_XCODE_ATTRIBUTE_ONLY_ACTIVE_ARCH=NO

cmake --build "$IOS_BUILD" --config Release --target ppf_core

LIB_A="$(find "$IOS_BUILD" -name 'libppf_core.a' | head -1)"
if [[ -z "$LIB_A" || ! -f "$LIB_A" ]]; then
  echo "libppf_core.a nao encontrado em $IOS_BUILD" >&2
  exit 1
fi
mkdir -p "$ROOT/build/ios-native"
cp -f "$LIB_A" "$ROOT/build/ios-native/libppf_core.a"
echo "Biblioteca nativa iOS: $ROOT/build/ios-native/libppf_core.a"
