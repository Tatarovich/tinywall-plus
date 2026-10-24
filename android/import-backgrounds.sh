#!/usr/bin/env bash
# Converts user images from ../backgrounds/ into assets and regenerates scenes.txt.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
SRC="$ROOT/../backgrounds"
ASSETS="$ROOT/assets"
CWEBP="${CWEBP:-$(command -v cwebp || true)}"

if [ -z "$CWEBP" ]; then
  echo "cwebp not found; install webp tools" >&2
  exit 1
fi

rm -f "$ASSETS"/bg_user*.webp
TMP="$ASSETS/scenes.txt.tmp"
: > "$TMP"

i=0
while IFS= read -r file; do
  i=$((i + 1))
  id="user$i"
  base="$(basename "$file")"
  name="${base%.*}"
  label="$(printf '%s' "$name" | sed 's/^[0-9][0-9]*[ _-]*//; s/[_-]/ /g; s/  */ /g; s/^ //; s/ $//')"
  [ -n "$label" ] || label="Фон $i"
  "$CWEBP" -quiet -q 86 -resize 1920 0 "$file" -o "$ASSETS/bg_$id.webp"
  printf '%s|%s\n' "$id" "$label" >> "$TMP"
done < <(find "$SRC" -maxdepth 1 -type f \( -iname '*.jpg' -o -iname '*.jpeg' -o -iname '*.png' -o -iname '*.webp' \) -print | LC_ALL=C sort)

if [ "$i" -eq 0 ]; then
  printf 'aurora|Сияние\ndusk|Сумерки\nforest|Лес\nspace|Космос\n' > "$TMP"
fi

mv "$TMP" "$ASSETS/scenes.txt"
echo "backgrounds: $i user image(s)"
