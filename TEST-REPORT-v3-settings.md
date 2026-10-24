# Panik Attak 3.0 Alpha 4 — essential Settings

24 October 2026 · Samsung SM-S938B · Android 16 · versionCode 10 · debug signed.

## Implemented

- Sound: actual alarm-stream volume, Android audio link, DND access/status and explanation of existing call-ringer behavior.
- Notifications: real alarm-channel readiness, Android channel controls, optional silent sequence-status notification (default on), lock-screen name visibility (default private).
- Reliability: exact alarms, full-screen access and battery guidance with Android links. System-controlled states are refreshed on return to the app.
- Unsupported Android destinations fall back to app information. Routine detail and the Oggi setup warning lead to Settings; routine-specific controls stay in detail.
- The two global switches persist immediately. They do not pause routines, change scheduled recalls, regenerate puzzles, adjust system volume or change DND policy.

## Checks

| Verification | Result |
| --- | --- |
| assembleDebug / assembleDebugAndroidTest | Passed |
| lintDebug | 0 errors, 20 warnings |
| SettingsSmoke | 36 checks passed |
| NavigationSmoke | 36 checks passed |
| RoutineSmoke | 35 checks passed, including real coincident alarms with screen off |
| Android destination resolution | All eight sound, DND, notification, exact-alarm, full-screen and app-information actions resolve on Samsung |
| Configuration restoration | All 42 original preference values unchanged; derived next-alarm/occurrence state excluded from comparison |
| APK identity | Root APK matches the final build installed on Samsung, version 3.0-alpha.4 / code 10 |

SettingsSmoke exercises real view callbacks and persisted values, recreates Settings, checks Back to the originating routine, hides/restores the silent status card and updates privacy while a real foreground alarm service is running. It verifies the current puzzle, alarm schedule, ongoing flags and full-screen intent are retained. It also verifies public notification content excludes the routine name, validates the channel/package extras, tests the missing-activity fallback and confirms unchanged system alarm/ringer volumes and DND policy.

System-link tests intercept the activity launch so verification does not change the user's Android settings. Device-side intent resolution separately confirms Samsung has a handler for each destination. We did not toggle real permission grants or notification-channel blocks during this run. Portrait captures of the top and bottom of Settings were visually inspected; the navigation regression also covers landscape and restoration.

The original app configuration was restored after each test. The instrumentation package was removed and the normal application reopened. The previous root APK is preserved locally in `backups/Panik-Attak-v3.0-alpha.3.apk` (ignored by Git).

## Scope limits

Habit history, charts, history export/deletion, widgets and color changes remain deferred. Lock-screen presentation still follows Android's user/channel visibility choices; notification privacy is verified at the published Notification object level, not as a guarantee against every lock-screen configuration. Older Android versions, enlarged fonts and the Samsung battery submenus were not separately exercised. Notification swipe behavior is unchanged from Alpha 2.

APK SHA-256: `DEA0F37B919C7CAB64BD21FD09F8945B1837B8C5689D506819E9A5DBA6CFA5AF`.
