#!/usr/bin/env bash
# Builds forge-addon-vps.tar.gz: everything the VPS needs, in one folder.
#   bash services/lossless-addon/vps/pack.sh [output directory]
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC="$(dirname "$HERE")"
OUT="${1:-$PWD}"
STAGE="$(mktemp -d)"
trap 'rm -rf "$STAGE"' EXIT
mkdir "$STAGE/forge-addon"
cp "$SRC"/server.mjs "$SRC"/worker.mjs "$SRC"/cache.mjs "$SRC"/pool.mjs "$SRC"/usage-server.mjs "$SRC"/auth.mjs "$SRC"/security.mjs "$SRC"/attestation.mjs "$SRC"/control.mjs "$SRC"/control.html "$SRC"/control.js "$SRC"/control.css "$STAGE/forge-addon/"
mkdir -p "$OUT" "$STAGE/forge-addon/usage-api/dashboard"
USAGE="$SRC/../../usage-api"
cp "$USAGE"/worker.mjs "$USAGE"/auth.mjs "$USAGE"/addons.mjs "$USAGE"/schema.sql "$STAGE/forge-addon/usage-api/"
cp "$USAGE"/dashboard/index.html "$USAGE"/dashboard/dashboard.js "$USAGE"/dashboard/dashboard.css "$USAGE"/dashboard/model.mjs "$USAGE"/dashboard/session.mjs "$STAGE/forge-addon/usage-api/dashboard/"
cp "$HERE"/import-usage.mjs "$STAGE/forge-addon/"
cp "$HERE"/install.sh "$HERE"/forge-addon.env "$HERE"/README.md "$STAGE/forge-addon/"
tar -czf "$OUT/forge-addon-vps.tar.gz" -C "$STAGE" forge-addon
echo "$OUT/forge-addon-vps.tar.gz"

python3 - "$STAGE" "$OUT" <<'PYZIP'
import pathlib,sys,zipfile,hashlib
stage,out=map(pathlib.Path,sys.argv[1:])
archive=out/'forge-hostinger.zip'
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
 for f in sorted((stage/'forge-addon').rglob('*')):
  if f.is_file(): z.write(f,f.relative_to(stage))
(out/'forge-hostinger.zip.sha256').write_text(hashlib.sha256(archive.read_bytes()).hexdigest()+'  '+archive.name+'\n')
print(archive)
PYZIP
