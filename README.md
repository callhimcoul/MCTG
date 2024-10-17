## Überblick
Dieses Projekt implementiert einen einfachen Server, der Clients empfängt und HTTP-Anfragen verarbeitet, ohne ein externes HTTP-Framework zu verwenden. Die Anwendung unterstützt Benutzerregistrierung, Login, Paketverwaltung und Kartenverwaltung mit token-basierter Sicherheit.

## Architektur
Die Anwendung folgt einer **Client-Server-Architektur** mit folgenden Hauptkomponenten:

- **Server**: Lauscht auf eingehende Verbindungen und startet für jede Verbindung einen neuen Thread (`ClientHandler`).
- **Datenzugriffsschicht**: Verwaltet die Interaktion mit der PostgreSQL-Datenbank über Klassen wie `Database`, `UserDatabase` und `PackageDatabase`.
- **Modellklassen**: Repräsentieren die Datenstrukturen der Anwendung, z.B. `User`, `Card`, `MonsterCard`, `SpellCard`.
- **Sicherheitsmechanismus**: Implementiert token-basierte Authentifizierung zur Sicherung der Endpunkte.

## Entwurfsentscheidungen

### Verwendung von Sockets statt HTTP-Framework
Anstelle eines bestehenden HTTP-Frameworks wie Spring Boot wurde die HTTP-Kommunikation manuell über Sockets implementiert. Dies ermöglicht ein tieferes Verständnis des HTTP-Protokolls und der zugrunde liegenden Netzwerkkommunikation.

### JSON für Datenübertragung
Die **Jackson-Bibliothek** wird zur Serialisierung und Deserialisierung von JSON-Daten verwendet. Dies erleichtert die Verarbeitung von Anfragen und Antworten im JSON-Format.

### Token-basierte Sicherheit
Tokens werden zur Authentifizierung und Autorisierung der Benutzer verwendet. Nach erfolgreichem Login erhält der Benutzer ein Token, das für nachfolgende Anfragen im `Authorization`-Header mitgesendet werden muss.

### Modularität und Trennung der Anliegen
Die Anwendung ist in verschiedene Module unterteilt, die jeweils eine spezifische Verantwortung tragen:
- **ClientHandler**: Verarbeitet eingehende Anfragen und leitet sie an die entsprechenden Datenbankklassen weiter.
- **Datenbankklassen** (`UserDatabase`, `PackageDatabase`): Verantwortlich für CRUD-Operationen und Geschäftslogik.
- **Modellklassen**: Repräsentieren die Datenobjekte der Anwendung.



### Klassendiagramm 

FINDET MAN IM pom.xml !!! 