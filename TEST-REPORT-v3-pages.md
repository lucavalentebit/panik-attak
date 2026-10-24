# Panik Attak 3.0 Alpha 3 — pages

24 October 2026 · Samsung SM-S938B · Android 16 · versionCode 9 · debug signed.

## Implemented

- Persistent top bar and bottom navigation: Oggi, Routine, Abitudini.
- Oggi: earliest actual recall across all enabled routines, equal-time count and chooser, countdown, running occurrences, up to three other routines, setup warning only when needed.
- Separate routine list and detail; per-routine switch and management menu. Existing editors, previews, test alarm, permission controls and puzzle gates retained.
- Abitudini placeholder with no fabricated statistics; empty Settings body, reached from the top gear and reserved for phase 3.
- Back returns from Settings to its originating page, from detail to the routine list, and from other tabs to Oggi. Destination, routine identity and scroll positions survive activity recreation.
- Single notification routes to the matching routine detail; summary routes to Oggi; stale occurrence identity does not select a different occurrence.

## Checks

| Verification | Result |
| --- | --- |
| assembleDebug | Passed |
| lintDebug | 0 errors, 20 warnings |
| NavigationSmoke | 36 checks passed |
| RoutineSmoke | 35 checks passed, including real coincident alarms with screen off |
| CountdownSmoke | 6 checks passed, including ticking, paused detail and hours beyond 24 |

Navigation tests exercise actual view callbacks, independent routine selection, the global countdown, correct detail entry, hard puzzle before editing an active routine, rejection of wrong answers, cancellation without a changed schedule, empty Settings, Back, recreation of Settings and detail, retained scroll, landscape, notification routing, running-state display and the no-enabled-routines state. Portrait pages and landscape were visually inspected from device captures. User settings are restored after each run.

## Scope limits

Global settings are intentionally not implemented. Habit history, charts, widget, new colors and a unified draft editor remain deferred. The existing dialog editors retain their existing behavior; this iteration verifies page restoration rather than promising restoration of every unsaved dialog draft. Enlarged system font and narrow-device emulator configurations were not separately tested. Notification swipe behavior remains as documented for Alpha 2.

The ready-to-install APK is `Panik-Attak.apk` at the repository root. The Samsung receives the same production APK; the instrumentation package is removed after verification.
