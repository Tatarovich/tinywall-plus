#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
SDK="${SDK:-/tmp/dsh-sdk}"
BUILD="/tmp/tihiy-build"
OUT="$ROOT/../dist"
VERSION="${VERSION:-0.10}"
NAME="TinyWallPlus-$VERSION"
rm -rf "$BUILD"
mkdir -p "$BUILD" "$OUT"
cp -R "$ROOT/AndroidManifest.xml" "$ROOT/res" "$ROOT/src" "$ROOT/assets" "$BUILD/"

cd "$BUILD"
AAPT2="$SDK/build-tools/35.0.0/aapt2"
ANDROID_JAR="$SDK/platforms/android-28/android.jar"
D8="$SDK/build-tools/35.0.0/d8"
ZIPALIGN="$SDK/build-tools/35.0.0/zipalign"
APKSIGNER="$SDK/build-tools/35.0.0/apksigner"

"$AAPT2" compile --dir res -o compiled.zip
"$AAPT2" link -o unsigned.apk -I "$ANDROID_JAR" --manifest AndroidManifest.xml \
  -R compiled.zip --java gen --auto-add-overlay -A assets

javac -source 8 -target 8 -Xlint:-options \
  -classpath "$ANDROID_JAR" -d classes \
  $(find src gen -name '*.java')

mkdir -p dex
"$D8" --lib "$ANDROID_JAR" --min-api 28 --output dex $(find classes -name '*.class')
cp unsigned.apk unaligned.apk
zip -q -j unaligned.apk dex/classes.dex
"$ZIPALIGN" -f -p 4 unaligned.apk aligned.apk

# The release key is private: it is not committed, and its password is only read
# from the environment. Without KEYSTORE_PASS the build still works, but the APK
# stays unsigned, which is fine for testing changes locally.
KEYSTORE="${KEYSTORE:-$ROOT/keystore.jks}"
KEY_ALIAS="${KEY_ALIAS:-tihiy}"

if [ -n "${KEYSTORE_PASS:-}" ]; then
  [ -f "$KEYSTORE" ] || { echo "keystore not found: $KEYSTORE" >&2; exit 1; }
  jarsigner -keystore "$KEYSTORE" -storepass:env KEYSTORE_PASS -keypass:env KEYSTORE_PASS \
    -sigalg SHA256withRSA -digestalg SHA-256 -signedjar signed.apk aligned.apk "$KEY_ALIAS" >/dev/null
  "$APKSIGNER" sign --ks "$KEYSTORE" --ks-key-alias "$KEY_ALIAS" \
    --ks-pass env:KEYSTORE_PASS --key-pass env:KEYSTORE_PASS \
    --v2-signing-enabled true --v3-signing-enabled true --v4-signing-enabled false \
    --out "$OUT/$NAME.apk" signed.apk
  "$APKSIGNER" verify --verbose --min-sdk-version 21 --max-sdk-version 27 "$OUT/$NAME.apk" | head -4
  "$APKSIGNER" verify --verbose --min-sdk-version 28 --max-sdk-version 28 "$OUT/$NAME.apk" | head -4
  ls -lh "$OUT/$NAME.apk"
else
  cp aligned.apk "$OUT/$NAME-unsigned.apk"
  echo "KEYSTORE_PASS is not set: built unsigned -> $OUT/$NAME-unsigned.apk" >&2
  ls -lh "$OUT/$NAME-unsigned.apk"
fi
