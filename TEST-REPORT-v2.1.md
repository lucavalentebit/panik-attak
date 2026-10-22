# Panik Attak 2.1 — verifica del 23 ottobre 2026

APK finale: `Panik-Attak.apk`, versione 2.1 (versionCode 5), build debug installata come aggiornamento sul Samsung SM-S938B Android 16. Versione precedente conservata in `backups/Panik-Attak-v2.0.apk`.

SHA-256: `0cba9ce6dc373ee2299d1dfb2eb6f76cd65bdeca36a4d1acc0f210b33d6a29a2`

## Risultati

- Build completa e compilazione dei test Android riuscite.
- `lintDebug`: 0 errori, 16 avvisi non bloccanti.
- `PlanTest`: 38 verifiche superate.
- `UpgradeTest`: 30044 verifiche superate.
- `RoutineScheduleTest`: 13 verifiche superate su pianificazione simultanea, coincidenze, giorni distinti, pausa e stop isolati, Fisso, mezzanotte e fuso orario.
- `RoutineSmoke`: 35 controlli superati sul Samsung. Migrazione dei valori esistenti, creazione, duplicazione, selezione e cancellazione, configurazioni indipendenti, protezione della routine corretta anche cambiando selezione, risposte errate e obsolete, contatori di difficoltà separati, stop della sola mattina e pausa della sola routine.
- Consegna reale Android di due richiami coincidenti a schermo spento: entrambi conservati, puzzle propri, secondo richiamo ancora attivo dopo la soluzione del primo, schermata aggiornata e programmazione successiva mantenuta.
- Schermate principale e richiami sovrapposti ispezionate: `build/routines-v21.png`, `build/routines-overlap-v21.png`.
- Confronto delle preferenze con la copia precedente ai test: nessuna impostazione originale modificata (esclusa la riprogrammazione del prossimo allarme). Profili temporanei eliminati e suono di prova fermato.

## Limiti

Le nuove prove riguardano il Samsung collegato. Non effettuati test notturni prolungati o riavvio fisico del telefono. Il test della schermata di allarme ammette un'apertura esplicita quando il monitor Android non rileva l'attività: conferma consegna a schermo spento e gestione della schermata sopra il blocco, senza certificare nuovamente l'apertura automatica a schermo intero in ogni condizione. Le verifiche v2.0 restano documentate nel precedente rapporto.
