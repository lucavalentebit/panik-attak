# Panik Attak 2.2 — verifica del 23 ottobre 2026

APK finale `Panik-Attak.apk`, versione 2.2 (versionCode 6), installata come aggiornamento sul Samsung SM-S938B Android 16. Copia precedente in `backups/Panik-Attak-v2.1.apk`.

SHA-256: `3819ec2a19def6e53d58ab66aa891b309af2495e15ed65feb2fb93add893f81f`

- Build e test Android compilati; lint: 0 errori, 17 avvisi non bloccanti.
- `ControlsSmoke`: 22 controlli superati con touch reale sul Samsung, inclusi trascinamento immediato, tessera che segue il dito, scambio al rilascio, conservazione delle altre posizioni, animazione, alternativa a due tocchi, gesto annullato e risposta errata.
- Controlli UI: riempimento lime effettivamente disegnato, testo scuro della conferma, radio e checkbox lime, altezza minima dei comandi e separatori dei menù. Verificati anche editor delle fasi e selettore orario.
- Schermate dei controlli e del trascinamento ispezionate. Corretto il caso dei pulsanti Material nei dialoghi di sistema che ignoravano il riempimento tramite stile.
- Preferenze originali del telefono confrontate dopo i test: nessuna modificata, esclusa la riprogrammazione del prossimo allarme. Pacchetto dei test disinstallato.

Ambito: questa verifica copre i controlli modificati sul Samsung collegato; pianificazione e allarmi multipli non modificati. I precedenti rapporti documentano i relativi test.
