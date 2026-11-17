/**
 * The monthly invitation, as on Android: a GitHub star after five days of use.
 * It comes at most once every 30 days, so it never nags. Kept in this browser
 * only; when storage is blocked, nothing is shown.
 */
export type Invitation = 'star';

export const GITHUB_URL = 'https://github.com/lokesh12dev/Forge-Music';

const KEY = 'forge:invitations';
const DAY = 86_400_000;
const GAP = 30 * DAY;
const DAYS_BEFORE: Record<Invitation, number> = { star: 5 };

interface State {
  lastDay: number;
  days: number;
  asked: Partial<Record<Invitation, number>>;
  never: Partial<Record<Invitation, boolean>>;
}

function read(): State | null {
  try {
    const value = JSON.parse(localStorage.getItem(KEY) ?? 'null') as Partial<State> | null;
    return { lastDay: value?.lastDay ?? -1, days: value?.days ?? 0, asked: value?.asked ?? {}, never: value?.never ?? {} };
  } catch {
    return null;
  }
}

function write(state: State) {
  try { localStorage.setItem(KEY, JSON.stringify(state)); } catch { /* Storage blocked: the invitation just won't come back. */ }
}

/** Counts today as a day of use, once a day. */
export function usedToday(now = Date.now()): void {
  const state = read();
  if (!state) return;
  const day = Math.floor(now / DAY);
  if (state.lastDay === day) return;
  write({ ...state, lastDay: day, days: state.days + 1 });
}

export function shouldAsk(state: Pick<State, 'days' | 'asked' | 'never'>, invitation: Invitation, now: number): boolean {
  return !state.never[invitation] && state.days >= DAYS_BEFORE[invitation] && now - (state.asked[invitation] ?? 0) >= GAP;
}

/** The invitation due now, if any. */
export function due(now = Date.now()): Invitation | null {
  const state = read();
  if (!state) return null;
  return (['star'] as const).find((invitation) => shouldAsk(state, invitation, now)) ?? null;
}

/** Shown (or turned down for good): not asked again for a month, or ever. */
export function answered(invitation: Invitation, never: boolean, now = Date.now()): void {
  const state = read();
  if (!state) return;
  write({ ...state, asked: { ...state.asked, [invitation]: now }, never: never ? { ...state.never, [invitation]: true } : state.never });
}
