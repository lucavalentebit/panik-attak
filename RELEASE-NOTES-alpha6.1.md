# Panik Attak 3.0 Alpha 6.1 — anteprima e luce

La prova in **Impostazioni → Prove sperimentali → Prova la fotocamera** usa un’anteprima video composta nella pagina, senza lo sfondo nero che poteva coprire la superficie della fotocamera. L’anteprima mantiene le proporzioni e il pulsante di scatto si abilita quando arriva un fotogramma. Nuova foto ripristina l’anteprima e cancella il risultato precedente.

La luce continua parte accesa se supportata; negli altri casi viene richiesto il flash allo scatto oppure segnalata la sua assenza. Si può spegnere la luce per limitare i riflessi sul metallo. Fotocamera e luce vengono rilasciate dopo lo scatto e quando si lascia la schermata. Lo scatto attende la messa a fuoco e analizza il JPEG in background.

I risultati indicano **Forchetta**, **Cucchiaio** e **Coltello** separatamente. Il modello e la soglia del 50% restano invariati: un cucchiaio al 44% resta una rilevazione incerta. Le percentuali sono punteggi del modello, non probabilità calibrate. Tazze e sedie restano disponibili; le cialde di caffè richiedono un modello diverso, come descritto nel [piano di miglioramento](https://github.com/lucavalentebit/panik-attak/blob/v3.0-alpha.6.1/docs/OBJECT-IMPROVEMENTS.md).

La versione è `3.0-alpha.6.1`, codice Android 13. L’APK mantiene la firma debug della Alpha 6, per consentire l’aggiornamento della stessa installazione. Le foto restano sul dispositivo e non entrano nello storico; routine, sveglie e puzzle restano indipendenti dalla prova.

Verifiche completate: compilazione e lint Android, compilazione dei test sul dispositivo, cinque suite logiche preesistenti e 12 nuove verifiche della gestione dei risultati su dati simulati. Questi controlli non misurano l’accuratezza fotografica del modello.

**Anteprima e flash devono ancora essere confermati sul telefono reale.** Il test della fotocamera nell’emulatore cloud non è stato completato per problemi dei servizi Android prima dell’avvio dell’app. Dettagli nel [rapporto della fotocamera](https://github.com/lucavalentebit/panik-attak/blob/v3.0-alpha.6.1/TEST-REPORT-v3-camera.md).

Allegati: `Panik-Attak.apk` e relativo checksum SHA-256 in `Panik-Attak.apk.sha256`.
