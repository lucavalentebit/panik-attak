# Riconoscere forchetta, cucchiaio, coltello e cialda di caffè

## Cosa cambia subito

- Anteprima in `TextureView`, senza lo sfondo opaco che poteva coprire il video. Mantiene le proporzioni e abilita lo scatto dopo l’arrivo di un fotogramma.
- Fotocamera posteriore, luce continua accesa per impostazione iniziale, oppure flash allo scatto se il telefono supporta solo quello. Il pulsante permette di spegnere la luce quando produce riflessi sul metallo. La fotocamera viene rilasciata e la luce spenta alla ricezione della foto, uscendo dalla pagina o passando in background.
- Messa a fuoco prima dello scatto, JPEG di qualità 95, risoluzione scelta fra quelle disponibili intorno a 1600×1200. Decodifica e inferenza in background; nuovo scatto riapre l’anteprima e pulisce il risultato precedente.
- Risultati distinti **Forchetta / Cucchiaio / Coltello**, invece del risultato aggregato “Posate”. Tazze e sedie restano disponibili nell’esperimento esistente.

Il modello e le soglie restano quelli originali. Le cialde **non sono ancora riconoscibili**: non esiste una classe corrispondente nel modello COCO incluso. Il 44% dello screenshot è un punteggio del modello, non una probabilità calibrata. Abbassare la soglia dal 50% al 40% cambierebbe il risultato visualizzato, senza dimostrare un miglioramento di accuratezza.

## 1. Definire le quattro classi e raccogliere esempi reali

Le classi del prossimo modello saranno `fork`, `spoon`, `knife`, `coffee_pod`. Prima di etichettare l’ultima, decidere se comprende cialde in carta ESE, capsule rigide o entrambe: sono oggetti visivamente diversi. Se la classe comprende solo le cialde ESE, le capsule devono comparire anche fra i negativi. Definire inoltre se riconoscere l’oggetto sfuso, nella confezione o entrambi.

Come punto di partenza, raccogliere **500–1.000 foto diverse per classe**, più **500–1.000 foto negative** senza oggetti richiesti. Questi numeri sono una stima iniziale: aumentare i dati dove le curve di apprendimento e gli errori lo richiedono. Fotografare molti esemplari fisici differenti, non soltanto molte pose della stessa posata.

Includere telefoni e cucine diversi, sfondi chiari/scuri, distanze e inclinazioni differenti, luce diurna e artificiale, luce continua accesa e spenta. Per le posate includere riflessi, acciaio/plastica/legno, cucchiaini, cucchiai e coltelli da tavola; raccogliere anche oggetti parzialmente coperti e foto difficili per misurare quando rifiutare una risposta. Fra i negativi: penne, bacchette, palette, monete, tappi, piattini, confezioni e piani di lavoro vuoti.

Annotare con rettangoli **tutti** gli oggetti delle quattro classi presenti in ogni foto. Le foto negative hanno zero rettangoli. Raccogliere dati separatamente dall’app distribuita: l’esperimento continua a non salvare o caricare le fotografie.

## 2. Addestrare e confrontare un modello dedicato

Partire da un detector leggero preaddestrato compatibile con TensorFlow Lite, per esempio **EfficientDet-Lite0**, e adattarlo alle quattro classi. Confrontare input 320×320 e una variante a risoluzione maggiore: forchette e manici sottili perdono dettaglio nell’attuale SSD MobileNet a 300×300. Se l’uso viene limitato rigorosamente a un solo oggetto centrato, valutare anche un classificatore MobileNetV3 con classe “altro”; il detector è la scelta iniziale per sfondi e scene con più oggetti.

Dividere indicativamente i dati in **70% addestramento / 15% validazione / 15% test**, separando esemplari fisici, sessioni fotografiche e ambienti. Foto quasi uguali dello stesso scatto o della stessa raffica non devono finire in gruppi diversi. Tenere nel test anche telefoni e ambienti non usati nell’addestramento.

Usare variazioni moderate di luminosità, contrasto, rotazione, scala e occlusione; non tagliare sistematicamente denti della forchetta o testa del cucchiaio. Conservare le proporzioni con il ridimensionamento previsto dal modello, per esempio padding/letterbox: l’attuale pipeline deforma la foto in un quadrato. Addestramento, valutazione ed esecuzione sul telefono devono usare lo stesso preprocessing.

## 3. Misurare precisione e rifiuto, poi esportare

Confrontare il modello attuale e quello nuovo sullo stesso test, separando luce accesa e spenta. Misurare **precisione e richiamo per ciascuna classe**, matrice di confusione, falsi riconoscimenti sui negativi e tempo di inferenza. Analizzare in particolare forchetta↔cucchiaio, coltello↔penna e cialda↔tappo/capsula. Aggiungere nuove foto degli errori al solo training e ripetere la validazione senza riutilizzare il test per scegliere le soglie.

Obiettivi iniziali proposti, **non risultati raggiunti**: precisione ≥95% e richiamo ≥90% per ognuna delle quattro classi sul test rappresentativo; falsi riconoscimenti sui negativi ≤1%; inferenza p95 inferiore a 1 secondo sul telefono meno potente supportato. Riportare numerosità e intervalli di confidenza: pochi esempi non giustificano questi numeri.

Scegliere soglie distinte per classe sul set di validazione. Quando il punteggio è insufficiente o più classi competono sulla stessa regione, mostrare “Non riconosciuto, riprova”. Un detector non può garantire riconoscimenti esatti in qualsiasi condizione; la possibilità di rifiutare evita conferme arbitrarie. Un futuro sblocco della sveglia richiede una valutazione specifica più severa dei falsi positivi e un metodo alternativo per completare la sfida.

Esportare prima in TFLite float, poi confrontare la versione **INT8** calibrata su immagini rappresentative. Verificare anche il modello esportato sul test: la quantizzazione può ridurre il riconoscimento dei piccoli dettagli. Conservare modello, etichette nell’ordine esatto, licenza, checksum, versione, preprocessing, soglie e rapporto delle metriche. Non riutilizzare gli indici COCO per il nuovo modello.

## 4. Integrare e provare sul telefono

Aggiornare `ObjectDetector` per il contratto effettivo del modello: dimensioni e tipo dell’input, normalizzazione/quantizzazione, ordine RGB, nomi e dimensioni degli output, rettangoli, classi e soppressione dei duplicati. Non basta sostituire il file `.tflite`. Aggiungere test con foto di riferimento versionate e verificare la corrispondenza fra output Python e Android.

In una seconda iterazione, usare l’area rilevata per aiutare l’utente a inquadrare meglio; segnalare mosso, poca luce e oggetto troppo piccolo con soglie misurate. Se si analizzano più fotogrammi, misurare sul dispositivo la stabilità della decisione e i consumi: fotogrammi consecutivi correlati non sono prove indipendenti.

Provare su Samsung reale e almeno un telefono di fascia inferiore: apertura, permesso negato/concesso, anteprima visibile, luce e riflessi, rotazione, ritorno dal background, scatti ripetuti, interruzione durante autofocus/analisi e assenza di flash. La fotocamera virtuale può verificare il flusso dell’app, ma non la qualità ottica, la luminosità del LED o l’accuratezza sulle quattro classi.
