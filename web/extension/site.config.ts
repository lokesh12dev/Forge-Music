/** Only Forge player pages on GitHub Pages may use the session bridge. */
export const PRODUCTION_ORIGIN = 'https://lokesh12dev.github.io';
export const LEGACY_PRODUCTION_ORIGIN = 'https://justshivv.github.io';
export const SITE_ORIGINS = [
  PRODUCTION_ORIGIN,
  LEGACY_PRODUCTION_ORIGIN,
  ...(process.env.OPENTUNE_STORE_BUILD === '1' ? [] : ['http://localhost:5173']),
];
export function matchPatternsFor(origins: string[]): string[] {
  return origins.flatMap((origin) => {
    if (origin === PRODUCTION_ORIGIN) return [origin + '/newforgemusic/player/*'];
    if (origin === LEGACY_PRODUCTION_ORIGIN) return [origin + '/Opentune/player/*'];
    return [new URL(origin).origin + '/*'];
  });
}
