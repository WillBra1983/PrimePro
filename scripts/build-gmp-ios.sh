#!/usr/bin/env bash
# Compila GMP estático para iphoneos (arm64). macOS / GitHub Actions.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
GMP_VERSION="6.3.0"
GMP_TARBALL="gmp-${GMP_VERSION}.tar.xz"
GMP_URL="https://ftp.gnu.org/gnu/gmp/${GMP_TARBALL}"
OUT_DIR="$ROOT/app/src/main/cpp/gmp/lib/ios-arm64"
BUILD_DIR="$ROOT/build/gmp-ios"
CACHE_DIR="$ROOT/build"
MIN_IOS="16.0"

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
echo "Fonte GMP: $GMP_SRC" >&2

SDK_PATH="$(xcrun --sdk iphoneos --show-sdk-path)"
CLANG="$(xcrun --sdk iphoneos --find clang)"
# Recipe comum iOS: host darwin + disable-assembly (evita erro mp_limb_t 32/64 bits)
IOS_FLAGS="-arch arm64 -isysroot ${SDK_PATH} -miphoneos-version-min=${MIN_IOS} -O3 -target arm64-apple-darwin"
export CC="${CLANG} ${IOS_FLAGS}"
export CXX="$(xcrun --sdk iphoneos --find clang++) ${IOS_FLAGS}"
export CPP="${CLANG} -E ${IOS_FLAGS}"
export CFLAGS="${IOS_FLAGS}"
export CXXFLAGS="${IOS_FLAGS}"
export CPPFLAGS="${IOS_FLAGS}"
export LDFLAGS="-arch arm64 -isysroot ${SDK_PATH} -miphoneos-version-min=${MIN_IOS}"

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR" "$OUT_DIR/lib"
cd "$BUILD_DIR"

echo "Configurando GMP (host=aarch64-apple-darwin, --disable-assembly) ..." >&2
if ! "$GMP_SRC/configure" \
  --host=aarch64-apple-darwin \
  --build="$(uname -m)-apple-darwin" \
  --prefix="$OUT_DIR" \
  --disable-shared \
  --enable-static \
  --disable-assembly \
  --with-pic \
  ABI=64; then
  echo "=== configure falhou; ultimas linhas de config.log ===" >&2
  tail -n 80 "$BUILD_DIR/config.log" >&2 || true
  exit 1
fi

NCPU="$(sysctl -n hw.ncpu 2>/dev/null || echo 4)"
echo "Compilando GMP (make -j${NCPU}) ..." >&2
make -j"$NCPU"
make install

if [[ -f "$OUT_DIR/lib/libgmp.a" && -f "$OUT_DIR/lib/libgmpxx.a" ]]; then
  echo "GMP iOS OK: $OUT_DIR/lib/libgmp.a" >&2
elif [[ -f "$OUT_DIR/lib/libgmp.a" ]]; then
  echo "GMP iOS OK (sem libgmpxx): $OUT_DIR/lib/libgmp.a" >&2
else
  echo "libgmp.a nao encontrado apos install em $OUT_DIR" >&2
  find "$OUT_DIR" -name '*.a' >&2 || true
  exit 1
fi
