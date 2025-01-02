# MCTG (Monster Trading Card Game)

## Überblick
Dieses Projekt implementiert einen einfachen Server, der Clients empfängt und HTTP-Anfragen verarbeitet, **ohne** ein externes HTTP-Framework zu verwenden. Die Anwendung unterstützt:
- **Benutzerregistrierung** und **Login**
- **Paketverwaltung** (Erstellen und Kaufen von Kartenpaketen)
- **Kartenverwaltung** (Anzeigen, Deck-Konfiguration)
- **Battles** (Kampflogik, ELO-Berechnung, Booster-Karten)
- **Trading** (Handel mit Karten)
- **Datenbank**-Anbindung (PostgreSQL) via JDBC

Die token-basierte Sicherheit sorgt dafür, dass nur authentifizierte Benutzer auf geschützte Endpunkte zugreifen können.

## Architektur
Die Anwendung folgt einer **Client-Server-Architektur** mit folgenden Hauptkomponenten:

- **Server**  
  Lauscht auf eingehende Verbindungen und startet für jede Verbindung einen eigenen Thread (`ClientHandler`).

- **ClientHandler**  
  Parst die eingehenden HTTP-Requests (Methode, Pfad, Headers, Body) und leitet sie anhand des Pfads an spezialisierte Methoden weiter. Diese Methoden rufen dann die entsprechenden Datenbankklassen (z.B. `UserDatabase`, `PackageDatabase`) auf und erstellen HTTP-Antworten.

- **Datenbank-Klassen** (`UserDatabase`, `PackageDatabase`, `TradingDatabase` …)  
  Verantwortlich für CRUD-Operationen und Geschäftslogik im Zusammenhang mit Benutzern, Paketen und Trading-Deals.

- **Battle**, **Player**  
  Implementieren die Spielmechanik bzw. die Kampf-Logik.

- **Modellklassen**
    - `User`: Repräsentiert einen Benutzer samt Coins, ELO, Profilinfos …
    - `Card`, `MonsterCard`, `SpellCard`: Repräsentieren Spielkarten (Monstertyp, Zaubertyp, Element, Schaden …)
    - `TradingDeal`: Beschreibt ein Tauschgeschäft.

## Technische Schritte (Protokoll)
Während der Entwicklung wurden diverse **Designentscheidungen** getroffen und teilweise wieder verworfen. Die wichtigsten Schritte:

1. **Zunächst** wurde ein simpler Socket-Server (`Main` + `ClientHandler`) erstellt, der auf Port 10001 lauscht.
    - *Herausforderung*: Manuelles HTTP-Parsing (Request Line, Header, Body) statt Verwendung eines Frameworks.
    - **Lösung**: Implementierung einer einfachen Schleife mit `readLine()`, Auftrennung der ersten Zeile (HTTP-Methode, -Pfad, -Version), sowie Parsen der Header bis zur Leerzeile.

2. **Einbindung der Datenbank** (PostgreSQL):
    - *Herausforderung*: Sichere DB-Zugriffe ohne SQL-Injections und Transaktionssicherheit.
    - **Lösung**: Verwendung von `PreparedStatement` und Transaktionshandling (`conn.setAutoCommit(false)` … `conn.commit()`/`rollback()`).

3. **Karten- und Paketlogik**
    - Design der Tabellen `cards`, `packages`, `package_cards`, `user_cards` …
    - *Herausforderung*: Jeder User hat beliebig viele Karten, ein Paket besteht aus 5 Karten, etc.
    - **Lösung**: Separate Zuordnungstabellen (z.B. `package_cards`), Verknüpfung über Fremdschlüssel (UUIDs).

4. **Battle-Mechanik**
    - *Herausforderung*: Realisierung der Element-Multiplikatoren und Sonderregeln (z.B. Goblin vs. Dragon).
    - **Lösung**: Methode `applySpecialRules()` im `Battle`-Objekt, die gezielt Ausnahmen abfragt.

5. **Booster-Feature**
    - *Herausforderung*: Ein User kann eine Karte als “Booster” festlegen, die einmalig den Schaden verdoppelt.
    - **Lösung**: Speicherort in `users.booster_card_id`, Logik in `Player.isBoosterCard()`.

Während der Entwicklung gab es immer wieder **Fehlschläge** (etwa unklare Trennung zwischen DB-Schicht und Anwendungslogik) – diese wurden behoben, indem wir **dedizierte Klassen** für Datenbankzugriffe geschrieben haben, um das Chaos aus dem ClientHandler zu verbannen.

## Warum diese Unit-Tests?
- **Ziel**: Wir wollten **kritische Kernfunktionen** sicherstellen, z.B.:
    - Konstruktoren und Getter/Setter der Karten (z.B. `MonsterCard`, `SpellCard`)  
      → **Grundlage** des gesamten Kartenspiels; wenn hier Fehler sind, funktionieren auch Käufe, Kämpfe, Trading nicht korrekt.
    - Element-Multiplikator (`Battle.getEffectivenessMultiplier`)  
      → **Kernstück** der Kampflogik. Fehler würden das Balancing komplett zerstören.
    - Sonderregeln (`Battle.applySpecialRules`)  
      → Die Spezialfälle (Goblin <-> Dragon, Wizzard <-> Ork etc.) sind ein wichtiger Teil der Spielmechanik.
    - Booster-Karten in `Player` (z.B. `isBoosterCard()`)  
      → Dieses Feature kann Kampfausgänge stark verändern – sollte also korrekt funktionieren.

Deshalb sind die **Unit-Tests** genau auf diese Komponenten fokussiert. Sobald eines dieser Elemente versagt, würde das Spielerlebnis beeinträchtigt, und ein Großteil der Spielabläufe würden nicht mehr korrekt funktionieren.

## Zeitaufwand
Die **Entwicklung dieses Projekts** (Backend-Logik, Datenbank-Anbindung, das manuelle HTTP-Parsing plus das Erstellen und Ausführen der Tests) hat ungefähr **80 bis 90 Arbeitsstunden** in Anspruch genommen.
- ca. 20h Konzept (Datenmodell, UML, Skizzen)
- ca. 25h Implementierung des Servers und der Datenbankklassen
- ca. 15h Feinschliff der Battle- und Trading-Logik
- ca. 15h Erstellen und Anpassen der Tests (Unit-Tests)
- ca. 10h Fehlersuche und Bugfixing


## Git-History
Die **Git-History** ist ebenfalls Teil der Projektdokumentation. Darin sind sämtliche Commits und Entwicklungsschritte protokolliert. Ich liste sie hier nicht explizit auf, da sie bereits in Git selbst vorhanden ist. Jeder Commit enthält in der Regel eine kurze Beschreibung (nicht allzu ausführliche) über die Änderungen und damit auch den Prozess der Projektentstehung.

Der gesamte Quellcode ist unter folgendem Link verfügbar:  
[https://github.com/callhimcoul/MCTG.git](https://github.com/callhimcoul/MCTG.git)


