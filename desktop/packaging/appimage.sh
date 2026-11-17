#!/usr/bin/env bash
# Wraps the app image Compose builds (its own Java runtime, FFmpeg and all) in an AppImage:
# one file that runs on any x86-64 Linux after chmod +x, with nothing installed.
#
#   desktop/packaging/appimage.sh <app dir from createDistributable> <output .AppImage>
#
# Downloads appimagetool on first use. Runs it extracted, so no FUSE is needed to build.
set -euo pipefail

app=$(cd "$1" && pwd)
out=$2
here=$(cd "$(dirname "$0")" && pwd)
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

appdir="$work/Forge.AppDir"
mkdir -p "$appdir"
cp -a "$app" "$appdir/forge"

cat > "$appdir/AppRun" <<'EOF'
#!/bin/sh
here=$(dirname "$(readlink -f "$0")")
exec "$here/forge/bin/Forge" "$@"
EOF
chmod +x "$appdir/AppRun"

cat > "$appdir/forge.desktop" <<'EOF'
[Desktop Entry]
Type=Application
Name=Forge
Comment=A music player for YouTube Music
Exec=Forge
Icon=forge
Categories=AudioVideo;Audio;Player;
Terminal=false
EOF
cp "$here/icons/AppIcon.png" "$appdir/forge.png"
ln -s forge.png "$appdir/.DirIcon"

tool="${APPIMAGETOOL:-$work/appimagetool}"
if [ ! -x "$tool" ]; then
  curl -fsSL -o "$tool" https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-x86_64.AppImage
  chmod +x "$tool"
fi
mkdir -p "$(dirname "$out")"
ARCH=x86_64 APPIMAGE_EXTRACT_AND_RUN=1 "$tool" --no-appstream "$appdir" "$out"
echo "Built $out"
