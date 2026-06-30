# راهنمای راه‌اندازی توسعه ProfessionalMapPro

این فایل برای راه‌اندازی محیط توسعه پروژه Android/Kotlin نوشته شده است. پروژه فعلی KMM نیست و source setهای `commonMain`، `iosMain` یا `desktopMain` ندارد.

- در پاس نهایی کد، مشاهده‌ی persisted WorkManager سخت‌تر شد: workهای تمام‌شده بعد از یک‌بار پردازش دوباره در هر polling tick پردازش نمی‌شوند.

## وضعیت فعلی build

در این نسخه، اسکریپت‌های static، architecture، test-suite guardrail و artifact verification پاس شده‌اند؛ اما build/test واقعی Gradle تا وقتی فایل زیر داخل repository نباشد کامل تأیید نشده است:

```text
gradle/wrapper/gradle-wrapper.jar
```

اولین کار در محیط توسعه باید تولید wrapper رسمی و سپس اجرای full build باشد.

## نکته مهم AGP 9 / Kotlin

در AGP 9 پشتیبانی Kotlin داخل خود Android Gradle Plugin فعال است؛ بنابراین ماژول‌های Android نباید پلاگین `org.jetbrains.kotlin.android` یا alias قدیمی `libs.plugins.kotlin.android` را apply کنند. نسخه Kotlin Gradle Plugin در root `build.gradle.kts` برای built-in Kotlin و پلاگین‌های compiler مثل Compose pin شده است.

## پیش‌نیازها

نصب کنید:

- JDK 21 برای هماهنگی با CI.
- Android Studio جدید با Android SDK 36.
- NDK نسخه `28.2.13676358`.
- CMake نسخه `3.22.1`.
- Git.
- Bash-compatible shell مثل Git Bash یا WSL در Windows.

پروژه با Gradle toolchain روی Java 17 کامپایل می‌شود، ولی CI از JDK 21 استفاده می‌کند.

## تنظیم Android SDK

در Linux/macOS:

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/platform-tools:$PATH"
```

در Windows PowerShell:

```powershell
setx ANDROID_HOME "$env:LOCALAPPDATA\Android\Sdk"
setx ANDROID_SDK_ROOT "$env:LOCALAPPDATA\Android\Sdk"
```

بعد از تنظیم متغیرها، ترمینال را ببندید و دوباره باز کنید.

## تولید Gradle Wrapper رسمی

اگر `gradle/wrapper/gradle-wrapper.jar` وجود ندارد:

```bash
./scripts/bootstrap-gradle-wrapper.sh
```

بعد از تولید wrapper، این فایل‌ها را بررسی و commit کنید:

```text
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
gradlew
gradlew.bat
```

اگر فقط برای بررسی موقت مجبور شدید از Gradle نصب‌شده روی سیستم استفاده کنید، fallback فقط با این env مجاز است:

```bash
PMP_ALLOW_LOCAL_GRADLE_FALLBACK=1 ./gradlew --version
```

برای repository production این fallback نباید جایگزین wrapper رسمی شود.

## بررسی محیط توسعه

```bash
./scripts/verify-build-environment.sh
```

این اسکریپت وجود wrapper jar، checksum، JDK و Android SDK را بررسی می‌کند.

## اجرای چک‌های سریع

```bash
./scripts/verify-static.sh
./scripts/verify-architecture.sh
./scripts/verify-test-suite.sh
```

این چک‌ها جایگزین build واقعی نیستند، اما برای catch کردن خطاهای ساختاری، dependency direction، documentation policy، security guardrails و test coverage guardrails استفاده می‌شوند.

## اجرای برنامه در حالت Debug

```bash
./gradlew :app:assembleDebug --stacktrace
```

یا از Android Studio:

1. پروژه را از root باز کنید.
2. صبر کنید Gradle sync کامل شود.
3. build variant را روی debug بگذارید.
4. روی emulator یا device واقعی اجرا کنید.

برای تست GPS، foreground service، notification permission و offline map بهتر است حداقل روی یک device واقعی هم تست بگیرید.

## اجرای تست‌ها

```bash
./gradlew \
  :core:model:test \
  :core:mapdata:test \
  :core:routing:test \
  :core:progress:test \
  :core:guidance:test \
  :core:geo:testDebugUnitTest \
  :core:location:testDebugUnitTest \
  :core:offline:testDebugUnitTest \
  :core:service:testDebugUnitTest \
  :core:observability:testDebugUnitTest \
  :feature:map:testDebugUnitTest \
  --stacktrace
```

## اجرای lint

```bash
./gradlew \
  :app:lintDebug \
  :feature:map:lintDebug \
  :core:location:lintDebug \
  :core:offline:lintDebug \
  :core:service:lintDebug \
  :core:observability:lintDebug \
  --stacktrace
```

## تنظیم routing در محیط توسعه

مقدار پیش‌فرض routing باید فقط برای توسعه استفاده شود. برای endpoint داخلی یا تستی:

```bash
PMP_OSRM_BASE_URL="https://dev-routing.example" \
PMP_ROUTING_USER_AGENT="ProfessionalMapPro/dev" \
./gradlew :app:assembleDebug
```

اگر endpoint فقط در شبکه داخلی در دسترس است، VPN و DNS را قبل از اجرای برنامه بررسی کنید.

## Firebase در توسعه

اگر `app/google-services.json` وجود نداشته باشد، پلاگین‌های Firebase در build غیرفعال می‌شوند. برای تست Crashlytics/Analytics، فایل واقعی را فقط به صورت local اضافه کنید و آن را commit نکنید.

## ساختار ماژول‌ها

```text
app                 پوسته Android و تنظیمات release/debug
feature:map         UI، presentation state، controllers و use caseهای map
core:model          مدل‌ها و contractهای pure
core:geo            محاسبات geo و JNI/C++
core:mapdata        catalog نقشه و مسیرهای reference
core:routing        قرارداد routing، OSRM adapter و cache
core:location       location contract و fused-location adapter
core:progress       progress، off-route detection و route matching
core:guidance       voice guidance و متن instruction
core:offline        offline map، MapLibre و WorkManager bridge
core:service        foreground navigation service و notification
core:observability  abstraction مانیتورینگ و Firebase adapter
```

## مشکلات رایج توسعه

### خطای نبود wrapper jar

```text
gradle/wrapper/gradle-wrapper.jar is missing
```

راه‌حل:

```bash
./scripts/bootstrap-gradle-wrapper.sh
./scripts/verify-build-environment.sh
```

### خطای Android SDK

اگر `ANDROID_HOME` یا `ANDROID_SDK_ROOT` تنظیم نیست، مسیر SDK را در environment یا `local.properties` مشخص کنید.

نمونه `local.properties`:

```properties
sdk.dir=/absolute/path/to/Android/Sdk
```

این فایل نباید به شکل وابسته به سیستم افراد commit شود.

### خطای NDK یا CMake

Native geo module به NDK و CMake نیاز دارد. از Android Studio SDK Manager این دو نسخه را نصب کنید:

```text
ndk;28.2.13676358
cmake;3.22.1
```

### خطای signing در release

برای debug نیازی به signing release نیست. برای release باید متغیرهای `PMP_RELEASE_*` تنظیم شوند.

### رفتار متفاوت notification روی Android 13+

مجوز notification را روی Android 13 به بعد جداگانه بررسی کنید. Foreground service و actions باید روی device واقعی تست شوند.

## چک UI و تجربه کاربری

برای توسعه UI، اول از primitiveهای مشترک `MapUiKit` استفاده کنید: `MapGlassPanel` برای پنل‌های شناور، `MapSectionCard` برای بخش‌های کنترل، و `StatusPill` برای وضعیت‌ها. ساختار اصلی صفحه در `MapScreenContent` adaptive است: موبایل از bottom-sheet محدود و اسکرول‌شونده استفاده می‌کند و صفحه‌های بزرگ‌تر پنل کنترلی را کنار نقشه قرار می‌دهند. قبل از Pull Request، `./scripts/verify-static.sh` را اجرا کنید تا guardrailهای UI، اندازه فایل‌ها، import hygiene، quick actions و semantics اصلی بررسی شود.

## نکته UI حرفه‌ای

در نسخه UI جدید، فایل‌های `MapUiKit.kt`، `MapUiTiles.kt`، `MapRouteFocusCard.kt` و `MapScreenContent.kt` هسته طراحی را نگه می‌دارند. برای تغییر UI، اول از همین primitiveها استفاده کنید تا پنل‌ها، metric tileها، HUD و layout واکنش‌گرا یکدست بمانند.
- نکته UI: فایل‌های MapUiLayout، MapSystemStatusStrip، MapRouteFocusCard، MapQuickActions و MapUiTiles بخشی از Design System داخلی نقشه هستند و نباید بدون اجرای verify-static تغییر داده شوند.

## نکته UI برای توسعه‌دهنده

برای تغییر UI صفحه نقشه، primitiveهای مشترک را در `MapUiKit.kt`، `MapUiTiles.kt`، `MapQuickActions.kt`، `MapControlPanelHeader.kt` و `MapUiLayout.kt` نگه دارید. اکشن‌های اصلی نباید با Button خام ساخته شوند؛ از `MapPrimaryAction` و `MapSecondaryAction` استفاده کنید تا touch target و شکل ظاهری در کل UI یکسان بماند.

## نکته نهایی برای کدنویسی UI و Workerها

در فایل‌های UI پنل نقشه، اکشن‌های اصلی را با `Button` یا `OutlinedButton` خام نسازید. فقط از `MapPrimaryAction` و `MapSecondaryAction` استفاده کنید تا حداقل touch target، shape و spacing در کل صفحه یکسان بماند. همچنین در تغییرات WorkManager، observer مربوط به کار تمام‌شده باید از map حذف شود و coroutine با `return@launch` خارج شود؛ self-cancel کردن همان observer بعد از finished state ممنوع شده است.
