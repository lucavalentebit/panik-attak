# Prova delle posate

Da **Impostazioni → Prove sperimentali → Prova le posate**, l’app estrae a caso una delle tre richieste: **fotografa una forchetta**, **fotografa un cucchiaio** o **fotografa un coltello**. La richiesta appare sopra l’anteprima. Inquadrare una sola posata, intera e ben visibile.

La luce continua parte accesa dove disponibile, con ripiego sul flash allo scatto. Il pulsante Luce permette di spegnerla per ridurre i riflessi. La luce si spegne dopo lo scatto e quando si lascia la schermata. La prova nelle Impostazioni è indipendente dalle sveglie; la foto resta in memoria e non viene caricata su un server o scritta nello storico.

## Sfida della sveglia — Alpha 7

Da **Routine → dettaglio della routine → Puzzle della sequenza → Trova la posata**, la stessa fotocamera diventa una sfida disponibile per i richiami, Ferma la mattina, pausa e modifica protetta. **Prova il puzzle** permette di provarla senza modificare le sveglie.

- La categoria richiesta è parte della sfida salvata: non cambia dopo un errore, rotazione, riapertura della fotocamera o ricreazione dell’attività. Un nuovo richiamo genera una nuova sfida.
- **Apri fotocamera** mostra la richiesta e avvia la stessa acquisizione e analisi locale delle Impostazioni. Solo un riconoscimento positivo restituisce una soluzione alla schermata chiamante.
- Per un richiamo, il successo abilita **Spegni solo questa**. Il suono continua fino alla conferma; le altre routine restano indipendenti. Nelle azioni protette, il successo applica l’azione richiesta come per gli altri puzzle.
- Indietro, permesso negato, foto sbagliata o errore della fotocamera non risolvono la sfida. **Alternativa · Matematica difficile** richiede sempre una moltiplicazione seguita da somma, anche per un richiamo semplice; evita di dipendere dalla disponibilità della fotocamera o dalla precisione del modello.
- Tutti i livelli fotografici richiedono una sola posata. I livelli non aumentano il numero di foto o le soglie del modello. Le protezioni mantengono il livello massimo e l’alternativa matematica difficile.
- Risultati fotografici riferiti a un puzzle precedente vengono scartati. Anche la conferma finale ricontrolla routine, richiamo e sfida correnti.

Le foto non sono persistite. Lo storico delle sequenze reali conserva il tipo di sfida e gli eventi di soluzione/conferma, secondo le impostazioni esistenti; non conserva foto, risposte o punteggi del modello. Le anteprime e le prove restano escluse. [Verifiche e limiti Alpha 7](../TEST-REPORT-v3-object-challenge.md).

## Regole

- Sono supportati soltanto **forchetta**, **cucchiaio** e **coltello**. Tutte le altre classi del modello sono ignorate, anche nei dettagli delle rilevazioni.
- Per superare la prova, la posata richiesta deve avere il punteggio più alto fra le tre categorie supportate, almeno al **50%**, senza parità con un’altra categoria. Viene usata la rilevazione più forte per ciascuna posata.
- Con una posata diversa, punteggio insufficiente o parità, **Riprova la foto** mantiene la stessa richiesta. Le rilevazioni delle posate dal **25%** in su aiutano a capire l’esito.
- Dopo il successo, **Prossima posata** estrae con uguale probabilità una delle altre due categorie. La prima estrazione dà uguale probabilità a tutte e tre.
- Ruotare il telefono o passare in background conserva la richiesta. La foto e il risultato non vengono salvati attraverso la ricreazione dell’attività.

Un cucchiaio al 44% resta incerto. Le percentuali sono punteggi del modello e non probabilità calibrate di correttezza. Una foto con più posate può produrre un rifiuto se la categoria richiesta non ha il punteggio più alto: per questo la schermata richiede una sola posata.

## Modello

Il modello è `ssd_mobilenet_v2_coco_quant_postprocess.tflite` dal repository pubblico [google-coral/test_data](https://github.com/google-coral/test_data), licenza Apache 2.0 inclusa in `app/src/main/assets/OBJECT_MODEL_LICENSE.txt`. SHA-256: `42fb3d70ffb7bb37dd518f730f7be784b831c2078f30c497d0019cc2e987fa26`. È quantizzato, pesa 6,2 MB e gira localmente sulla CPU con TensorFlow Lite, con input RGB uint8 300×300.

Gli indici COCO usati dall’app sono **47=forchetta, 49=cucchiaio, 48=coltello**. Il file del modello resta quello della Alpha 6; l’app ne limita i risultati alle tre posate, senza riaddestramento. Le soglie restano iniziali e non calibrate su un dataset del prodotto. Il modello può mancare oggetti piccoli, coperti, riflettenti o poco illuminati e può produrre falsi positivi.

L’utente ha confermato il funzionamento della Alpha 6.1 con le posate. La nuova selezione casuale e le sue regole sono coperte da test su output simulati; questo non equivale a una nuova valutazione fotografica. Verifiche e limiti dell’aggiornamento: [rapporto Alpha 6.2](../TEST-REPORT-v3-cutlery.md).
