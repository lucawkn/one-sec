# One Sec Interceptor

Ein nativer Android "App-Interceptor" (Kotlin, Jetpack Compose), der bewusstes
Nutzen von Apps wie Instagram oder TikTok fördert: Sobald eine ausgewählte App
in den Vordergrund kommt, legt sich eine kurze Atempause mit anschließender
Rückfrage darüber, bevor die App tatsächlich benutzt werden kann.

Nur für den privaten Gebrauch / Sideloading gedacht (kein Play-Store-Ziel).

## Funktionsweise

1. **App-Auswahl** (Tab "Apps"): Liste aller startbaren Apps auf dem Gerät,
   mit Schaltern zum Markieren als "überwacht".
2. **Erkennung**: Ein `AccessibilityService` beobachtet
   `TYPE_WINDOW_STATE_CHANGED`-Events und erkennt so App-Wechsel.
3. **Unterbrechung**: Kommt eine überwachte App in den Vordergrund, legt sich
   sofort ein Vollbild-Overlay (`SYSTEM_ALERT_WINDOW`) darüber: 10 Sekunden
   (konfigurierbar) pulsierender Atem-Kreis mit Countdown, danach die
   Rückfrage "Möchtest du [App] wirklich öffnen?".
4. **Ja, öffnen**: Overlay schließt, App bleibt für eine konfigurierbare
   Ruhezeit (Standard 5 Minuten) von weiteren Unterbrechungen ausgenommen.
5. **Nein**: Nutzer landet auf dem Homescreen.
6. **Statistik** (Tab "Statistik"): Wie oft eine App geöffnet vs. abgebrochen
   wurde, gefiltert nach Heute / Diese Woche / Gesamt.

## Zuverlässige Erkennung von App-Wechseln

`AppInterceptorAccessibilityService` reagiert nur auf
`TYPE_WINDOW_STATE_CHANGED`-Events und leitet daraus die aktuell im
Vordergrund stehende App (`event.packageName`) ab. Um Fehlalarme zu
vermeiden:

- **Deduplizierung**: Es wird nur reagiert, wenn sich das Package gegenüber
  dem zuletzt gesehenen tatsächlich ändert - die vielen Events, die eine App
  für ihre eigenen Unterfenster/Activities feuert, werden ignoriert.
- **Debounce** (300 ms): Fängt Bursts von Events während eines einzelnen
  Übergangs ab (z. B. Splashscreen gefolgt von der Hauptactivity).
- **`ForegroundAppFilter`**: Blendet Packages aus, die nie sinnvoll "geöffnet"
  werden - die eigene App/das eigene Overlay, den aktuellen Standard-Launcher
  (dynamisch über `Intent.ACTION_MAIN`/`CATEGORY_HOME` aufgelöst) und bekannte
  Systempakete (`com.android.systemui`, Tastaturen, ...).
- **Grace-Period-Check**: Selbst für überwachte Apps wird die Sperre
  übersprungen, solange die App noch innerhalb ihrer Ruhezeit aus einem
  vorherigen "Ja, öffnen" liegt (`SnoozedAppEntity` in Room).

Das Overlay-Fenster selbst löst keine eigenen `TYPE_WINDOW_STATE_CHANGED`-
Events mit fremdem Package-Namen aus, die einen erneuten Trigger verursachen
könnten.

## Projektstruktur

```
app/src/main/java/com/onesec/interceptor/
├── OneSecApp.kt                  Application-Klasse, initialisiert ServiceLocator
├── di/ServiceLocator.kt          Manuelles DI (kein Hilt nötig für dieses Projekt)
├── data/
│   ├── local/                    Room: Entities, DAOs, AppDatabase
│   ├── repository/               MonitoredAppRepository, StatsRepository
│   └── settings/                 SettingsDataStore (Preferences DataStore)
├── model/InstalledAppInfo.kt
├── util/                         InstalledAppsProvider, PermissionUtils
├── service/
│   ├── AppInterceptorAccessibilityService.kt
│   └── ForegroundAppFilter.kt
├── overlay/
│   ├── OverlayManager.kt          WindowManager-Overlay-Verwaltung
│   ├── OverlayLifecycleOwner.kt   Lifecycle/SavedState/ViewModelStore für Compose außerhalb einer Activity
│   └── InterceptorOverlayContent.kt  Atem-Animation + Bestätigungsdialog (Compose)
└── ui/
    ├── MainActivity.kt, navigation/, theme/
    ├── onboarding/                Berechtigungs-Onboarding mit Deep-Links zu den Systemeinstellungen
    ├── appselection/              App-Auswahl-Screen
    ├── settings/                  Ruhezeit, Atemdauer, Master-Schalter
    └── stats/                     Statistik-Screen
```

Persistenz: **Room** für überwachte Apps, Intercept-Events (Statistik) und
Snooze-Status; **DataStore (Preferences)** für globale Einstellungen
(Master-Schalter, Ruhezeit, Atemdauer).

## Projekt öffnen & auf dem Gerät installieren

Voraussetzung: [Android Studio](https://developer.android.com/studio)
(inkl. Android SDK), ein Gerät mit aktiviertem USB-Debugging.

1. Projekt in Android Studio öffnen (diesen Ordner auswählen). Android
   Studio lädt beim ersten Öffnen automatisch die passende Gradle-Distribution
   über den mitgelieferten Wrapper (`./gradlew`).
2. Gerät per USB anschließen, USB-Debugging bestätigen.
3. "Run" (▶) in Android Studio auf das Zielgerät ausführen - oder per
   Kommandozeile:

   ```bash
   ./gradlew installDebug
   ```

4. Nach der Installation in der App:
   - Tab **Status**: Bedienungshilfe aktivieren (führt in die
     Systemeinstellungen) und Overlay-Berechtigung erteilen.
   - Tab **Apps**: gewünschte Apps als überwacht markieren.
   - Tab **Einstellungen**: Ruhezeit / Atemdauer bei Bedarf anpassen.

## Hinweis zu dieser Umgebung

Dieses Projekt wurde in einer Sandbox ohne Zugriff auf das Android SDK / die
Google-Maven-Repositories erstellt - ein `gradle build` konnte hier deshalb
nicht ausgeführt werden. Der komplette Quellcode und das Gradle-Setup
(inkl. generiertem Wrapper) sind vorhanden; der erste Build/Sync sollte in
Android Studio auf deinem Rechner erfolgen, wo der Zugriff auf
`dl.google.com` und `services.gradle.org` gegeben ist.
