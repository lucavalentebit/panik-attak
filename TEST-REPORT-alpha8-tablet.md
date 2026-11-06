# Alpha 8 Tablet — verifiche

## Ambito

Presentazione delle schermate esistenti: font, dimensioni dei controlli, spaziature, larghezza e centratura. Nessuna modifica agli algoritmi di pianificazione, ai puzzle, all’audio, alla fotocamera, al riconoscimento o alle statistiche. Il package dedicato `it.panikattak.app.tablet` e l’etichetta `Panik Attak Tablet` permettono l’installazione separata richiesta dall’utente.

## Build e logica

- `./gradlew --offline --no-daemon --max-workers=2 -PtabletApk=true -PtestRunner=it.panikattak.app.TabletLayoutSmoke assembleRelease lintRelease assembleDebugAndroidTest`: PASS.
- Lint release: nessun errore. Gli avvisi sono disponibili nel report Gradle locale.
- Suite Java esistente: **48.039 controlli superati** (pianificazione, puzzle, upgrade, routine multiple, storico).
- APK release: versione `3.0-alpha.8-tablet`, codice `16`, package `it.panikattak.app.tablet`, non debuggabile.
- ABI incluse: `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`.
- Firma APK verificata con `apksigner`; SHA-256 certificato `12b4d56e7d15448031ffda1c109af7763503f6d9b297ad1eb7686c82bc3340e0`.
- Hash del file verificabile tramite `sha256sum -c Panik-Attak-Tab-S11-Ultra.apk.sha256`.

## Layout Android

`TabletLayoutSmoke` controlla le pagine Oggi, Routine, Abitudini, Impostazioni e dettaglio routine, oltre ai sei tipi di puzzle: dimensioni leggibili dei pulsanti, testo completo, assenza di tagli verticali, contenuti centrati, larghezza limitata e spazio disponibile per contenuti e navigazione. Verifica anche la selezione della scala per finestre telefono, tablet medio e tablet grande.

Un primo test a 924 × 1480 dp ha rilevato un comando Anteprima ancora troppo piccolo: corretto prima della distribuzione. La ripetizione sulla stessa interfaccia ha superato **220 controlli**.

Sull’APK definitivo con package separato:

- **924 × 1480 dp, verticale: 223 controlli superati**.
- **1480 × 924 dp, orizzontale: 223 controlli superati**.
- **600 × 960 dp con caratteri di sistema al 130%: 223 controlli superati** (conversione sp/pixel verificata con l’API Android, compatibile con lo scaling non lineare).
- **669 controlli di layout complessivi sull’APK definitivo**.
- Installazione contemporanea all’app originale verificata tramite Package Manager.
- Render Android esaminati: [verticale](docs/tablet-layout-portrait.png) e [orizzontale](docs/tablet-layout-landscape.png).

## Riproduzione

```bash
source /workspace/panik-attak-tools/env.sh
./gradlew --offline --no-daemon --max-workers=2 -PtabletApk=true \
  -PtestRunner=it.panikattak.app.TabletLayoutSmoke \
  assembleRelease lintRelease assembleDebugAndroidTest
adb install -r app/build/outputs/apk/release/app-release.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell wm density 160
adb shell wm size 924x1480
adb shell am instrument -w it.panikattak.app.tablet.test/it.panikattak.app.TabletLayoutSmoke
adb shell wm size 1480x924
adb shell am instrument -w it.panikattak.app.tablet.test/it.panikattak.app.TabletLayoutSmoke
```

Le dimensioni dell’emulatore sono unità logiche Android equivalenti a un tablet grande; non sono una misura della risoluzione fisica del dispositivo. Le immagini nel report sono render delle viste Android reali.

## Limiti

Emulatore AOSP API 35 senza accelerazione hardware: osservati ANR del sistema e lentezza durante l’avvio; non attribuiti al codice dell’app. Nessun Galaxy Tab S11 Ultra fisico disponibile: audio, vibrazione, fotocamera, politiche Samsung e comportamento Android 16 richiedono una prova sul dispositivo. Il modello di riconoscimento e le sue soglie sono invariati.

La variante separata non migra dati dall’altra app. Routine, storico e autorizzazioni sono indipendenti; il file `Panik-Attak.apk` preesistente rimane invariato.
