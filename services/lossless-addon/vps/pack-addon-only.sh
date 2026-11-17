#!/usr/bin/env bash
# Builds forge-addon-only.tar.gz and .zip: the add-on without the owner dashboard or the usage dashboard.
#   bash services/lossless-addon/vps/pack-addon-only.sh [output directory]
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC="$(dirname "$HERE")"
OUT="${1:-$PWD}"
STAGE="$(mktemp -d)"
trap 'rm -rf "$STAGE"' EXIT
mkdir -p "$OUT" "$STAGE/forge-addon"
cp "$SRC"/server.mjs "$SRC"/worker.mjs "$SRC"/cache.mjs "$SRC"/pool.mjs "$SRC"/auth.mjs "$SRC"/security.mjs "$SRC"/attestation.mjs "$STAGE/forge-addon/"
cp "$HERE"/install.sh "$STAGE/forge-addon/"
cp "$HERE"/forge-addon-only.env "$STAGE/forge-addon/forge-addon.env"
cp "$HERE"/README-addon-only.md "$STAGE/forge-addon/README.md"
# Tells the installer this is the add-on alone.
printf 'add-on only: no owner dashboard, no usage dashboard\n' > "$STAGE/forge-addon/ADDON_ONLY"
tar -czf "$OUT/forge-addon-only.tar.gz" -C "$STAGE" forge-addon
echo "$OUT/forge-addon-only.tar.gz"
python3 - "$STAGE" "$OUT" <<'PYZIP'
import pathlib,sys,zipfile,hashlib
stage,out=map(pathlib.Path,sys.argv[1:])
archive=out/'forge-addon-only.zip'
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
 for f in sorted((stage/'forge-addon').rglob('*')):
  if f.is_file(): z.write(f,f.relative_to(stage))
(out/'forge-addon-only.zip.sha256').write_text(hashlib.sha256(archive.read_bytes()).hexdigest()+'  '+archive.name+'\n')
print(archive)
PYZIP
