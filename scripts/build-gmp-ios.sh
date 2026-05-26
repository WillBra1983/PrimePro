#!/usr/bin/env bash
# Compila GMP estático para iphoneos (arm64). macOS / GitHub Actions.
# Se gmp-6.3.0/ não existir no repo, baixa do gnu.org automaticamente.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
GMP_VERSION="6.3.0"
GMP_TARBALL="gmp-${GMP_VERSION}.tar.xz"
GMP_URL="https://ftp.gnu.org/gnu/gmp/${GMP_TARBALL}"
OUT_DIR="$ROOT/app/src/main/cpp/gmp/lib/ios-arm64"
BUILD_DIR="$ROOT/build/gmp-ios"
CACHE_DIR="$ROOT/build"

if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "build-gmp-ios.sh requer macOS (Xcode toolchain)." >&2
  exit 1
fi

resolve_gmp_source() {
  if [[ -n "${GMP_SRC:-}" && -f "${GMP_SRC}/configure" ]]; then
    echo "$GMP_SRC"
    return 0
  fi
  if [[ -f "$ROOT/gmp-${GMP_VERSION}/configure" ]]; then
    echo "$ROOT/gmp-${GMP_VERSION}"
    return 0
  fi
  local extracted="$CACHE_DIR/gmp-${GMP_VERSION}"
  if [[ -f "$extracted/configure" ]]; then
    echo "$extracted"
    return 0
  fi
  mkdir -p "$CACHE_DIR"
  local tar_path="$CACHE_DIR/$GMP_TARBALL"
  if [[ ! -f "$tar_path" ]]; then
    echo "Baixando GMP ${GMP_VERSION} de ${GMP_URL} ..." >&2
    curl -fsSL --retry 3 --retry-delay 5 -o "$tar_path" "$GMP_URL"
  fi
  echo "Extraindo ${GMP_TARBALL} ..." >&2
  tar -xJf "$tar_path" -C "$CACHE_DIR"
  if [[ ! -f "$extracted/configure" ]]; then
    echo "configure nao encontrado apos extrair: $extracted" >&2
    exit 1
  fi
  echo "$extracted"
}

GMP_SRC="$(resolve_gmp_source)"
echo "Fonte GMP: $GMP_SRC"

SDK_PATH="$(xcrun --sdk iphoneos --show-sdk-path)"
export CC="$(xcrun --sdk iphoneos --find clang)"
export CXX="$(xcrun --sdk iphoneos --find clang++)"
export CFLAGS="-arch arm64 -isysroot $SDK_PATH -miphoneos-version-min=15.0 -O3"
export CXXFLAGS="$CFLAGS"
export LDFLAGS="-arch arm64 -isysroot $SDK_PATH"

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR" "$OUT_DIR/lib"
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

if [[ -f "$OUT_DIR/lib/libgmp.a" ]]; then
  echo "GMP iOS instalado em $OUT_DIR/lib/libgmp.a"
elif [[ -f "$OUT_DIR/libgmp.a" ]]; then
  echo "GMP iOS instalado em $OUT_DIR/libgmp.a"
else
  echo "libgmp.a nao encontrado apos install em $OUT_DIR" >&2
  exit 1
fi
