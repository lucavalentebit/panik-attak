# Panik Attak 3.0 Alpha 7 — Sfida fotografica

- **Trova la posata** è selezionabile tra i puzzle di ogni routine. Fotografa la forchetta, il cucchiaio o il coltello richiesto per risolvere la sfida e abilitare **Spegni solo questa**.
- Disponibile anche per Ferma la mattina, pausa, modifica protetta e anteprime.
- Richiesta conservata tra tentativi e ricreazione della schermata; risultati di vecchie sfide scartati.
- **Alternativa · Matematica difficile** permette di completare la sfida senza fotocamera, risolvendo comunque un’operazione in due passaggi.
- Analisi delle foto sul telefono, senza salvataggio o invio. La prova autonoma nelle Impostazioni resta disponibile.

Modello e regole di riconoscimento invariati dalla Alpha 6.2. Tutti i livelli fotografici richiedono una sola posata. Build e lint verificati; test sul dispositivo da eseguire con un telefono collegato. [Rapporto delle verifiche](TEST-REPORT-v3-object-challenge.md).

## Distribuzione

Versione Android `3.0-alpha.7`, codice `15`. L’APK Alpha 6.2 distribuito usa il certificato SHA-256 `12b4d56e7d15448031ffda1c109af7763503f6d9b297ad1eb7686c82bc3340e0`; il keystore debug locale usa un certificato diverso. Per pubblicare un aggiornamento installabile senza disinstallare o perdere dati occorre recuperare il keystore della Alpha 6.2. L’APK nella radice resta quello della Alpha 6.2 finché questo requisito non viene risolto.
