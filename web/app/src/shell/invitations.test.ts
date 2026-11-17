import { describe, expect, it } from 'vitest';
import { shouldAsk } from './invitations';

const day = 86_400_000;

describe('monthly invitations', () => {
  it('waits five days of use before asking for the star', () => {
    expect(shouldAsk({ days: 4, asked: {}, never: {} }, 'star', 100 * day)).toBe(false);
    expect(shouldAsk({ days: 5, asked: {}, never: {} }, 'star', 100 * day)).toBe(true);
  });
  it('asks at most once in thirty days', () => {
    expect(shouldAsk({ days: 40, asked: { star: 100 * day }, never: {} }, 'star', 129 * day)).toBe(false);
    expect(shouldAsk({ days: 40, asked: { star: 100 * day }, never: {} }, 'star', 130 * day)).toBe(true);
  });
  it('never asks again once turned down', () => {
    expect(shouldAsk({ days: 400, asked: {}, never: { star: true } }, 'star', 500 * day)).toBe(false);
  });
});
