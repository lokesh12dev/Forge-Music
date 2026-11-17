/** Only the Forge player path on GitHub Pages may use the session bridge. */
export const PRODUCTION_ORIGIN = 'https://lokesh12dev.github.io';
export const SITE_ORIGINS = [PRODUCTION_ORIGIN, ...(process.env.FORGE_STORE_BUILD === '1' ? [] : ['http://localhost:5173'])];
export function matchPatternsFor(origins: string[]): string[] {
  return origins.map(o => o === PRODUCTION_ORIGIN ? `${o}/Forge-Music/player/*` : `${new URL(o).protocol}//${new URL(o).hostname}/*`);
}
