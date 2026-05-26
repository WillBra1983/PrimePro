#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
IOS_BUILD="$ROOT/build/ios-native"
GMP_LIB="$ROOT/app/src/main/cpp/gmp/lib/ios-arm64"

# Artefato legado (builds antigos copiavam para a raiz e quebravam o archive)
rm -f "$IOS_BUILD/libppf_core.a"

gmp_ok() {
  [[ -f "$GMP_LIB/lib/libgmp.a" || -f "$GMP_LIB/libgmp.a" ]] \
    && [[ -f "$GMP_LIB/lib/libgmpxx.a" || -f "$GMP_LIB/libgmpxx.a" ]]
}

if ! gmp_ok; then
  echo "GMP iOS incompleto (precisa libgmp.a + libgmpxx.a) — executando build-gmp-ios.sh" >&2
  bash "$ROOT/scripts/build-gmp-ios.sh"
fi

cmake -S "$ROOT/ios" -B "$IOS_BUILD" \
  -G Xcode \
  -DCMAKE_SYSTEM_NAME=iOS \
  -DCMAKE_OSX_DEPLOYMENT_TARGET=16.0 \
  -DCMAKE_OSX_ARCHITECTURES=arm64 \
  -DCMAKE_XCODE_ATTRIBUTE_ONLY_ACTIVE_ARCH=NO

cmake --build "$IOS_BUILD" --config Release --target ppf_core

STATIC_LIB="$IOS_BUILD/Release-iphoneos/libppf_core.a"
if [[ ! -f "$STATIC_LIB" ]]; then
  STATIC_LIB="$(find "$IOS_BUILD" -path '*/Release-iphoneos/libppf_core.a' -print -quit)"
fi
if [[ -z "${STATIC_LIB:-}" || ! -f "$STATIC_LIB" ]]; then
  echo "libppf_core.a nao encontrado em $IOS_BUILD" >&2
  exit 1
fi
echo "Biblioteca nativa iOS: $STATIC_LIB" >&2
