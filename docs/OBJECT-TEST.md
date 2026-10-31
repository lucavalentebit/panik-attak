# Prova delle posate

Da **Impostazioni → Prove sperimentali → Prova le posate**, l’app estrae a caso una delle tre richieste: **fotografa una forchetta**, **fotografa un cucchiaio** o **fotografa un coltello**. La richiesta appare sopra l’anteprima. Inquadrare una sola posata, intera e ben visibile.

La luce continua parte accesa dove disponibile, con ripiego sul flash allo scatto. Il pulsante Luce permette di spegnerla per ridurre i riflessi. La luce si spegne dopo lo scatto e quando si lascia la schermata. La prova è indipendente dalle sveglie e non sblocca puzzle; la foto resta in memoria e non viene caricata su un server o scritta nello storico.

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
