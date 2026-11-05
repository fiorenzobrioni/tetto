# 🏠 TETTO - Product Vision Canvas (Expanded)

Questo documento descrive dettagliatamente l'identità, le meccaniche, l'interfaccia e la struttura tecnica dell'app "Tetto". Funge da linea guida (Single Source of Truth) per lo sviluppo, garantendo che ogni implementazione rispetti la filosofia del prodotto: un'esperienza Premium, priva di rumore visivo e senza attrito.

## 1. IL PROBLEMA (Pain Points)

* **Attrito da Data-Entry:** Le app finanziarie tradizionali richiedono troppo tempo (inserimento di causali, tag, scansione scontrini, selezione date).
* **Complessità Inutile:** Abbondanza di grafici complessi, conti multipli da sincronizzare e funzionalità che generano confusione.
* **Frustrazione e Abbandono ("What the hell effect"):** Sforare un budget in un mese porta i debiti al ciclo successivo. L'utente si sente in colpa, parte in svantaggio e finisce per abbandonare il tracciamento.
* **Mancanza di Flessibilità:** I budget sono spesso vincolati a un singolo arco temporale rigido (es. tutto il budget resettato l'1 del mese).

## 2. IL TARGET (La User Persona)

* Utenti che desiderano un approccio minimalista e diretto alla finanza personale (la versione digitale e istantanea del "metodo delle buste").
* Persone che vogliono sapere solo "quanto margine mi resta" per una specifica spesa, disinteressandosi dello storico analitico.
* Amanti del design pulito, premium, privo di pubblicità e che esigono il controllo totale sulla privacy (dati solo in locale, nessun login, nessun cloud obbligatorio).

## 3. LA SOLUZIONE (Value Proposition)

**Un'app di budgeting istantanea, empatica e priva di attrito.**
Tetto permette di impostare limiti di spesa ("Tetti") con archi temporali indipendenti per ogni categoria. L'inserimento richiede solo due tap. Il sistema matematico interno premia il risparmio mettendo da parte i fondi non spesi ("Riserva"), ammortizza gli imprevisti e, in caso di sforamento totale, "perdona" il debito resettandolo al nuovo ciclo temporale.

## 4. CORE MECHANICS & UX (L'Interazione)

* **La Regola dei 2 Tap (Flusso di inserimento):**
  1. *Tap 1:* L'utente tocca la categoria nella dashboard. Si apre il Custom Numpad (Bottom Sheet).
  2. *Tap 2:* L'utente digita l'importo e preme il tasto gigante "Aggiungi". Finito.

* **Focus sul Residuo:** Nessuna enfasi sul "quanto hai speso". La UI enfatizza in caratteri enormi la quota rimanente prima di infrangere il Tetto.

* **Live Preview Dinamica:** Durante la digitazione della spesa nel Numpad, un testo secondario mostra l'impatto della spesa in tempo reale. Se il budget residuo è 20€ e digito "5", il testo recita *"Ti restano 15€"*. Se digito "50", il testo diventa rosso e recita *"Vai sotto di 30€"*.

* **Prevenzione Errori senza Storico:** Non essendoci una schermata per la cronologia movimenti, la correzione degli errori è gestita via *Snackbar*. Subito dopo l'inserimento, appare un piccolo avviso per 3 secondi ("Aggiunti 50€ a Caffè") con un bottone **ANNULLA** per stornare immediatamente l'operazione in caso di svista.

## 5. IL MOTORE LOGICO (Rollover e Database)

Tutta l'app si basa su un'unica entità Room (`CategoriaBudget`) per garantire performance assolute a 120 fps costanti.

* **Tetti Indipendenti:** Ogni categoria possiede un suo ciclo vitale (giornaliero, settimanale, mensile, annuale). "Caffè" può resettarsi ogni lunedì, mentre "Bollette" ogni 1° del mese.

* **La "Magia" del Rollover (Il Sistema della Riserva):**
  Tutta la logica di fine periodo si risolve in una formula matematica pulita: `nuovaRiserva = max(0.0, riserva + tettoBase - spesaCorrente)`
  * *Il Premio (Risparmio):* Il residuo non speso a fine ciclo si somma alla Riserva.
  * *L'Ammortizzatore:* Gli sforamenti vanno a erodere prima di tutto la Riserva accumulata nei cicli virtuosi.
  * *Il Condono (Empatia):* Grazie alla funzione `max(0.0, ...)`, se il calcolo dà risultato negativo, la Riserva riparte da zero. Il debito non viene mai ereditato.

* **Lazy Evaluation (Zero Battery Drain):** Nessun *WorkManager* o processo in background. Al lancio dell'app, un Use Case verifica se `Timestamp Attuale > dataFinePeriodo` per le varie categorie. Se sì, applica la formula di rollover, azzera la spesa e ricalcola il nuovo fine periodo in una frazione di secondo. Affidabilità al 100%.

## 6. GESTIONE CATEGORIE E INTERFACCIA (UI Design)

Stile rigoroso, basato su Material Design 3 e Jetpack Compose.

* **Onboarding Guidato (Primo Avvio):**
  Per evitare la "sindrome del foglio bianco", l'app non parte vuota ma non impone nemmeno categorie pre-impostate difficili da cancellare.
  * Schermata *"A cosa vuoi dare un Tetto?"* con una griglia di macro-aree predefinite (Cibo, Auto, Shopping, Bollette, ecc.).
  * L'utente seleziona solo le icone di suo interesse. Successivamente, assegna rapidamente un limite di budget a ciascuna. L'app si avvia con una dashboard già ritagliata sulle sue esigenze.

* **Creazione Categorie Personalizzate (Post-Onboarding):**
  L'aggiunta di nuove categorie deve rimanere fedele al minimalismo:
  * Solo tre campi essenziali: **Nome**, **Importo**, **Periodo** (con menu a tendina minimale).
  * *La scelta dell'Icona:* Invece di offrire una dispersiva libreria di sistema con migliaia di icone, l'app presenta una palette curata e pre-selezionata di 20-30 Material Symbols ad alta leggibilità. Questo garantisce che la dashboard mantenga sempre un look uniforme ed elegante.

* **Dashboard (La Plancia di Controllo):**
  * Lista (`LazyColumn`) di Card (stile `OutlinedCard` o `ElevatedCard`).
  * *Header Card:* Icona a sinistra + Nome, a destra indicatore temporale discreto (es. *Rinnova tra 5 gg*).
  * *Dato Centrale:* Cifra gigante del residuo (`headlineLarge`) e sotto il totale ("su 200€ totali").
  * *Progress Bar:* Barra orizzontale che cambia colore semanticamente (Verde -> Arancio -> Rosso).
  * *Badge Riserva:* Se la Riserva > 0, un piccolo chip elegante ("+ 30€ di riserva") compare accanto alla cifra. Se a zero, scompare.

* **Custom Numpad (Bottom Sheet Nativo):**
  Niente tastiera di sistema. Un componente Compose diviso in tre fasce:
  1. *Fascia Superiore:* Cifra inserita lampeggiante e Live Preview del residuo.
  2. *Griglia Numerica:* Numeri da 0 a 9 ampi, ariosi, senza bordi. Implementazione del `LocalHapticFeedback` per una risposta tattile secca ("Tick") a ogni pressione.
  3. *Tasto di Conferma:* Bottone a tutta larghezza con testo dinamico ("Annulla" all'apertura, "Aggiungi X €" alla digitazione).

## 7. FUNZIONALITÀ EXTRA E IMPOSTAZIONI (Minimalismo e Comfort)

Il menu Impostazioni contiene solo quattro blocchi logici:

1. **Interfaccia e Comfort:**
   * Selettore Tema (Sistema/Chiaro/Scuro).
   * Interruttore per *Material You (Dynamic Colors)*.
   * Interruttore per attivare/disattivare il feedback aptico del Numpad.

2. **Localizzazione Perfetta:**
   * Utilizzo nativo del *Per-App Language Settings* di Android 13 per forzare la lingua IT/EN.
   * Formattazione Valuta (`NumberFormat.getCurrencyInstance`) con opzione per slegarla dal sistema e forzare €, $, £.

3. **L'Assistente Silenzioso (Promemoria Intelligente):**
   * Notifica push opzionale (es. alle 20:30) per ricordare l'inserimento.
   * *La chicca:* La notifica **non parte** se il sistema rileva che l'utente ha già registrato almeno una spesa durante la giornata in corso.

4. **I Tuoi Dati (Privacy & Backup):**
   * Testo di garanzia: *"I tuoi dati rimangono solo su questo dispositivo"*.
   * Tasto rapido per esportazione CSV (tramite `ACTION_CREATE_DOCUMENT`).
   * Bottone di "Hard Reset" per azzerare tutta l'app con dialog di sicurezza.

## 8. STACK TECNOLOGICO E VINCOLI

* **Linguaggio:** Kotlin
* **UI Toolkit:** Jetpack Compose, Material Design 3.
* **Persistenza:** Room Database.
* **Target OS:** MinSDK 33 (Android 13). Nessun compromesso per retrocompatibilità. Accesso nativo ad animazioni, colore dinamico e formattazione avanzata senza debito tecnico legato alle librerie `compat`.