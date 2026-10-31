# Alpha 6.1 — fotocamera e risultati delle posate

Data: 31 ottobre 2026. Ambiente cloud Linux, Java 17, Android SDK 35, Gradle 8.11.1. APK `3.0-alpha.6.1`, versionCode 13.

## Verifiche completate

| Verifica | Esito |
| --- | --- |
| `assembleDebug` | PASS |
| `lintDebug` | PASS: nessun errore, 40 avvisi; includono stringhe italiane nel codice e uso di ExifInterface del framework |
| `assembleDebugAndroidTest` con `ObjectCameraSmoke` | PASS: test strumentale compilato |
| Cinque suite Java preesistenti | PASS: 38.137 controlli |
| `ObjectRecognitionTest` | PASS: 12 controlli |
| Firma e checksum dell’APK | PASS |
| Versione del pacchetto | PASS: codice 13, nome `3.0-alpha.6.1` |
| Modello incluso | Identico alla Alpha 6, SHA-256 `42fb3d70ffb7bb37dd518f730f7be784b831c2078f30c497d0019cc2e987fa26` |

I test del riconoscimento verificano categorie delle posate separate, cucchiaio al 44% ancora incerto, soglie del 50% e 25%, rilevazioni duplicate, rispetto del numero di output, punteggi non validi, classi estranee e limite sulle gambe della sedia. Non misurano l’accuratezza fotografica del modello.

La firma debug coincide con quella dell’APK Alpha 6 presente prima della modifica: SHA-256 del certificato `12b4d56e7d15448031ffda1c109af7763503f6d9b297ad1eb7686c82bc3340e0`. Il file `Panik-Attak.apk.sha256` accompagna il nuovo APK.

## Fotocamera: limiti della verifica cloud

Il primo tentativo nell’AVD AOSP API 35 senza KVM non ha raggiunto l’avvio dell’app: il sistema Android ha perso i servizi Package Manager e Activity Manager durante l’avvio. L’installazione ha restituito `Broken pipe` / `Can't find service: package`; il lancio strumentale ha trovato `IActivityManager` nullo. I log riportano `DeadSystemException` anche per System UI e Telefono. Questi errori precedono l’esecuzione della pagina fotocamera.

Dopo il completamento dell’avvio (`sys.boot_completed=1`) è stato riprovato il caricamento dell’APK con installazione non streaming. Anche questo tentativo ha restituito `Failure calling service package: Broken pipe (32)` e non ha raggiunto il test. L’emulatore avviato per la prova è stato arrestato. **Il test strumentale della fotocamera non è stato eseguito con successo.**

Il test `ObjectCameraSmoke` è predisposto per verificare arrivo di fotogrammi con contenuto visibile, scatto, inferenza, foto mostrata, riapertura dell’anteprima, rilascio della fotocamera, ritorno dal background, interruzione dell’autofocus e rotazione. Verifica anche i parametri flash se il dispositivo li espone, altrimenti il comportamento senza flash. La compilazione del test non equivale al suo superamento.

Restano da confermare su un telefono reale la visibilità e le proporzioni dell’anteprima, il comportamento del LED, l’esposizione delle posate riflettenti e il riconoscimento nelle condizioni d’uso. Non è stata misurata una maggiore accuratezza del modello e non è stato addestrato un modello per le cialde. Il [piano delle quattro classi](docs/OBJECT-IMPROVEMENTS.md) descrive il lavoro necessario.

## Riproduzione

Con Java 17 e SDK Android 35 configurati:

```sh
./gradlew --no-daemon --max-workers=2 -PtestRunner=it.panikattak.app.ObjectCameraSmoke assembleDebug lintDebug assembleDebugAndroidTest
adb install -r -g app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w it.panikattak.app.test/it.panikattak.app.ObjectCameraSmoke
```

Usare un dispositivo di test. Il test non modifica le preferenze delle sveglie e salva soltanto screenshot della prova nella directory esterna privata dell’app.

Per i 12 controlli Java del riconoscimento:

```sh
mkdir -p /tmp/panik-object-tests
javac -d /tmp/panik-object-tests app/src/main/java/it/panikattak/app/ObjectRecognition.java tests/objects/ObjectRecognitionTest.java
java -cp /tmp/panik-object-tests it.panikattak.app.ObjectRecognitionTest
```
