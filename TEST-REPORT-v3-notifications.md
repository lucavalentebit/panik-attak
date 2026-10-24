# Panik Attak 3.0 Alpha 2 — sequence notifications

Date: 24 October 2026. Device: Samsung SM-S938B, Android 16. Build: `3.0-alpha.2`, version code 8, debug signed, installed as an update.

## Delivered

- A silent ongoing notification between recalls, beginning after the first real delivery.
- Routine name, current phase and next recall time; an expandable summary for multiple waiting routines.
- Existing foreground notification during ringing, without a duplicate status for that same routine.
- Cleanup on protected stop/pause or completion; the final unresolved recall stays active past the planned end.
- Persisted occurrence state and validated routine routing. Test alarms do not create real occurrence status.
- No direct stop/dismiss shortcut, notification polling, silent foreground service, or changes to routine configuration.

## Verification

| Check | Result |
| --- | --- |
| `assembleDebug` | Passed |
| `lintDebug` | Passed: 0 errors, 19 warnings |
| `NotificationSmoke` | 29 checks passed; actual AlarmManager delivery, foreground-to-silent transition and protected pause included |
| `RoutineSmoke` | 35 checks passed, including two simultaneous real recalls with screen off and independent solutions |
| User preferences after testing | Exact match to pre-test snapshot, excluding recalculated `next`/`nextBatch` and the new status key |
| Packaging | Updated APK installed on Samsung; test-only package removed |

Notification checks cover enabled-versus-running state, silent channel, ongoing/no-clear flags, public lock-screen payload, occurrence persistence, single-routine routing, overlapping summary, isolated pause, removal during ringing, retention between recalls, stop cleanup, expired occurrences, pending alarms beyond the end, and exclusion of test alarms. The first live-delivery test used an overly short fixed wait; the final test waits for delivery up to 10 seconds and passes.

## Swipe behavior and limits

The unlocked Samsung was tested with a real horizontal gesture. During one run the notification was absent after the swipe while the routine and next alarm remained active; a later run retained the card. This does **not** establish an absolute swipe lock. Samsung did not expose the notification label through the test accessibility query, so the gesture used a position verified from a screenshot.

Android 14+ explicitly permits individual dismissal of ordinary ongoing notifications while unlocked, including foreground-service notifications. They remain protected while locked and against Clear all according to the platform contract. The app requests both ongoing and no-clear behavior; it does not misuse call/media categories or immediately repost on dismissal. [Official Android behavior](https://developer.android.com/about/versions/14/behavior-changes-all#non-dismissable-notifications).

Clear all was not exercised because it would remove unrelated user notifications. Locked swipe behavior, reboot recovery, process-death recovery, channel denial and time-zone changes were not separately exercised in this device run. Public payload and persisted state were checked programmatically; the actual swipe observation was unlocked. Notification permission or channel settings can prevent display.

Pages, statistical event history, habit charts and the widget remain outside this build.
