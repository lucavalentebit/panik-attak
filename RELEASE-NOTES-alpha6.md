# Panik Attak 3.0 Alpha 6 — prova riconoscimento oggetti

Questa versione aggiunge una pagina sperimentale in **Impostazioni → Prove sperimentali → Prova la fotocamera**. Si scatta una foto all'interno dell'app e un modello locale cerca tazze, posate (forchette, coltelli e cucchiai) e sedie. La pagina mostra risultato e confidenza; non cambia routine, allarmi o puzzle. Le foto vengono analizzate in memoria e non caricate online.

Il modello COCO quantizzato pesa 6,2 MB. L'APK universale include librerie native per più architetture, quindi è più grande delle Alpha precedenti. Questa build è firmata con la chiave di debug, come le versioni precedenti del progetto.

**Limiti della prova:** il modello può classificare una sedia ma non verificare che abbia quattro gambe. La soglia di riconoscimento iniziale è il 50% e non è stata calibrata su foto reali di cucine. Oggetti piccoli, parzialmente coperti o con poca luce possono essere mancati; sono possibili falsi positivi. Non usare questa funzione come prova per fermare una sveglia.

Verifiche cloud: build, lint e compilazione dei test Android completati; 38.137 controlli dei cinque test logici esistenti superati. Il modello è stato invocato su CPU con un'immagine campione per verificare input, output e indici delle classi. La fotocamera e il riconoscimento dei tre oggetti non sono ancora stati provati su un telefono reale.

APK: `Panik-Attak.apk` nella repository e allegato a questa release. SHA-256 in `Panik-Attak.apk.sha256`.
