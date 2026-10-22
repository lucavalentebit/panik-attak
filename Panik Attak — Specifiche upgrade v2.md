# Panik Attak — Specifiche upgrade v2

## Panoramica

## Questo documento specifica tre upgrade da sviluppare prima della pubblicazione su Play Store: nuovi tipi di puzzle cognitivi alternativi alla matematica, un time picker circolare per l'impostazione degli orari, e la modalità Fisso per gli slot. L'obiettivo è rendere l'app più interessante e moderna senza aumentare la complessità percepita dall'utente.

## Nuovi puzzle cognitivi

L'utente potrà scegliere il tipo di puzzle nella configurazione della sequenza. Il tipo selezionato si applica uniformemente a tutti i richiami di quella sequenza — sia per "spegni questo richiamo" (sfida semplice) sia per "ferma tutto / metti in pausa" (sfida difficile). La difficoltà delle due sfide si scala automaticamente in base al tipo scelto.

I tipi disponibili sono:

**Matematica** *(esistente, invariata)* Sfida semplice: addizione. Sfida difficile: moltiplicazione + addizione.

**Sequenza di colori** Viene mostrata una sequenza di 4–6 cerchi colorati per 2–3 secondi, poi nascosta. L'utente riproduce la sequenza toccando i colori nella stessa posizione. Sfida semplice: 4 colori, 2 s di visualizzazione. Sfida difficile: 6 colori, 1,5 s. I colori usati sono sempre nettamente distinti (rosso, blu, giallo, verde, viola, arancione) — nessuna ambiguità cromatica.

**Trova l'intruso** Viene mostrata una griglia di simboli, lettere o forme in cui tutti gli elementi sono identici tranne uno. L'utente tocca l'elemento diverso. Sfida semplice: griglia 4×4, differenza evidente (forma diversa). Sfida difficile: griglia 5×5, differenza sottile (orientamento o colore leggermente diverso). Nessun testo da leggere — funziona a qualsiasi ora anche con gli occhi semichiusi.

**Ordinamento** Vengono mostrate 3–5 tessere con numeri, lettere o immagini (es. facce emoji che esprimono emozioni in scala). L'utente le trascina nell'ordine corretto (crescente/decrescente, alfabetico, o dalla più felice alla più triste). Sfida semplice: 3 tessere con numeri. Sfida difficile: 5 tessere con concetti astratti. Richiede coordinazione motoria fine — difficile da fare in dormiveglia.

**Tocca la sequenza** Appaiono 3×3 caselle che si illuminano in sequenza una alla volta. L'utente deve poi toccarle nello stesso ordine (come Simon Says ma senza suoni). Sfida semplice: 4 passi. Sfida difficile: 6 passi. Questo tipo è particolarmente insidioso perché la memoria a breve termine è l'ultima a tornare al mattino.

---

### Regole comuni a tutti i tipi

- Nessun timer: l'utente ha tutto il tempo che vuole, ma il richiamo continua a suonare finché non risolve.
- Errori: ogni risposta sbagliata mostra un feedback visivo (bordo rosso, leggera vibrazione) e ripropone immediatamente la stessa sfida senza penalità.
- Generazione: ogni sfida è generata casualmente a ogni richiamo — mai la stessa due volte di fila.
- Accessibilità: tutti i tipi usano elementi visivi grandi, toccabili facilmente con il dito, senza richiedere testo scritto né lettura (tranne Ordinamento nella variante lettere, che è opzionale).

## 

## Time picker circolare

Il selettore dell'orario attuale (presumibilmente un NumberPicker o un TimePicker standard Android) viene sostituito con un orologio circolare analogico interattivo, nello stile del Material Design 3 Time Picker in modalità "dial". È lo stesso componente che usa l'app Clock di Google su Android 12+, quindi familiare alla maggior parte degli utenti Android.

### Comportamento

**Interazione primaria:** l'utente tocca o trascina la lancetta dell'orologio per selezionare ore e minuti. Il cerchio esterno mostra le 12 ore; dopo aver selezionato l'ora, la vista passa automaticamente ai minuti (cerchio con tacche a 5-in-5 minuti, tocco preciso per minuti intermedi).

**Input testuale di fallback:** un tocco sull'orario visualizzato al centro del cerchio (es. "07:30") passa alla modalità tastiera numerica — utile quando si vuole inserire un orario esatto senza trascinare. Stesso comportamento del Material Time Picker nativo.

**Conferma:** pulsante «Fatto» / «Annulla» sotto il cerchio, coerente con il design esistente dell'app.

### Applicazione nella UI attuale

Il picker circolare appare ovunque nell'app venga impostato un orario:

- Orario di inizio della sequenza (slot «07:30» nella schermata principale)
- Orario di inizio e fine di ogni fase (es. «07:30 – 08:00» nella fase «Con calma»)

L'aspetto visivo (colori, font, bordi arrotondati) segue il tema dark esistente dell'app: sfondo scuro, accento verde acido per la selezione attiva, testo bianco.

### Note implementative

## Android non espone il dial nativo come componente standalone facilmente personalizzabile. La soluzione raccomandata è usare la libreria **Material Components for Android** (`com.google.android.material:material`) che include `MaterialTimePicker` con `inputMode = INPUT_MODE_CLOCK` — supporta il dial circolare, il fallback tastiera, e si integra con il tema Material dell'app senza dipendenze aggiuntive. Compatibile da Android 5.0 (API 21) in su, nessun problema di copertura su Android 8.0+.

## Modalità Fisso per gli slot

Ogni fase ha oggi un intervallo tra i richiami configurabile (es. ogni 10', ogni 5', ogni 1'). La modalità Fisso è un'opzione alternativa che permette di impostare un orario preciso a cui scatta un singolo richiamo fisso all'interno della fase, indipendentemente dall'intervallo.

### Casi d'uso

L'utente con riunione alle 9:00 vuole un richiamo tassativo alle 8:45 — non "ogni 5 minuti a partire dalle 8:30", ma "esattamente alle 8:45". La modalità Fisso soddisfa questo senza aggiungere una fase dedicata.

### Comportamento

Ogni fase ha un nuovo toggle «Fisso» accanto all'impostazione dell'intervallo. Quando attivato:

- Il campo «ogni X minuti» viene sostituito da un campo «alle HH:MM» (con il time picker circolare).
- In quella fase scatta esattamente un richiamo all'orario specificato, se rientra nella finestra temporale della fase.
- Se l'orario fisso è fuori dalla finestra della fase, l'app mostra un avviso in rosso e non consente di salvare finché non viene corretto.
- Il richiamo fisso si comporta identicamente agli altri: richiede la soluzione del puzzle per essere spento; il Panic Mode si attiva regolarmente se la fase lo prevede.

### UI nella schermata di configurazione fase

```
┌─────────────────────────────────────────┐
│  02  Ci siamo              ogni 5'  ↗   │
│      08:00 – 08:15 · 15 min             │
│                                         │
│  Modalità  ○ Intervallo  ● Fisso        │
│            alle  [08:10]                │
└─────────────────────────────────────────┘
```

Il radio button «Fisso» sostituisce il selettore dell'intervallo con il campo orario. Il cambio è istantaneo, senza scroll o schermate aggiuntive.

### Interazione con il Panic Mode

## Il Panic Mode non supporta la modalità Fisso — è per definizione una fase ripetitiva e insistente. Il toggle «Fisso» è disabilitato nelle fasi Panic Mode, con nota: «Il Panic Mode richiede un intervallo.»

## Difficoltà crescente dei puzzle (opzionale)

Questa feature è disattivabile nelle impostazioni — non è il comportamento di default. Se attiva, la difficoltà della sfida semplice (spegni il richiamo corrente) aumenta con ogni richiamo ignorato nella stessa mattina.

| Richiami ignorati | Sfida semplice |
| --- | --- |
| 0–1 | Addizione o equivalente nel tipo scelto |
| 2–3 | Difficoltà media (sequenza più lunga, griglia più grande) |
| 4+ | Stessa difficoltà della sfida «ferma tutto» |

La sfida «ferma tutto / metti in pausa» mantiene sempre la sua difficoltà massima, indipendentemente dal contatore.

Il contatore si azzera a sequenza completata o al giorno successivo. Non viene mostrato esplicitamente all'utente durante la sveglia — non è una punizione dichiarata, è una pressione silenziosa.

## **Perché opzionale:** alcuni utenti trovano frustrante la difficoltà crescente al mattino; deve essere una scelta consapevole, non il comportamento di default.à crescente

## Note implementative e priorità

Ordine di sviluppo consigliato, dal più semplice al più complesso:

1. **Time picker circolare** — una riga di configurazione Gradle più sostituzione del componente esistente. Impatto visivo immediato, zero rischio funzionale. Iniziare da qui.
2. **Modalità Fisso** — aggiunge logica di scheduling puntuale su infrastruttura già esistente. Attenzione ai casi edge: orario fisso nel passato (sequenza già avviata), cambio fuso orario, riavvio del dispositivo.
3. **Nuovi tipi di puzzle** — il lavoro più corposo. Sviluppare un tipo alla volta con test su dispositivi fisici. Ordine suggerito: Sequenza di colori → Trova l'intruso → Tocca la sequenza → Ordinamento. Ogni tipo va testato in condizioni di sonnolenza reale usando l'anteprima esistente.
4. **Difficoltà crescente** — da implementare solo dopo che i tipi di puzzle sono stabili. Aggiunge un flag in SharedPreferences e una logica di contatore in memoria; non tocca il core dell'allarme.

### Considerazioni trasversali

- **Internazionalizzazione:** tutti i nuovi testi UI vanno in `strings.xml` dall'inizio, anche se la v1.0 è solo italiana. Aggiungere lingue sarà banale in seguito.
- **Accessibilità:** i puzzle visivi devono avere touch target di almeno 48×48 dp — fondamentale per un'app usata con gli occhi semichiusi.
- **Test su dispositivi reali:** l'emulatore non è sufficiente per le sveglie in background. Testare su almeno un Samsung e un dispositivo stock Android.

## 
