# Prova di riconoscimento oggetti

Da **Impostazioni → Prove sperimentali → Prova la fotocamera**, l'app scatta una foto nella propria schermata e cerca posate (forchetta, coltello, cucchiaio), tazze e sedie. La prova è indipendente dalle sveglie e non sblocca puzzle. La foto resta in memoria durante l'analisi e non viene caricata su un server o scritta nello storico.

Il modello è `ssd_mobilenet_v2_coco_quant_postprocess.tflite` dal repository pubblico [google-coral/test_data](https://github.com/google-coral/test_data), licenza Apache 2.0 inclusa in `app/src/main/assets/OBJECT_MODEL_LICENSE.txt`. SHA-256: `42fb3d70ffb7bb37dd518f730f7be784b831c2078f30c497d0019cc2e987fa26`. È quantizzato, pesa 6,2 MB e gira localmente sulla CPU con TensorFlow Lite. Nell'elenco COCO associato le classi sono 46=tazza, 47=forchetta, 48=coltello, 49=cucchiaio, 61=sedia. L'interfaccia accetta una rilevazione da 50% in su e mostra anche le altre rilevazioni da 25% in su. Queste soglie sono iniziali, non calibrate su foto del prodotto.

Il modello riconosce la classe *sedia* ma non conta le gambe: **non può confermare che ne abbia quattro**. Occorrono foto reali su telefoni diversi, in cucine diverse e con sedie a basi differenti prima di usarlo come sfida per fermare la sveglia. Il modello generale può mancare oggetti piccoli, coperti o poco illuminati e può produrre falsi positivi.

Verifica tecnica in cloud: build e lint Android; modello invocato su CPU host con input RGB uint8 300×300, quattro output del tipo atteso e 20 rilevazioni massime. Su una foto di prova di un gatto, la classe `cat` (indice 16) è risultata al 95,7%, coerente con la lista delle classi. Fotocamera, resa fotografica e accuratezza sulle tre categorie richiedono prove su un telefono reale.
