# Panik Attak 3.0 Alpha 6.2 — trova la posata

In **Impostazioni → Prove sperimentali → Prova le posate**, l’app estrae una richiesta casuale: **fotografa una forchetta, un cucchiaio oppure un coltello**. La posata richiesta viene mostrata sopra l’anteprima.

La prova viene superata solo se la posata richiesta ha il punteggio più alto fra le tre categorie supportate, almeno al 50%, senza parità. Una posata diversa, un punteggio insufficiente o un risultato a pari merito richiedono un nuovo tentativo. L’app mostra le rilevazioni delle sole tre posate per aiutare a correggere l’inquadratura.

- **Riprova la foto** mantiene la stessa richiesta.
- **Prossima posata**, dopo il successo, estrae casualmente una delle altre due.
- Ruotare il telefono o tornare dal background conserva la richiesta.
- Tazze, sedie e tutte le altre categorie sono escluse dalla prova e dai risultati.

Anteprima e luce automatica della Alpha 6.1 restano disponibili, con il pulsante per spegnere la luce se crea riflessi. L’utente ha confermato il funzionamento della versione precedente con le posate; questo riscontro non è una misura dell’accuratezza del modello.

Il modello locale è invariato: le categorie estranee vengono ignorate dall’app. Le percentuali restano punteggi del modello, non probabilità calibrate. Le foto rimangono in memoria sul telefono; questa prova è indipendente dalle sveglie e non le sblocca.

Versione `3.0-alpha.6.2`, codice Android 14, firma debug compatibile con Alpha 6 e Alpha 6.1.

Verifiche: build e lint Android, compilazione dei test strumentali e 73 controlli delle regole della prova su risultati simulati, inclusi riconoscimento della posata sbagliata, categorie escluse, soglie, parità e selezione casuale. Sono verifiche del codice; non è stata eseguita una nuova valutazione fotografica del modello. [Rapporto completo](https://github.com/lucavalentebit/panik-attak/blob/v3.0-alpha.6.2/TEST-REPORT-v3-cutlery.md).

Allegati: `Panik-Attak.apk` e `Panik-Attak.apk.sha256`.
