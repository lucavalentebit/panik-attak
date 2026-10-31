# Alpha 7 — sfida fotografica della sveglia

31 ottobre 2026. Versione `3.0-alpha.7`, codice Android 15. Java 17, SDK Android 35, Gradle 8.11.1.

## Implementazione

`Puzzle.CAMERA` è il sesto tipo selezionabile. La sfida persistita contiene categoria, identificatore casuale e operazione matematica alternativa. `ObjectTestActivity` usa lo stesso percorso di acquisizione e inferenza della prova nelle Impostazioni; in modalità puzzle restituisce un risultato solo dopo il riconoscimento positivo della categoria richiesta. Il risultato contiene l’identità completa della sfida e viene confrontato con quella del chiamante.

Il richiamo si risolve solo premendo Spegni solo questa dopo la foto o l’operazione alternativa. `Scheduler.dismissCurrent` conserva i controlli su routine, orario, puzzle e risposta. Le azioni protette usano il tipo selezionato al livello massimo; per la fotocamera tutti i livelli richiedono una sola posata. L’alternativa matematica richiede sempre due passaggi al livello difficile e viene validata anche al confine con Scheduler.

Modello, categorie e soglie non cambiano. La prova autonoma delle Impostazioni non restituisce soluzioni alle sveglie. Le foto non vengono scritte in preferenze o nello storico.

## Verifiche eseguite

- `PlanTest`: **38** controlli.
- `WakeChallengeTest`: **8.011** controlli.
- `UpgradeTest`: **39.946** controlli, comprese generazione dei sei tipi, salvataggio della selezione fotografica, persistenza della categoria, risultati fotografici corrispondenti/obsoleti e alternativa matematica difficile a ogni livello.
- `RoutineScheduleTest`: **13** controlli.
- `HabitStatsTest`: **31** controlli.
- `ObjectRecognitionTest`: **73** controlli su output sintetici, comprese categorie errate, parità e soglie.
- `assembleDebug`, `lintDebug`, `assembleDebugAndroidTest`: completati. Lint senza errori, 41 avvisi.

`GateSmoke` include ora anche il sesto tipo. `ObjectChallengeSmoke` verifica selezione persistita, anteprima, modifica protetta, pausa, risultato annullato/obsoleto, conferma esplicita del richiamo e alternativa matematica tramite i controlli UI. I risultati fotografici di questa suite sono sintetici: non misurano il modello. Entrambi i runner e `ObjectCameraSmoke` sono compilati.

## Limiti

Nessun telefono o emulatore disponibile in `adb devices`: i test strumentali sono **compilati, non eseguiti**. Da verificare sul telefono: foto reali delle tre posate, flusso dallo schermo bloccato, permesso negato/concesso, fotocamera occupata, interruzioni/rotazione, suono fino alla conferma e routine simultanee. La precisione fotografica non è stata nuovamente misurata.

## Firma e pubblicazione

L’APK Alpha 6.2 recuperato dalla repo usa il certificato `12b4d56e7d15448031ffda1c109af7763503f6d9b297ad1eb7686c82bc3340e0`. Il debug keystore di questo PC ha certificato `b203ce1c072059ef9f70ccf22492d34240cecae2f8e5eb028491302b44dbf01c`: l’APK compilato localmente non può aggiornare direttamente la Alpha 6.2.

Perciò l’APK distribuito nella radice e il relativo checksum restano Alpha 6.2. L’APK Alpha 7 di verifica è in `app/build/outputs/apk/debug/app-debug.apk`, escluso dal commit. Il workflow `release-alpha7.yml` verifica versione, checksum e certificato prima di pubblicare; il tag di release va inviato dopo aver firmato con la chiave originale e aggiornato l’APK nella radice.

## Riproduzione

Compilare in una cartella temporanea `Plan.java`, `WakeChallenge.java`, `Puzzle.java`, `RoutineSchedule.java`, `HabitStats.java`, `ObjectRecognition.java` insieme ai file Java in `tests/`, poi eseguire le sei classi test nel package `it.panikattak.app`.

```sh
./gradlew --no-daemon --max-workers=2 assembleDebug lintDebug assembleDebugAndroidTest -PtestRunner=it.panikattak.app.ObjectChallengeSmoke
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w it.panikattak.app.test/it.panikattak.app.ObjectChallengeSmoke
```

Eseguire l’installazione solo su un dispositivo di test con firma compatibile. Non disinstallare la build dell’utente per aggirare la differenza di firma: si perderebbero i dati locali.
