// Zips the built extension for the site's Extension page and writes the
// manifest the page checks for updates:
//
//   web/app/public/downloads/forge-extension-chrome.zip
//   web/app/public/downloads/forge-extension-firefox.zip
//   web/app/public/downloads/forge-extension-<browser> (<version>).zip
//   web/app/public/downloads/extension.json    { version, builtAt, files }
//
// The stable names avoid URL-escaping problems. Each zip holds a single
// versioned folder so "Extract here" leaves one tidy folder to load unpacked.
//
//   corepack pnpm --dir web/extension package   (builds both, then runs this)

import { mkdirSync, readdirSync, readFileSync, rmSync, statSync, writeFileSync } from 'node:fs';
import { join, relative, sep } from 'node:path';
import { fileURLToPath } from 'node:url';
import { zipSync } from 'fflate';

const output = fileURLToPath(new URL('../.output/', import.meta.url));
const target = fileURLToPath(new URL('../../app/public/downloads/', import.meta.url));
mkdirSync(target, { recursive: true });
// Keep only the current versioned zips and stable download aliases.
for (const name of readdirSync(target)) if (/^(?:forge|opentune)-extension-.*\.zip$/.test(name)) rmSync(join(target, name));

function filesUnder(dir) {
  return readdirSync(dir).flatMap((name) => {
    const path = join(dir, name);
    return statSync(path).isDirectory() ? filesUnder(path) : [path];
  });
}

const builds = { chrome: 'chrome-mv3', firefox: 'firefox-mv3' };
const files = {};
let version;

for (const [browser, folder] of Object.entries(builds)) {
  const root = join(output, folder);
  const manifest = JSON.parse(readFileSync(join(root, 'manifest.json'), 'utf8'));
  if (version && manifest.version !== version) {
    throw new Error(`Chrome and Firefox builds disagree on the version (${version} vs ${manifest.version}); rebuild both`);
  }
  version = manifest.version;

  // Keep the version on the folder inside both archives: "forge-extension-chrome (0.4.3)".
  const parent = `forge-extension-${browser} (${manifest.version})`;
  const entries = {};
  for (const file of filesUnder(root)) {
    entries[`${parent}/${relative(root, file).split(sep).join('/')}`] = readFileSync(file);
  }
  const archive = zipSync(entries, { level: 9 });
  const versionedName = `${parent}.zip`;
  const stableName = `forge-extension-${browser}.zip`;
  writeFileSync(join(target, versionedName), archive);
  writeFileSync(join(target, stableName), archive);
  files[browser] = stableName;
  console.log(`${folder} -> app/public/downloads/${stableName} (${parent}/, v${version})`);
}

writeFileSync(
  join(target, 'extension.json'),
  `${JSON.stringify({ version, builtAt: new Date().toISOString(), files }, null, 2)}\n`,
);
console.log(`app/public/downloads/extension.json (v${version})`);

