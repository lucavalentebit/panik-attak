# ϟ Panik Attak

### Il letto ha perso.

Una sveglia Android che accompagna il risveglio con richiami progressivi e puzzle da risolvere. Crea routine diverse per lavoro, weekend o altre situazioni e tienile attive insieme, ognuna con i propri giorni e orari.

**Android 8+ · In italiano · Offline · Nessun account · Nessuna pubblicità**

[Scarica l’APK 3.0 Alpha 3](Panik-Attak.apk) · [Dettagli tecnici](docs/TECHNICAL.md) · [Piano V3](docs/V3-PLAN.md) · [Prossimi passi](NOTES.md)

## Le pagine

- **Oggi** mostra il prossimo richiamo fra tutte le routine attive, il countdown, gli orari coincidenti e le sequenze in corso.
- **Routine** raccoglie le routine e apre una pagina dedicata a ciascuna, con tutti i controlli e i puzzle già disponibili. Il menù ⋮ gestisce nome, copia ed eliminazione.
- **Abitudini** è pronta nella navigazione; grafici e storico arriveranno in seguito. Nessun dato statistico viene ancora raccolto.
- **Impostazioni**, dall’ingranaggio in alto, è volutamente vuota: verrà completata nella fase 3. I controlli esistenti per permessi e audio restano accessibili nel dettaglio della routine.

## Un risveglio, quattro fasi

| Fase | Cosa succede |
| --- | --- |
| Con calma | I primi richiami, distanziati e progressivi. |
| Ci siamo | Gli intervalli si accorciano. |
| Alzati | Richiami più ravvicinati. |
| Panic Mode | Suono pieno e vibrazione più insistente. |

Puoi personalizzare durata e intervallo di ogni fase. Le prime tre supportano anche un singolo orario fisso. Il conto alla rovescia **hh:mm:ss**, sopra Modifica, mostra quanto manca al prossimo richiamo della routine selezionata e si aggiorna ogni secondo. Se la routine è in pausa, mostra `--:--:--`.

## Routine indipendenti

- Crea, rinomina e duplica le routine; nuove routine e copie partono in pausa.
- Scegli giorni, orari, puzzle, vibrazione e difficoltà crescente per ciascuna.
- Attivane più di una: i richiami coincidenti vengono mantenuti separatamente.
- Mettere in pausa una routine o fermarne la mattina lascia funzionare le altre.

## Cinque modi per svegliarsi

| Puzzle | La sfida |
| --- | --- |
| Matematica | Risolvi un’operazione. |
| Sequenza di colori | Ricorda e ripeti i colori mostrati. |
| Trova l’intruso | Individua il simbolo diverso. |
| Ordinamento | Trascina numeri o emozioni nell’ordine corretto. |
| Tocca la sequenza | Ripeti i passi su una griglia. |

L’ordinamento segue il dito e anima lo scambio. Puoi anche toccare due tessere per scambiarle. Le anteprime permettono di provare i puzzle senza modificare le sveglie.

**Spegni solo questa** risolve il richiamo corrente. **Ferma la mattina**, pausa e modifica di una routine attiva richiedono una sfida difficile. La difficoltà crescente è facoltativa: aumenta dopo i richiami ignorati, separatamente per ciascuna routine.

## La sequenza resta visibile

Dal primo richiamo, una notifica mostra la routine in corso. Mentre suona apre il puzzle; fra i richiami resta una notifica silenziosa con fase e prossimo orario. Più routine in corso vengono riepilogate insieme. Si rimuove quando termini la sequenza, metti in pausa la routine o risolvi l’ultimo richiamo. Una routine soltanto programmata per domani non occupa le notifiche tutto il giorno.

Le notifiche sono impostate come **in corso**, protette da “Cancella tutto”. **Android 14 e successivi consentono comunque lo swipe individuale a telefono sbloccato**: rimuovere la scheda non ferma la routine e non risolve il puzzle. È un [comportamento di Android](https://developer.android.com/about/versions/14/behavior-changes-all#non-dismissable-notifications), quindi non promettiamo una notifica impossibile da rimuovere in ogni situazione.

## Inizia in un minuto

1. Installa [Panik-Attak.apk](Panik-Attak.apk) sul telefono.
2. Apri **Routine**, scegli una routine e usa **Controlla impostazioni** nel suo dettaglio per consentire notifiche, sveglie precise e schermo intero.
3. Imposta orario e giorni, quindi attiva la sequenza.
4. Usa **Prova tra 12 secondi** per verificare il funzionamento sul tuo dispositivo.

Su Samsung, imposta la batteria dell’app su **Senza restrizioni**. Controlla che il volume Sveglia sia udibile e che Non disturbare consenta le sveglie.

All’attivazione l’app riattiva anche la suoneria delle chiamate, almeno al 60%, conservando il filtro e le eccezioni di Non disturbare. Android può richiedere l’accesso dedicato. La pausa non rimette automaticamente il telefono in muto.

Un telefono spento o un’app arrestata forzatamente non può suonare: riapri Panik Attak dopo un arresto forzato.

## Sviluppo

Java 17 · Android nativo · Material Components · Gradle Wrapper

Configura Android SDK 35 in `local.properties` (`sdk.dir=...`) oppure tramite `ANDROID_HOME`, quindi:

```sh
./gradlew assembleDebug lintDebug
```

Su Windows usa `gradlew.bat`. L’APK generato si trova in `app/build/outputs/apk/debug/app-debug.apk`; quello pronto da installare è anche nella radice della repository.

La build distribuita è firmata per debug. I test logici sono in `tests/`, quelli sul dispositivo in `app/src/androidTest/`. Procedura e comportamento dettagliato: [documentazione tecnica](docs/TECHNICAL.md).

## Verifiche

Sviluppata e provata sul Samsung SM-S938B con Android 16. I rapporti descrivono copertura e limiti delle prove:

- [Upgrade e puzzle — 2.0](TEST-REPORT-v2.md)
- [Routine multiple — 2.1](TEST-REPORT-v2.1.md)
- [Controlli e trascinamento — 2.2](TEST-REPORT-v2.2.md)
- [Notifiche delle sequenze — 3.0 Alpha 2](TEST-REPORT-v3-notifications.md)
- [Navigazione e pagine — 3.0 Alpha 3](TEST-REPORT-v3-pages.md)

Le impostazioni necessarie alle sveglie rimangono sul dispositivo; l’app non richiede un account né una connessione internet.
