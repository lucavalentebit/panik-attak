# Panik Attak 2.0 — verifica del 22 ottobre 2026

APK finale: `Panik-Attak.apk`, versione 2.0 (versionCode 4). Build debug firmata con lo stesso certificato della versione precedente, installata come aggiornamento sul Samsung SM-S938B con Android 16. Originale conservato in `backups/Panik-Attak-v1.2.apk`.

SHA-256 APK: `fc0e22236a386c2b988a72548f8c842d17c891119761e50f704ec4b3b1024b4a`

## Risultati

- Build `assembleDebug` e compilazione dei test Android completate.
- `lintDebug`: 0 errori, 14 avvisi (compatibilità, dipendenze e indicazioni non bloccanti).
- `PlanTest`: 38 verifiche superate.
- `WakeChallengeTest`: 8011 verifiche superate.
- `UpgradeTest`: 30044 verifiche superate su generazione, risposte, persistenza, difficoltà, Fisso, mezzanotte, fusi orari e cambio ora legale.
- `GateSmoke` sul Samsung: 49 controlli superati. Tipi di puzzle, anteprime, protezioni, risposte errate, pausa, stop della mattina, risposte riferite a vecchi richiami, persistenza, apertura MaterialTimePicker.
- `UpgradeSmoke` sul Samsung: 26 controlli superati. Riproduzione e ripetizione dopo errore delle sequenze, risoluzione di tutti i puzzle, trascinamento touch reale, ricezione della sveglia a schermo spento, apertura automatica sopra il blocco schermo, servizio audio in primo piano e spegnimento con risposta corretta.
- `ConfigSmoke` sul Samsung: 9 controlli superati. Quadrante iniziale, tastiera di fallback, blocco del salvataggio per Fisso fuori finestra, salvataggio di Fisso valido e divieto in Panic Mode.
- Schermate dei puzzle, Fisso e quadrante ispezionate in `build/device-screens/files/`.
- Preferenze iniziali del Samsung ripristinate; sequenza originale attiva e sveglia di prova fermata. Nessuna modifica al volume o a Non disturbare durante i nuovi test.

## Limiti della verifica

Non eseguiti test su un secondo telefono Android stock, prove notturne prolungate, riavvio fisico del telefono o valutazioni in condizioni di sonnolenza. La logica di ricalcolo temporale è coperta dai test automatici; la ricezione reale è stata verificata sul Samsung collegato.

Il quadrante usa il comportamento del componente Material: commutazione alla tastiera tramite il pulsante dedicato, formato 24 ore con doppio anello. Le fasi restano consecutive; modificare un confine sposta gli orari successivi. Per i cambi di ora legale, i richiami fissi vengono risolti nel fuso locale e scartati se non ricadono nella finestra effettiva della fase.
