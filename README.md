# centrale-reports-open

## Configurazione Connessione DB per Report

Le configurazioni delle connessione JDBC sono gestite in web.xml.
Esiste una configurazione predefinita che può essere utilizzata come Connessione per tutti i report.
E' possibile speficiare configurazioni aggiuntive specifiche ed utilizzare il parmatro **conn=X** per specificare quale configurazione usare per il report che ne richiede una di specifica.

## Configurazione PDF differenziati per stesso Report

Per ogni report è possibile specificare un parametro **tipo=TIPOESEMPIO** in modo che per lo stesso report vengano generati più PDF ed allegati alla stessa email.
Utile se per lo stesso report ad esempio si vuole generare un PDF per ogni tipo di Prodotto (nota: il tipo poi deve essere gestito internamente nel report per applicare il "filtro dati" corretto)

## Configurazione PROFILI

E' possibile configurare dei profili richiamabili con il parametro **profilo=NOMEPROFILO** in modo che la configurazione venga caricata da database (vedi configurazione JPA) ed non attraverso i parametri query string della servlet

## Esempio Configurazione CRON

	wget -t 1 -q "http://server:8080/centralereport/Genera?report=test&tipo=GRUPPO1&to=email@dominio.it"
