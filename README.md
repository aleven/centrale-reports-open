# centrale-reports-open

## Configurazione EMAIL

Le configurazioni Email sono gestite in web.xml.

## Configurazione Connessione DB per Report

Le configurazioni delle connessione JDBC sono gestite in web.xml.
Esiste una configurazione predefinita che può essere utilizzata come Connessione per tutti i report.
E' possibile speficiare configurazioni aggiuntive specifiche ed utilizzare il parmatro **conn=X** per specificare quale configurazione usare per il report che ne richiede una di specifica.

## Configurazione PDF multipli per stesso Report

Per ogni report è possibile specificare un parametro **tipo=TIPOESEMPIO** in modo che per lo stesso report vengano generati più PDF ed allegati alla stessa email.
Utile se per lo stesso report ad esempio si vuole generare un PDF per ogni tipo di Prodotto (nota: il tipo poi deve essere gestito internamente nel report per applicare il "filtro dati" corretto)

## Configurazione PROFILI

E' possibile configurare dei profili richiamabili con il parametro **profilo=NOMEPROFILO** in modo che la configurazione venga caricata da database (vedi configurazione JPA) ed non attraverso i parametri query string della servlet

## Esempio Configurazione CRON / URL

Generazione PDF ed invia via Email

	wget -t 1 -q "http://server:8080/centralereport/Genera?report=test&to=email@dominio.it"

Anteprima PDF

	wget -t 1 -q "http://server:8080/centralereport/Genera?report=test"
	
Generazione PDF del Report differenziati per TIPO1 e TIPO2 ed invia via email

	wget -t 1 -q "http://server:8080/centralereport/Genera?report=test&tipo=TIPO1,TIPO2&to=email@dominio.it"
	
Utilizza la configurazione da uno dei Profili pre-configurati su database

	wget -t 1 -q "http://server:8080/centralereport/Genera?profilo=NOMEPROFILO"
	