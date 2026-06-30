# راهنمای DEV/PRD و Windows/Linux برای ProfessionalMapPro

این فایل تفاوت محیط توسعه و production و دستورهای عملیاتی Windows/Linux را پوشش می‌دهد.

- WorkManager hardening: persisted observers skip handled finished work IDs; this reduces repeated state churn on Windows/Linux CI/device test runs.

## تفاوت DEV و PRD

| موضوع | DEV | PRD |
| --- | --- | --- |
| Build type | `debug` | `release` |
| Application ID | `com.msa.professionalmap.debug` | `com.msa.professionalmap` |
| Signing | debug signing | keystore امن سازمانی |
| Minify/shrink | معمولاً غیرفعال | فعال |
| Routing endpoint | dev/internal/test | production SLA-backed |
| Firebase | اختیاری | طبق policy سازمان |
| Logs | debug-friendly | بدون secret و PII حساس |
| Cleartext | فقط با opt-in توسعه | غیرفعال |
| Notification privacy | باید تست شود | اجباری |
| Wrapper | باید رسمی باشد | commit شده و checksum-verified |

## متغیرهای مهم محیطی

```text
ANDROID_HOME
ANDROID_SDK_ROOT
PMP_OSRM_BASE_URL
PMP_ROUTING_USER_AGENT
PMP_RELEASE_STORE_FILE
PMP_RELEASE_STORE_PASSWORD
PMP_RELEASE_KEY_ALIAS
PMP_RELEASE_KEY_PASSWORD
PMP_ALLOW_LOCAL_GRADLE_FALLBACK
```

`PMP_ALLOW_LOCAL_GRADLE_FALLBACK` فقط برای شرایط موقت توسعه است. در PRD/CI نباید جایگزین wrapper رسمی شود.

## Windows PowerShell

### تنظیم Android SDK

```powershell
setx ANDROID_HOME "$env:LOCALAPPDATA\Android\Sdk"
setx ANDROID_SDK_ROOT "$env:LOCALAPPDATA\Android\Sdk"
```

ترمینال را ببندید و دوباره باز کنید.

### اجرای چک‌های پروژه

در PowerShell با Git Bash یا WSL راحت‌تر است. اگر Bash در دسترس است:

```powershell
bash ./scripts/verify-static.sh
bash ./scripts/verify-architecture.sh
bash ./scripts/verify-test-suite.sh
bash ./scripts/verify-build-environment.sh
```

### تولید wrapper

```powershell
bash ./scripts/bootstrap-gradle-wrapper.sh
```

### اجرای build debug

```powershell
.\gradlew.bat :app:assembleDebug --stacktrace
```

### اجرای test matrix

```powershell
.\gradlew.bat `
  :core:model:test `
  :core:mapdata:test `
  :core:routing:test `
  :core:progress:test `
  :core:guidance:test `
  :core:geo:testDebugUnitTest `
  :core:location:testDebugUnitTest `
  :core:offline:testDebugUnitTest `
  :core:service:testDebugUnitTest `
  :core:observability:testDebugUnitTest `
  :feature:map:testDebugUnitTest `
  --stacktrace
```

### build release با env در PowerShell

```powershell
$env:PMP_OSRM_BASE_URL="https://routing.your-company.example"
$env:PMP_ROUTING_USER_AGENT="ProfessionalMapPro/1.0 production"
$env:PMP_RELEASE_STORE_FILE="C:\secure\release.jks"
$env:PMP_RELEASE_STORE_PASSWORD="***"
$env:PMP_RELEASE_KEY_ALIAS="release"
$env:PMP_RELEASE_KEY_PASSWORD="***"
.\gradlew.bat :app:assembleRelease --stacktrace
```

## Linux Bash

### تنظیم Android SDK

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/platform-tools:$PATH"
```

برای دائمی شدن، این خطوط را به `~/.bashrc` یا profile مربوطه اضافه کنید.

### نصب NDK و CMake با sdkmanager

```bash
sdkmanager "ndk;28.2.13676358" "cmake;3.22.1"
```

### تولید wrapper و بررسی محیط

```bash
./scripts/bootstrap-gradle-wrapper.sh
./scripts/verify-build-environment.sh
```

### اجرای چک‌ها

```bash
./scripts/verify-static.sh
./scripts/verify-architecture.sh
./scripts/verify-test-suite.sh
```

### build debug

```bash
./gradlew :app:assembleDebug --stacktrace
```

### build release

```bash
PMP_OSRM_BASE_URL="https://routing.your-company.example" \
PMP_ROUTING_USER_AGENT="ProfessionalMapPro/1.0 production" \
PMP_RELEASE_STORE_FILE="/secure/release.jks" \
PMP_RELEASE_STORE_PASSWORD="***" \
PMP_RELEASE_KEY_ALIAS="release" \
PMP_RELEASE_KEY_PASSWORD="***" \
./gradlew :app:assembleRelease --stacktrace
```

## مسیرهای مهم

```text
app/build/outputs/apk/debug/
app/build/outputs/apk/release/
app/build/outputs/bundle/release/
app/build/reports/
app/build/outputs/mapping/release/
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
app/google-services.json
local.properties
```

## Permissions و فایل‌های حساس

این فایل‌ها نباید در repository عمومی یا source artifact معمولی قرار بگیرند:

```text
*.jks
*.keystore
*.p12
*.pem
*.key
.env
*.env
secrets.properties
release-signing.properties
app/google-services.json
```

در Linux مجوز keystore را محدود کنید:

```bash
chmod 600 /secure/release.jks
```

در Windows فایل keystore را در مسیر امن خارج از repository نگه دارید و دسترسی آن را محدود کنید.

## systemd

این پروژه یک Android app است و سرویس server-side systemd ندارد. اگر در آینده routing provider داخلی یا backend جداگانه برای آن deploy شد، systemd باید در repository همان backend مستند شود، نه داخل این پروژه Android.

## Troubleshooting مخصوص Windows

### اسکریپت‌های Bash اجرا نمی‌شوند

Git Bash یا WSL نصب کنید و دستورها را با `bash` اجرا کنید:

```powershell
bash ./scripts/verify-static.sh
```

### مسیر SDK با backslash مشکل دارد

در `local.properties` از مسیر absolute استفاده کنید. نمونه:

```properties
sdk.dir=C\:\\Users\\YOUR_USER\\AppData\\Local\\Android\\Sdk
```

### خطای طول مسیر

Long paths را در Windows فعال کنید یا پروژه را در مسیر کوتاه مثل `C:\src\ProfessionalMapPro` قرار دهید.

## Troubleshooting مخصوص Linux

### permission denied برای gradlew

```bash
chmod +x gradlew scripts/*.sh
```

### JDK اشتباه انتخاب شده

```bash
java -version
./gradlew -version
```

CI از JDK 21 استفاده می‌کند و Gradle toolchain کامپایل را روی Java 17 تنظیم کرده است.

### Android SDK پیدا نمی‌شود

```bash
echo "$ANDROID_HOME"
echo "$ANDROID_SDK_ROOT"
ls "$ANDROID_HOME/platforms/android-36"
```

اگر SDK نصب نیست، Android Studio یا command line tools را نصب کنید.

## چک نهایی قبل از تحویل PRD

- [ ] wrapper JAR commit شده است.
- [ ] checksum wrapper و distribution پاس شده است.
- [ ] build debug پاس شده است.
- [ ] build release با signing پاس شده است.
- [ ] test matrix پاس شده است.
- [ ] lint پاس شده است.
- [ ] route provider production تنظیم شده است.
- [ ] real device validation انجام شده است.
- [ ] secret داخل artifact وجود ندارد.
- [ ] چهار فایل documentation بیشتر وجود ندارد.

## UI Validation در DEV و PRD

در DEV و PRD علاوه بر اجرای build، صفحه نقشه را روی اندازه‌های مختلف emulator تست کنید: compact phone، landscape و tablet/desktop-like. انتظار درست این است که کنترل‌ها روی موبایل به صورت bottom-sheet محدود و روی صفحه بزرگ به صورت side panel کنار نقشه نمایش داده شوند.

در DEV روی emulator و دستگاه واقعی، حالت‌های edge-to-edge، navigation bar، status bar، RTL/LTR و dynamic color را تست کنید. در PRD، smoke test شامل start GPS، route calculation، start/stop navigation، voice guidance، offline download و teardown سرویس ناوبری باشد.
- UI verification در DEV و PRD: بعد از build، صفحه نقشه را در compact و expanded window تست کنید تا MapSystemStatusStrip، Route Focus Card، MapQuickActions و control panel به‌درستی نمایش داده شوند.

## UI consistency در DEV و PRD

در DEV و PRD، مسیر تست دستی UI شامل حالت‌های permission denied، GPS active، route ready، routing error، navigation active و offline worker است. این حالت‌ها باید در HUD، status strip و control-panel header خوانا و سازگار نمایش داده شوند.

## کنترل کد در محیط DEV/PRD

در smoke test نهایی، علاوه بر start/stop navigation و offline download، وضعیت panel actionها را هم بررسی کنید: اکشن‌های location/routing/navigation/guidance/offline باید شکل و touch target یکسان داشته باشند. در لاگ‌ها هم مطمئن شوید completed offline workers بدون خطای observer cleanup از لیست active jobs حذف می‌شوند.
