# Panik Attak

Sveglia Android nativa, in italiano. Nessun account, pubblicità o connessione internet.

## Uso

- In **Le tue routine**, usa **Nuova routine** per aggiungerne una. Tocca il nome per configurarla; gli interruttori permettono di tenerne attive più di una insieme. **Gestisci** permette di rinominare, duplicare o eliminare la routine selezionata.
- Tocca **Modifica** per scegliere la prima sveglia della routine selezionata.
- Tocca una fase per cambiarne inizio, fine e intervallo (1–30 minuti), oppure scegliere **Fisso** e un singolo orario. Le fasi restano consecutive, con durata da 1 a 120 minuti; le fasi successive si spostano insieme. Panic Mode mantiene sempre un intervallo.
- Scegli i giorni della settimana, quindi **Attiva la sequenza**.
- All'attivazione, la suoneria delle chiamate esce dal muto e viene portata almeno al 60% (un volume già più alto viene conservato). Non disturbare e le sue eccezioni, compresi i preferiti, restano invariati. La pausa della sequenza non rimette automaticamente il telefono in muto.
- **Prova tra 12 secondi** verifica una sveglia Panic Mode senza attivare la routine.
- Scegli **Puzzle della sequenza**: Matematica, Sequenza di colori, Trova l’intruso, Ordinamento oppure Tocca la sequenza. Risolvi e scegli **Spegni solo questa** per il richiamo corrente. **Prova il puzzle** offre anteprime semplici e difficili senza modificare le sveglie.
- **Ferma la mattina** richiede la versione difficile del puzzle scelto (per Matematica: moltiplicazione seguita da somma, per esempio `(17 × 4) + 26`). Il risultato corretto ferma immediatamente i richiami di questa sequenza; i giorni successivi rimangono attivi. Il pulsante è disponibile anche senza aver prima risolto l'addizione semplice.
- **Metti in pausa · risolvi** richiede la stessa prova anche a telefono sbloccato e disattiva la routine fino alla prossima attivazione manuale. Nessun timer o penalità: una risposta errata lascia le sveglie attive e mantiene lo stesso problema.
- Anche modificare orario, giorni o fasi di una sequenza già attiva richiede la prova, per impedire di aggirarla spostando o accorciando le sveglie. Quando la sequenza è in pausa, le modifiche sono libere.

Sequenza iniziale: 07:30, 07:40, 07:50; 08:00, 08:05, 08:10; ogni minuto dalle 08:15 alle 08:19; Panic Mode ogni minuto dalle 08:20 alle 08:34. Le fasi includono l'inizio ed escludono la fine, evitando duplicati. Nessun nuovo richiamo dopo le 08:35; una sveglia già in corso continua fino alla soluzione.

Il suono parte progressivamente nelle prime fasi; Panic Mode usa da subito l'intero volume **Sveglia** configurato sul telefono e un pattern di vibrazione più insistente. Il volume Sveglia resta quello configurato sul telefono. Dalla versione 1.2 viene invece riattivato il volume **Suoneria** all'attivazione della sequenza, per rendere udibili le chiamate consentite da Non disturbare.

## Samsung

Consenti notifiche e notifiche a schermo intero. Impostazioni → Applicazioni → Panik Attak → Batteria → **Senza restrizioni** è consigliato. Verifica che Non disturbare consenta le sveglie e che il volume Sveglia sia udibile. La pagina dell'app contiene collegamenti alle impostazioni.

Per uscire automaticamente dal muto serve anche l'accesso Android a **Non disturbare**. Senza accesso l'app mostra una spiegazione e il collegamento alle autorizzazioni; dopo la concessione occorre premere di nuovo Attiva. Panik Attak non chiama le API che cambiano il filtro, le regole o i contatti autorizzati: imposta un volume positivo per il flusso Suoneria con `FLAG_ALLOW_RINGER_MODES`, quindi verifica il filtro e la policy. Se Non disturbare blocca tutte le chiamate, chiede di consentire i preferiti nelle impostazioni. Il [permesso richiesto per i cambi di volume](https://developer.android.com/reference/android/media/AudioManager#setStreamVolume(int,int,int)) è documentato da Android.

Le sveglie vengono riprogrammate dopo riavvio, aggiornamento dell'app e cambio di fuso/orario. Lo spegnimento completo del telefono e **Arresto forzato** dalle impostazioni Android impediscono all'app di suonare: riaprila dopo un arresto forzato. Una normale uscita dall'app non annulla le sveglie.

## Progetto

Java 17, Android Gradle Plugin 8.7.2, Gradle 8.11.1, compile/target SDK 35, Android minimo 8.0. Material Components 1.12.0 e AndroidX per il selettore orario circolare, in tema Material 3 scuro. Le preferenze necessarie per ripristinare gli allarmi sono in memoria protetta dal dispositivo, disponibile prima dello sblocco dopo il riavvio.

Build: `./gradlew assembleDebug`. Impostare `sdk.dir` in `local.properties` o `ANDROID_HOME` verso il proprio SDK. APK: `app/build/outputs/apk/debug/app-debug.apk`.

Controlli: `./gradlew lintDebug`; per la logica temporale compilare `Plan.java` insieme a `tests/PlanTest.java` ed eseguire `it.panikattak.app.PlanTest`. Il test strumentale `AlarmSmoke` verifica la consegna reale dell'allarme e l'avvio del servizio; la sua conclusione termina il processo di test, quindi la verifica manuale del suono va eseguita dal pulsante dell'app.

Versione 1.1: `WakeChallengeTest` controlla i problemi generati. Il test sul dispositivo `GateSmoke` verifica risposte errate, annullamento, pausa globale, stop della sola mattina, prove riferite a una sequenza precedente e modifiche protette; ripristina le impostazioni originali. Compilazione del test: `./gradlew assembleDebugAndroidTest -PtestRunner=it.panikattak.app.GateSmoke` (su PowerShell racchiudere l'ultimo argomento tra apici).

Versione 1.2: `RingerSmoke` verifica sul dispositivo il passaggio dal muto a un volume Suoneria di almeno il 60% e confronta prima/dopo il filtro e l'intera policy Non disturbare. Sul Samsung SM-S938B Android 16: suoneria 9/15, filtro prioritario invariato e contatti preferiti conservati. Il test ripristina la modalità audio iniziale.

Il prodotto usa `AlarmManager.setAlarmClock()` per i singoli richiami, come descritto nella [documentazione Android sulle sveglie esatte](https://developer.android.com/develop/background-work/services/alarms). Solo il prossimo richiamo resta registrato; ogni esecuzione programma il successivo. Una notifica a schermo intero apre il puzzle selezionato sopra la schermata di blocco.


## Versione 2.0

- Orari con `MaterialTimePicker` in modalità quadrante 24 ore, selezione ore/minuti e pulsante tastiera numerica; conferma Fatto/Annulla. Usato per prima sveglia, inizio/fine fase e ora fissa.
- Fisso sostituisce tutti i richiami della fase con un solo evento: inizio incluso, fine esclusa. L’ora fuori finestra blocca il salvataggio con un messaggio rosso, anche se resa non valida modificando un’altra fase. Gli orari fissi restano orari locali e vengono ricalcolati dopo riavvio o cambio fuso. Quelli già trascorsi non vengono recuperati.
- Sequenza di colori: 4 elementi per 2 secondi; difficile 6 per 1,5 secondi. Tocca la sequenza: griglia 3×3, 4 o 6 passi. Intruso: griglia 4×4 con forma diversa, oppure 5×5 con orientamento diverso. Ordinamento: 3 numeri crescenti, oppure 5 emozioni dalla più felice alla più triste; trascinamento diretto e alternativa accessibile toccando due tessere.
- Gli errori mantengono la sfida, mostrano un bordo rosso e una breve vibrazione se consentita. Nessun limite di tempo. I puzzle di memoria possono essere rivisti. La sfida corrente è persistita, quindi un cambio di schermata o riavvio del processo non genera una risposta diversa. Non si ripete la stessa sfida consecutivamente per tipo e livello.
- **Difficoltà crescente**, disattivata inizialmente: semplice con 0–1 richiami ignorati, intermedia con 2–3, massima con almeno 4. Un richiamo è ignorato se il successivo arriva mentre quello precedente suona ancora. Contatore persistito e azzerato alla nuova sequenza, al nuovo giorno, alla pausa o allo stop della mattina. Le prove non fanno crescere la difficoltà. La sfida di pausa/stop/modifica resta sempre difficile.
- Preferenze v1 mantenute: in assenza delle nuove chiavi, Matematica, intervalli e difficoltà costante.

Test logici: compilare `Plan.java`, `WakeChallenge.java` e `Puzzle.java` insieme ai tre file in `tests/`, quindi eseguire `PlanTest`, `WakeChallengeTest`, `UpgradeTest` nel package `it.panikattak.app`.

Test Samsung: `GateSmoke` copre protezioni, cinque tipi di puzzle, anteprime, persistenza e Material dial. `UpgradeSmoke` prova input reali, memoria, trascinamento e consegna a schermo spento; ripristina le preferenze e ferma il suono di prova. Compilare con `-PtestRunner=it.panikattak.app.UpgradeSmoke` ed eseguire tramite Android instrumentation. Le schermate di verifica vengono salvate nella cartella esterna privata dell’app.

## Versione 2.1 — routine multiple

Ogni routine ha nome, giorni, orari, fasi, puzzle, difficoltà crescente e vibrazione propri. La selezione apre le sue impostazioni e non cambia quali routine sono attive. Le nuove routine e le copie partono in pausa. La configurazione precedente viene mantenuta come **La mia routine**.

Le routine possono essere attive contemporaneamente. Quando più richiami coincidono, ognuno mantiene il proprio puzzle: risolto il primo, compare il successivo. La schermata mostra il nome della routine e il numero di altri richiami in attesa. I richiami successivi della stessa routine aggiornano la sua sfida senza rimuovere quelle delle altre. La difficoltà crescente viene conteggiata separatamente.

Pausa, stop della mattina e modifiche protette riguardano solo la routine indicata. Eliminare una routine attiva richiede prima il puzzle di pausa; almeno una routine viene sempre conservata.

`RoutineScheduleTest` verifica la pianificazione combinata, inclusi orari coincidenti, giorni differenti, pause, stop isolati, Fisso, mezzanotte e fuso. Compilare anche `RoutineSchedule.java` insieme alle altre classi dei test logici. `RoutineSmoke` esegue sul dispositivo migrazione, gestione dei profili, isolamento delle protezioni e consegna reale di due richiami simultanei a schermo spento, poi ripristina le preferenze iniziali. Runner: `-PtestRunner=it.panikattak.app.RoutineSmoke`.

## Versione 2.2 — controlli e trascinamento

Il puzzle Ordinamento permette di trascinare subito una tessera: segue il dito, poi scambia posizione con la destinazione con una transizione di 180 ms. Anche lo scambio tramite due tocchi è animato. Annullare un gesto conserva l’ordine; la conferma attende la fine dello spostamento.

I dialoghi usano conferme lime con testo scuro e annullamento con bordo sottile, angoli di 12 dp e bersagli touch di almeno 48 dp. I menù hanno righe di almeno 52 dp con separatori sottili. Checkbox e radio riprendono l’accento lime; i pulsanti principali hanno angoli di 20 dp. Tipografia e superfici dei dialoghi restano quelle esistenti.

`ControlsSmoke` verifica sul Samsung i gesti touch reali, lo scambio, l’annullamento, l’alternativa a due tocchi, la resa effettiva dello sfondo dei pulsanti, i selettori e le dimensioni dei controlli. Runner: `-PtestRunner=it.panikattak.app.ControlsSmoke`.

## Versione 2.3 — countdown e documentazione

Conto alla rovescia hh:mm:ss del prossimo richiamo della routine selezionata, nella riga sopra Modifica. Aggiornamento ogni secondo mentre la schermata è visibile; routine in pausa: --:--:--. Le ore non ricominciano da zero dopo 24 ore. Il timer usa i giorni e l’eventuale mattina saltata della routine.

Build assembleDebug e lintDebug completati. Test CountdownSmoke compilato ma non eseguito sul dispositivo: Samsung scollegato al momento della verifica. Il test copre formato, durate oltre 24 ore, valori non negativi, aggiornamento dei secondi e pausa.

## Versione 3.0 Alpha 2 — notifiche delle sequenze

`SequenceStatus` conserva solo l’identità delle occorrenze effettivamente iniziate, il richiamo più recente e la fase. Non è uno storico statistico. La notifica 100 sul canale silenzioso `sequence_status` copre gli intervalli fra i richiami; la notifica 99 del servizio audio copre quelli ancora da risolvere. Le routine che suonano non vengono duplicate nel riepilogo silenzioso. Le altre routine restano visibili e indipendenti.

Aggiornamenti su consegna, soluzione, stop, pausa, rinomina, ripianificazione, riavvio e cambi dell’orologio. Nessun servizio sempre acceso o aggiornamento ogni secondo per il solo stato. Lo stato persistito viene riconciliato con coda, routine abilitate e prossimi eventi della stessa occorrenza; una notifica in attesa ha anche una scadenza di sicurezza. L’ultimo richiamo non risolto continua a suonare oltre la fine prevista.

Il tocco su una singola routine seleziona quella corrispondente, verificando che l’occorrenza esista ancora. Il riepilogo multiplo apre la schermata attuale delle routine. Nessuna azione della notifica interrompe direttamente un allarme. Il canale silenzioso offre una versione pubblica generica per lo schermo bloccato; le impostazioni Android di visibilità e autorizzazione restano determinanti.

Entrambe le notifiche richiedono `FLAG_ONGOING_EVENT` e `FLAG_NO_CLEAR`. Android 14+ permette comunque di rimuoverle individualmente a telefono sbloccato; non vengono usati stili chiamata/media fittizi o cicli di ripubblicazione. La rimozione della scheda non cambia coda, puzzle o pianificazione. Fonte: [Android — non-dismissible notifications](https://developer.android.com/about/versions/14/behavior-changes-all#non-dismissable-notifications).

`NotificationSmoke` verifica il ciclo di vita e la consegna reale. Se Samsung non espone il testo nella gerarchia accessibile, il parametro `-e swipeY <pixel>` permette un gesto sulla posizione prima verificata nella schermata `notification-before-swipe.png`; non usare coordinate non controllate. Il runner ripristina le preferenze, come `RoutineSmoke`. Risultati e limiti nel [rapporto Alpha 2](../TEST-REPORT-v3-notifications.md).

## Versione 3.0 Alpha 3 — infrastruttura delle pagine

`AppShell` mantiene barra superiore, contenuto scorrevole e navigazione Material con Oggi, Routine e Abitudini. `MainActivity` gestisce le destinazioni e il dettaglio della routine, conserva destinazione, ID, origine di Impostazioni e posizioni di scorrimento nel saved state. Il ripristino dello scroll attende il layout e non viene sovrascritto dal render successivo in `onResume`. Le schermate allarme e puzzle restano separate, senza barra inferiore.

`TodayPage` calcola il prossimo richiamo per ogni routine abilitata, ordina per orario e raggruppa gli ex aequo. Non cambia la routine selezionata. Mostra le occorrenze in corso da `SequenceStatus` e riceve gli aggiornamenti dell’allarme mentre è visibile. Il conto alla rovescia rimane limitato alla schermata visibile; Abitudini e Impostazioni non avviano un timer.

`RoutineUi` mostra lista, interruttori indipendenti, prossimi orari e gestione per riga. Il tocco apre il dettaglio della routine corretta, mantenendo i precedenti editor, controlli dei permessi, anteprime e protezioni. Una nuova routine o copia apre il proprio dettaglio e parte in pausa. Il nuovo contenitore non introduce un nuovo sistema di salvataggio delle bozze degli editor.

`PlaceholderPages` contiene Abitudini senza dati fittizi e la pagina Impostazioni con corpo vuoto, riservata alla fase 3. Le notifiche singole aprono il dettaglio verificando l’identità dell’occorrenza; il riepilogo apre Oggi. Il comportamento dello swipe non viene modificato in questa versione.

Test sul Samsung: 36 verifiche di navigazione (`NavigationSmoke`), 35 sulle routine e consegne simultanee (`RoutineSmoke`), 6 sul countdown nel dettaglio (`CountdownSmoke`). [Rapporto completo](../TEST-REPORT-v3-pages.md).
