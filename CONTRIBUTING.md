# Contributing to Forge

Thank you for wanting to improve Forge. Contributions are welcome — bug
reports, fixes and small features — under the terms below.

## Reporting a problem

Open an issue at https://github.com/lokesh12dev/Forge-Music/issues with what you
did, what you expected, and what happened. Screenshots and steps to reproduce
help most. For security issues, please say so in the issue title and describe
the impact without posting exploit details.

## Discussing ideas

Use https://github.com/lokesh12dev/Forge-Music/discussions before building anything
large. Forge is intentionally focused; a proposal may be declined even when
it is well made.

## Contributor terms

By opening a pull request or otherwise submitting code, documentation or any
other material to this repository you confirm that you accept all of the
following, and the acceptance is recorded by the submission itself:

1. **Confidentiality.** You agree to the non-disclosure terms in LICENSE.
   Unpublished parts of Forge — designs, algorithms, endpoints, keys,
   protocols and roadmap — are confidential to M.Lokesh. You will not
   disclose anything you learn from private reviews, discussions or access,
   and you will not reuse it in other projects, during and after your
   contribution, indefinitely.
2. **Rights assignment.** You assign to M.Lokesh, irrevocably and free of
   charge, all rights in your contribution, including copyright, so that
   Forge can stay a single owned project. You confirm your contribution is
   your own original work (or you say clearly, in the pull request, what it
   builds on, so it can be reviewed properly).
3. **No personal data.** Do not commit secrets, private keys or other
   people's personal data.

## Building and testing

- Android: `./gradlew :app:assembleDebug` and `./gradlew :app:testDebugUnitTest`.
- Web: `pnpm install`, then `pnpm -r test` and `pnpm -r typecheck` under `web/`.
- Desktop: `./gradlew :desktop:test`.

Keep changes small and focused: one intent per pull request, tests for
behaviour changes, and no unrelated reformatting.

## Code style

Match the file you are in. Comments explain why, not what. Names are plain
words; UI strings go through `com.forge.i18n.Tr`.

## Licensing

Forge is proprietary; see LICENSE. Your contribution becomes part of Forge
under the terms above — nothing in this repository grants you, or anyone
else, a licence to redistribute Forge or its source.
