# Alpha 6.2 — prova casuale delle posate

Data: 31 ottobre 2026. Versione `3.0-alpha.6.2`, codice Android 14. Java 17, SDK Android 35, Gradle 8.11.1.

## Comportamento verificato nel codice

La prima richiesta viene scelta fra forchetta, cucchiaio e coltello. Una prova superata permette di estrarre una delle altre due posate; un tentativo fallito mantiene la richiesta. La richiesta viene conservata nel Bundle dell’attività durante la rotazione e resta in memoria al ritorno dal background.

La verifica usa soltanto gli output COCO 47=forchetta, 49=cucchiaio e 48=coltello. Le altre classi vengono escluse prima di scegliere la posata riconosciuta e prima di formattare i risultati. Il successo richiede che la categoria richiesta abbia il punteggio massimo tra le tre, almeno 0,5, senza parità. Il modello e le soglie di confidenza sono invariati rispetto alla Alpha 6.1.

## Test automatici

`tests/objects/ObjectRecognitionTest.java` esegue **73 verifiche delle regole su output simulati**. Coprono:

- Tutte e tre le richieste iniziali e tutte le alternative casuali dopo un successo.
- Ripristino della categoria salvata e fallback per stato mancante o non più valido.
- Successo per ogni posata richiesta e rifiuto di ciascuna delle altre due, anche con punteggio alto.
- Rifiuto se un’altra posata ha un punteggio maggiore o se due categorie sono a pari merito.
- Esclusione completa delle categorie estranee dalla decisione e dai dettagli.
- Soglie del 50% e 25%, cucchiaio al 44%, duplicati, conteggi vuoti/non validi, output di lunghezze diverse, punteggi non validi e identificativi di classe non validi.

Sono passate anche le cinque suite logiche preesistenti di programmazione sveglie, sfide matematiche, puzzle, routine e statistiche. Non sono test fotografici.

`assembleDebug`, `lintDebug` e `assembleDebugAndroidTest` completati. `ObjectCameraSmoke` è stato adattato ai nuovi pulsanti e al controllo della richiesta prima/dopo lo scatto e dopo la rotazione. **Il test strumentale è compilato, non eseguito in questa sessione**: il precedente AVD cloud aveva ripetute interruzioni dei servizi Android prima dell’avvio dell’app, documentate nel [rapporto Alpha 6.1](TEST-REPORT-v3-camera.md).

## Riscontro sul telefono e limiti

L’utente ha confermato che la Alpha 6.1 funziona con le posate. Questo riscontro riguarda il flusso fotografico precedente; non è una misura di precisione o richiamo e non sostituisce la prova della nuova richiesta casuale sul telefono.

Il modello non è stato riaddestrato. La valutazione fotografica delle tre categorie non è stata ripetuta. Sono possibili errori dovuti a riflessi, luce, oggetti piccoli o più posate nell’immagine. L’esperimento non sblocca sveglie e non modifica le loro preferenze.

## APK

L’APK conserva il pacchetto `it.panikattak.app` e la firma debug delle Alpha 6/6.1: SHA-256 del certificato `12b4d56e7d15448031ffda1c109af7763503f6d9b297ad1eb7686c82bc3340e0`. Il checksum del file viene salvato in `Panik-Attak.apk.sha256` e ricontrollato dal workflow di pubblicazione.

## Riproduzione dei controlli delle regole

```sh
mkdir -p /tmp/panik-cutlery-tests
javac -d /tmp/panik-cutlery-tests app/src/main/java/it/panikattak/app/ObjectRecognition.java tests/objects/ObjectRecognitionTest.java
java -cp /tmp/panik-cutlery-tests it.panikattak.app.ObjectRecognitionTest
```

Build e compilazione dei test della fotocamera:

```sh
./gradlew --no-daemon --max-workers=2 -PtestRunner=it.panikattak.app.ObjectCameraSmoke assembleDebug lintDebug assembleDebugAndroidTest
```
