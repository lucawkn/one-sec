# App Interceptor

Native Android-App (Kotlin, Jetpack Compose), die das Öffnen ausgewählter Apps
(Instagram, TikTok, …) mit einer 10-sekündigen Atempause unterbricht – für
bewusstere Handynutzung. Optimiert für Xiaomi Redmi 13 Pro (MIUI/HyperOS).

## Funktionsweise

- Ein **AccessibilityService** (`InterceptorService`) erkennt Vordergrund-Wechsel
  über `TYPE_WINDOW_STATE_CHANGED`-Events.
- Bei einer überwachten App legt sich ein **Vollbild-Overlay** darüber:
  10 Sekunden Atemanimation ("Ein… / Aus…"), danach die Frage
  "Möchtest du [App] wirklich öffnen?".
- **Ja** → App wird freigegeben und für den konfigurierten Cooldown
  (Standard 5 min, einstellbar 1–30 min) nicht erneut unterbrochen.
- **Nein** → `GLOBAL_ACTION_HOME`, zurück zum Homescreen.
- Jede Entscheidung wird lokal in einer Room-Datenbank gespeichert
  (Statistik-Screen: heute / letzte 7 Tage).

### Warum TYPE_ACCESSIBILITY_OVERLAY?

Das Overlay nutzt primär `TYPE_ACCESSIBILITY_OVERLAY` statt
`TYPE_APPLICATION_OVERLAY`. Dieser Fenstertyp gehört zum AccessibilityService
und wird von MIUIs "Popup im Hintergrund"-Sperre in der Regel **nicht**
blockiert. `TYPE_APPLICATION_OVERLAY` (klassisches `SYSTEM_ALERT_WINDOW`)
ist als Fallback implementiert. Trotzdem führt das Onboarding durch alle
MIUI-Berechtigungen, weil sich MIUI-Versionen unterschiedlich verhalten.

## Build & Installation (Redmi 13 Pro)

1. Projekt in **Android Studio** öffnen (`File > Open` → dieser Ordner).
   Gradle 8.9 / AGP 8.5.2 / Kotlin 2.0.20 – der Wrapper lädt Gradle selbst.
   `local.properties` mit dem SDK-Pfad legt Android Studio automatisch an.
2. Auf dem Telefon: **Einstellungen > Mein Gerät > Alle Spezifikationen** →
   7× auf "OS-Version" tippen → Entwickleroptionen aktiv.
3. **Einstellungen > Weitere Einstellungen > Entwickleroptionen**:
   - "USB-Debugging" aktivieren
   - "Über USB installieren" aktivieren (MIUI verlangt dafür ggf. eine
     eingelegte SIM-Karte und/oder Mi-Konto-Anmeldung)
   - optional "USB-Debugging (Sicherheitseinstellungen)" für spätere Tests
4. Telefon per USB verbinden, Debugging-Prompt bestätigen, in Android Studio
   **Run ▶** (oder `gradlew.bat :app:installDebug`).

## Ersteinrichtung auf dem Gerät (wichtig!)

Beim ersten Start führt das Onboarding durch alle Schritte. Die kritischsten:

1. **Bedienungshilfen-Dienst** aktivieren. Auf Android 13+ ist der Schalter
   bei sideloaded Apps zunächst gesperrt ("Eingeschränkte Einstellung"):
   Einstellungen > Apps > App Interceptor > ⋮-Menü > "Eingeschränkte
   Einstellungen zulassen" – erst danach lässt sich der Dienst einschalten.
2. **MIUI "Weitere Berechtigungen"**: beide Popup-Einträge erlauben
   ("Popup-Fenster anzeigen" und "… während die App im Hintergrund läuft").
3. **Autostart** in der Security-App erlauben.
4. **Akku**: "Keine Einschränkungen" für die App.
5. App in der Übersicht der letzten Apps **sperren** (Schloss-Symbol), damit
   MIUI sie nicht wegräumt.

Der Home-Screen der App enthält einen **Health-Check**, der bei jedem Öffnen
prüft, ob alles noch aktiv ist (MIUI deaktiviert Dienste gern nach Updates),
und Deep-Links zum Beheben anbietet. Nach einem Neustart prüft ein
`BOOT_COMPLETED`-Receiver den Dienststatus und warnt per Notification.

## Projektstruktur

```
app/src/main/java/com/luca/appinterceptor/
├── App.kt                        Application, initialisiert Graph + Notification-Channel
├── boot/BootReceiver.kt          Warnung nach Reboot, falls Dienst deaktiviert
├── data/
│   ├── Graph.kt                  Mini-Service-Locator (Settings + DB)
│   ├── SettingsRepository.kt     DataStore: überwachte Apps, Cooldown, Onboarding
│   └── AppDatabase.kt            Room: InterceptEvent + EventDao
├── service/InterceptorService.kt AccessibilityService, Kern der Erkennung
├── overlay/
│   ├── OverlayController.kt      WindowManager-Overlay (Accessibility/App-Overlay)
│   ├── OverlayLifecycleOwner.kt  Lifecycle-Bridge für ComposeView ohne Activity
│   └── OverlayScreen.kt          Atemanimation + Entscheidungsdialog
├── ui/
│   ├── MainActivity.kt           Navigation (home/onboarding/apps/stats)
│   ├── home/HomeScreen.kt        Health-Check + Einstellungen
│   ├── onboarding/OnboardingScreen.kt  MIUI-Permission-Flow mit Deep-Links
│   ├── apps/AppSelectionScreen.kt      Installierte Apps, Mehrfachauswahl
│   └── stats/StatsScreen.kt      Geöffnet vs. abgebrochen (heute/7 Tage)
└── util/
    ├── MiuiIntents.kt            Xiaomi-Deep-Links mit Fallback-Kaskade
    ├── PermissionChecks.kt       inkl. MIUI-AppOp 10021 (Popup im Hintergrund)
    └── Notifications.kt
```

## Bekannte Grenzen

- Die MIUI-Popup-Prüfung (AppOp 10021) ist eine undokumentierte Heuristik –
  auf manchen HyperOS-Versionen liefert sie kein Ergebnis (Status "ℹ" =
  manuell prüfen).
- Die Xiaomi-Deep-Links variieren je nach Version; schlägt einer fehl, zeigt
  die App die manuelle Anleitung an.
- Der Autostart-Status ist programmatisch nicht abfragbar (MIUI bietet keine
  öffentliche API), daher dort nur Anleitung + Deep-Link.
