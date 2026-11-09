# Pixel NetSpeed — Native Android 17 Live Network Speed Indicator

**Pixel NetSpeed** is a lightweight, production-grade Android application designed specifically for Google Pixel devices (targeting **Android 17 / API 37**) that displays real-time internet and network traffic speed.

---

## 🌟 Key Features

1. **Accurate Real-Time Network Speed Measurement**:
   - Measures actual device-wide received (RX) and transmitted (TX) traffic via `TrafficStats`.
   - Uses `SystemClock.elapsedRealtime()` monotonic clock for precise rate computation.
   - Exponential moving average (EMA) filter to smooth micro-fluctuations while remaining ultra-responsive.
   - Safely handles negative deltas, interface resets, device reboots, and unsupported counters.

2. **Floating Status Bar Indicator**:
   - Renders a floating speed widget positioned immediately below the status bar using `TYPE_APPLICATION_OVERLAY`.
   - Drag-to-position gesture with position persistence using AndroidX DataStore.
   - Customizable text size, background opacity, text color, background color, and layout orientation (compact horizontal vs. stacked vertical).

3. **Persistent Live Notification**:
   - Ongoing status bar notification displaying real-time download and upload speeds.
   - Low-priority notification channel (`IMPORTANCE_LOW`) for silent updates.
   - Direct action button to stop monitoring with one tap.

4. **Pixel Quick Settings Tile**:
   - Android `TileService` integration allowing users to toggle network monitoring directly from the Quick Settings panel.
   - Automatically synchronizes tile state (`ACTIVE` / `INACTIVE`).

5. **Android 17 / API 37 Compliance**:
   - Native compatibility with Android 17 behavior changes.
   - Compliant Foreground Service declared with `foregroundServiceType="specialUse"` and `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`.
   - Granular permission handling for `SYSTEM_ALERT_WINDOW`, `POST_NOTIFICATIONS`, and `ACCESS_NETWORK_STATE`.

6. **Modern Jetpack Compose & Material 3 UI**:
   - Polished Material You dashboard with live speed meters, active network transport detection (Wi-Fi, Cellular, Ethernet, VPN), and permission setup cards.
   - Live widget preview in Appearance settings.

---

## 🏗️ Architecture

The codebase follows Clean Architecture principles:

- `data/model/`: Value classes (`NetworkSpeed`, `AppSettings`, `SpeedUnit`, `DisplayMode`, `NetworkStatus`).
- `data/repository/`:
  - `NetworkTrafficDataSource`: Abstraction over `TrafficStats`.
  - `UserPreferencesRepository`: Preference storage backed by AndroidX DataStore.
- `domain/`:
  - `SpeedCalculator`: Baseline tracking, rate calculation, EMA filter, unit conversion, and display formatting.
  - `NetworkMonitor`: Network capabilities listener using `ConnectivityManager`.
- `service/`:
  - `NetSpeedForegroundService`: Foreground monitoring service managing the sampling loop, notification, and overlay.
  - `NetSpeedTileService`: Quick Settings tile provider.
- `ui/overlay/`:
  - `OverlayManager`: WindowManager controller managing the floating window view and touch dragging.
- `ui/screens/` & `ui/viewmodel/`:
  - `DashboardScreen`, `AppearanceSettingsScreen`, `GeneralSettingsScreen`, `PermissionGuideScreen`, and `MainViewModel`.

---

## 🛠️ Build & Installation

### Build Requirements
- JDK 17+
- Android SDK 37 (Android 17)
- Android Gradle Plugin 9.5.1
- Kotlin 2.0.21

### Building the Debug APK
```bash
./gradlew clean
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

The compiled APK will be generated at:
`app/build.gradle.kts` -> `app/build/outputs/apk/debug/app-debug.apk`

---

## 🧪 Unit Test Execution Results

All 13 unit tests executed successfully:
- `first sample establishes baseline and returns zero speed` (PASSED)
- `subsequent sample calculates correct download and upload speeds` (PASSED)
- `counter reset or device reboot handles negative deltas gracefully` (PASSED)
- `unsupported traffic stats returns unavailable speed` (PASSED)
- `formatSpeed AUTO_BYTES formats bytes kilobytes megabytes correctly` (PASSED)
- `formatSpeed KBPS_BIT calculates kilobits correctly` (PASSED)
- `formatDisplayText outputs correct layout string for display modes` (PASSED)
- `smoothing factor averages rapid fluctuations correctly` (PASSED)
- `default AppSettings has correct defaults` (PASSED)
- `AppSettings copy retains un-modified values` (PASSED)
- `combinedBytesPerSec calculates sum of download and upload` (PASSED)
- `zero network speed returns zero combined` (PASSED)

---

## 📄 License & Compatibility
Target SDK: **37 (Android 17)**  
Minimum SDK: **26 (Android 8.0+)**  
Package Name: `com.pixel.netspeed`
