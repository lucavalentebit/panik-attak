# Panik Attak 3.0 Alpha 5 — statistics and local history

24 October 2026 · Samsung SM-S938B · Android 16 · versionCode 11 · debug signed.

## Delivered

- Abitudini with the existing lime/olive accents, dark cards, typography and rounded controls.
- Persisted 7/30/90-day and routine filters; median first response and explicit-stop time; Panic reach rate with sample counts.
- Elapsed/local-time scatter plot with distinct response/ending marks, separate stop/completion/pause histograms, phase and weekday breakdowns, text alternatives and session timelines.
- Local SQLite history with stable event IDs, historical name/configuration snapshots, scheduled/delivered/displayed/solved/accepted timestamps and monotonic timing context. No answer text.
- Recording, retention, CSV export and confirmed erasure controls. Test/preview/edit purposes are excluded; partial, interrupted and still-running sessions do not become successful summary samples.

## Verification

| Check | Result |
| --- | --- |
| assembleDebug / assembleDebugAndroidTest | Passed |
| lintDebug | 0 errors, 28 warnings |
| HabitStatsTest | 31 deterministic aggregation checks passed |
| HabitsSmoke | 33 device checks passed |
| NavigationSmoke | 36 device checks passed |
| RoutineSmoke | 35 device checks passed, including real simultaneous screen-off alarms |
| SettingsSmoke | 36 device checks passed, including live foreground notification updates |

**171 checks total: 140 on the Samsung and 31 pure calculation checks.**

The deterministic fixture hand-checks a five-minute first-response median, sixteen-minute explicit-stop median, seven complete sessions with one Panic reach, separate terminal-reason bins and routine/date filters. Additional cases cover duplicate replay, preview/test/edit exclusion, midnight, daylight-saving time, changed event offsets, invalid wall-clock/boot spans, missing first recalls, unknown outcomes, completed-session immutability and CSV quoting/formula protection.

Device verification uses separate `instrumentation-` databases. It exercises the actual puzzle solve and accepted-dismissal callbacks, confirms that a solved-but-not-dismissed alarm is still running, preserves renamed routine snapshots, aggregates wrong attempts, records superseded recalls and only marks natural completion after the final accepted dismissal. Changing recording during an active alarm leaves the alarm running and marks coverage partial.

The page is checked empty and populated, with persisted filters, activity recreation, small-sample summaries and both plot modes. Portrait captures and a 1.5× in-app font configuration were inspected; navigation regression covers landscape. Filtered CSV dispatch is checked through Android's document-creation action, and actual CSV output is written and parsed through ContentResolver. Outbox replay is simulated twice to verify idempotency; retention and erase include pending events. An intentionally unusable test database and outbox confirm delivery and valid dismissal still succeed despite storage failure.

The original routine preferences are restored after each test. All 42 original preference entries were compared after final installation; routine values were retained, excluding derived schedule state. The production history database was separately checked and contains zero events, confirming fixture isolation. The test package, test databases and temporary export were removed. Fixture screenshots are explicitly labelled as test data in the README.

## Limits and remaining V3 work

Full cold-boot/first-unlock behavior was not exercised by rebooting the user's phone; the storage/import path was tested with an isolated outbox. Android versions before 16 and maximum system font scaling were not separately device-tested. The document picker action and file output were verified without saving a test file through the user's actual cloud/document provider. Lint warnings are non-blocking style/compatibility findings, including explicit synchronous preference boundaries and programmatic chart constructors.

History begins with this build and does not reconstruct Alpha 1–4 responses. Active-visible puzzle duration, optional solve-time distributions, cross-period comparisons, IQR and the widget remain future work. This release does not claim to measure waking, sleep or leaving the bed, and it does not change Android notification swipe behavior.

The delivered root `Panik-Attak.apk` matches the installed production build. Its SHA-256 is supplied in `Panik-Attak.apk.sha256` and verified against GitHub's uploaded release-asset digest before publication.
