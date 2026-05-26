#!/usr/bin/env bash
# Compila GMP estático para iphoneos (arm64). Executar no macOS (local ou GitHub Actions).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
GMP_SRC="${GMP_SRC:-$ROOT/gmp-6.3.0}"
OUT_DIR="$ROOT/app/src/main/cpp/gmp/lib/ios-arm64"
BUILD_DIR="$ROOT/build/gmp-ios"

if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "build-gmp-ios.sh requer macOS (Xcode toolchain)." >&2
  exit 1
fi

if [[ ! -d "$GMP_SRC" ]]; then
  echo "Fonte GMP não encontrada: $GMP_SRC" >&2
  exit 1
fi

SDK_PATH="$(xcrun --sdk iphoneos --show-sdk-path)"
export CC="$(xcrun --sdk iphoneos --find clang)"
export CXX="$(xcrun --sdk iphoneos --find clang++)"
export CFLAGS="-arch arm64 -isysroot $SDK_PATH -miphoneos-version-min=15.0 -O3"
export CXXFLAGS="$CFLAGS"
export LDFLAGS="-arch arm64 -isysroot $SDK_PATH"

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR" "$OUT_DIR"
cd "$BUILD_DIR"

"$GMP_SRC/configure" \
  --host=aarch64-apple-ios \
  --build="$(uname -m)-apple-darwin" \
  --prefix="$OUT_DIR" \
  --disable-shared \
  --enable-static \
  --with-pic

make -j"$(sysctl -n hw.ncpu 2>/dev/null || echo 4)"
make install

echo "GMP iOS instalado em $OUT_DIR"
