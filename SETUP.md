# راهنمای راه‌اندازی و انتشار

این پروژه یک اپلیکیشن native Android با Jetpack Compose است و با Android Studio توسعه و build می‌شود.

## 1. پیش‌نیازها

- JDK 17 یا 21 (نصب‌شده روی سیستم؛ دانلود خودکار toolchain غیرفعال است)
- Android Studio با Android SDK 36
- NDK `28.2.13676358`
- CMake `3.22.1`
- Gradle Wrapper `9.5.1` (داخل پروژه pin شده است)
- Git و اتصال به Google Maven و Maven Central

فایل `local.properties` را از نمونه بسازید:

```properties
sdk.dir=C:\\Users\\YOUR_USER\\AppData\\Local\\Android\\Sdk
```

در macOS/Linux مسیر معمولاً شبیه زیر است:

```properties
sdk.dir=/home/USER/Android/Sdk
```

## 2. Clone و کنترل محیط

```bash
git clone <repository-url>
cd ProfessionalMapPro
cp local.properties.example local.properties
./scripts/verify-build-environment.sh
./scripts/verify-static.sh
```

در Windows PowerShell از `gradlew.bat` استفاده کنید.

## 3. اجرای debug

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

APK در مسیر زیر ساخته می‌شود:

```text
app/build/outputs/apk/debug/app-debug.apk
```

برای تست‌ها:

```bash
./gradlew \
  :core:model:test \
  :core:routing:test \
  :core:progress:test \
  :core:guidance:test \
  :core:service:testDebugUnitTest \
  :core:observability:testDebugUnitTest \
  :feature:map:testDebugUnitTest
```

تست دستگاه/Emulator:

```bash
./gradlew :app:connectedDebugAndroidTest \
  :core:service:connectedDebugAndroidTest \
  :core:offline:connectedDebugAndroidTest
```

برای تست متمرکز marker و navigation:

```bash
./gradlew \
  :core:routing:test \
  :core:progress:test \
  :core:service:testDebugUnitTest \
  :feature:map:testDebugUnitTest
```

چک دستی روی دستگاه واقعی:

1. در zoom پایین، clusterها را لمس کنید و بزرگ‌نمایی مرحله‌ای را بررسی کنید.
2. markerهای Office، Warehouse، Customer و Alert باید رنگ و نماد متفاوت داشته باشند.
3. marker انتخابی باید halo و هسته پرکنتراست داشته باشد و کارت جزئیات باز شود.
4. از کارت marker مسیر بسازید و مسیر جایگزین را مستقیماً از روی خط نقشه انتخاب کنید.
5. روی مسیر رفت‌وبرگشتی یا خیابان‌های موازی، snapped location نباید بدون دلیل به segment مخالف بپرد.
6. با accuracy ضعیف GPS، برنامه نباید با یک fix نویزی فوراً reroute کند.
7. در navigation mode، camera باید جلوتر از خودرو را نشان دهد و با سرعت zoom مناسب بگیرد.
8. خاموش/روشن‌کردن صفحه و خروج از صفحه نباید foreground navigation را متوقف کند.

## 4. بررسی زبان و تم برنامه

برنامه فقط دو locale اعلام می‌کند: `en` و `fa`. فایل `app/src/main/res/xml/locales_config.xml`
هر دو زبان را برای تنظیمات زبان Android معرفی می‌کند و AppCompat انتخاب کاربر را روی نسخه‌های قدیمی‌تر
Android نیز نگهداری می‌کند.

برای تست دستی:

1. برنامه را اجرا کنید و از **Menu / منو** وارد **Settings / تنظیمات** شوید.
2. زبان را بین `English` و `فارسی` عوض کنید.
3. بررسی کنید جهت کل رابط در فارسی RTL و در انگلیسی LTR شود.
4. برنامه را ببندید و دوباره باز کنید؛ زبان انتخاب‌شده باید حفظ شود.
5. تم را به‌ترتیب روی `System`، `Light` و `Dark` قرار دهید.
6. status bar، navigation bar، bottom sheet، پنل‌ها، دکمه‌ها و متن‌ها را در هر سه حالت بررسی کنید.
7. در بخش Map style، سبک `Dark / تاریک` را نیز جداگانه تست کنید.

انتخاب تم در SharedPreferences خصوصی اپ ذخیره می‌شود. حالت `System` از تنظیم روشن/تاریک دستگاه پیروی
می‌کند. تغییر زبان برنامه، زبان راهنمای صوتی را همگام می‌کند؛ کاربر بعداً می‌تواند زبان صدا را از بخش
Voice Guidance مستقل تغییر دهد.

برای کنترل resourceها:

```bash
./gradlew :app:processDebugResources :feature:map:testDebugUnitTest
```

## 5. رفع خطای Google Services plugin

پروژه دیگر plugin marker زیر را در root درخواست نمی‌کند:

```text
com.google.gms.google-services.gradle.plugin
```

Firebase Gradle plugins فقط زمانی resolve و apply می‌شوند که فایل واقعی زیر وجود داشته باشد:

```text
app/google-services.json
```

بنابراین برای build بدون Firebase، فایل template را rename نکنید و همان‌طور که هست نگه دارید.
برای فعال‌کردن Firebase:

1. اپ Android را در Firebase Console با `applicationId` درست ثبت کنید.
2. `google-services.json` را در `app/` قرار دهید.
3. فایل را commit نکنید؛ `.gitignore` باید آن را نادیده بگیرد.
4. Gradle را با `--refresh-dependencies` دوباره sync کنید.

```bash
./gradlew --stop
./gradlew :app:assembleDebug --refresh-dependencies
```

نسخه‌های تنظیم‌شده:

- Google Services Gradle plugin: `4.5.0`
- Firebase Crashlytics Gradle plugin: `3.0.7`

اگر همچنان artifact دانلود نمی‌شود، مشکل شبکه/DNS/Proxy است؛ `google()`, `mavenCentral()` و
`plugins.gradle.org` باید از سیستم شما قابل دسترسی باشند. Proxy را در `~/.gradle/gradle.properties`
تنظیم کنید و از کپی‌کردن JAR ناشناس داخل پروژه خودداری کنید.

## 6. تنظیم routing در development

مقادیر را می‌توان در `gradle.properties` کاربر یا environment قرار داد:

```properties
PMP_OSRM_BASE_URL=https://router.project-osrm.org
PMP_ROUTING_USER_AGENT=ProfessionalMapPro-Dev/1.0 (dev-team@example.com)
```

endpoint عمومی فقط برای توسعه است. درخواست‌ها شامل مختصات مسیر هستند؛ بنابراین در production باید
backend سازمانی با TLS، سیاست لاگ امن و کنترل دسترسی استفاده شود.

## 7. Firebase و telemetry

بدون `google-services.json`، Firebase monitor به `DisabledAppMonitor` تبدیل می‌شود. حتی با وجود
فایل، collection پیش‌فرض خاموش است. فعال‌سازی release:

```text
PMP_TELEMETRY_DEFAULT_ENABLED=true
```

قبل از فعال‌سازی باید رضایت/مبنای قانونی، retention، Crashlytics، Analytics، زمان‌سنجی‌های سفارشی و فرم
Data safety بررسی شوند. Firebase Performance عمداً استفاده نمی‌شود تا URLهای حاوی مختصات توسط
instrumentation خودکار شبکه جمع‌آوری نشوند. هیچ user ID یا متن آزاد حساس به monitor ندهید.

## 8. متغیرهای build

| نام | کاربرد | release |
|---|---|---|
| `PMP_OSRM_BASE_URL` | URL سرویس routing | اجباری، HTTPS و غیرعمومی |
| `PMP_ROUTING_USER_AGENT` | شناسه و راه ارتباطی client | اجباری |
| `PMP_RELEASE_STORE_FILE` | مسیر keystore | اجباری |
| `PMP_RELEASE_STORE_PASSWORD` | رمز keystore | اجباری |
| `PMP_RELEASE_KEY_ALIAS` | alias کلید | اجباری |
| `PMP_RELEASE_KEY_PASSWORD` | رمز کلید | اجباری |
| `PMP_VERSION_CODE` | versionCode مثبت | اجباری در pipeline |
| `PMP_VERSION_NAME` | versionName | اجباری در pipeline |
| `PMP_TELEMETRY_DEFAULT_ENABLED` | فعال‌سازی صریح telemetry | پیش‌فرض `false` |

secretها را در repository، `gradle.properties` مشترک یا logهای CI قرار ندهید.


## 9. تست دستگاه و performance

تست‌های unit در CI اصلی اجرا می‌شوند. تست‌های رمزگذاری Android و launch test به دستگاه یا emulator نیاز دارند. برای اجرای همه تست‌های instrumented ماژول‌ها از Android Studio یا taskهای `connectedAndroidTest` استفاده کنید.

ماژول `benchmark` یک build نزدیک به release، ولی با کلید debug محلی، می‌سازد. این ماژول دو مسیر دارد:

- `BaselineProfileGenerator`: مسیر cold start را ثبت می‌کند تا پروفایل دستی `app/src/main/baseline-prof.txt` بازبینی و به‌روزرسانی شود.
- `StartupBenchmark`: startup و frame timing را در حالت بدون compilation و با Baseline Profile مقایسه می‌کند.

اجرای تکرارپذیر روی Gradle Managed Device API 33:

```bash
./gradlew :benchmark:pixel6Api33BenchmarkAndroidTest --stacktrace
```

برای مقایسه startup بدون compilation و با Baseline Profile، خروجی testهای `StartupBenchmark` را جدا نگه دارید.
پس از تغییرهای مهم UI، MapLibre، startup یا dependencyها، generator را اجرا و profile تولیدشده را با
`app/src/main/baseline-prof.txt` مقایسه کنید؛ پروفایل را بدون اندازه‌گیری و بازبینی دستی جایگزین نکنید.

چک performance قبل از release:

- cold start و frame timing روی یک دستگاه ضعیف و یک دستگاه میان‌رده
- اسکرول پنل کنترل با font scale بزرگ
- حرکت دوربین و update مسیر هنگام GPS با نرخ بالا
- انتخاب cluster/marker با مجموعه POI بزرگ
- تم روشن و تاریک با GPU overdraw و jank قابل قبول

برای تصمیم release، عدد emulator کافی نیست. نتیجه نهایی باید روی حداقل یک دستگاه واقعی ضعیف و یک دستگاه میان‌رده ثبت شود. workflow زمان‌بندی‌شده `.github/workflows/performance.yml` برای کشف regression است و artifactهای benchmark را نگه می‌دارد.

## 10. ساخت release

نمونه PowerShell:

```powershell
$env:PMP_OSRM_BASE_URL="https://routing.company.example"
$env:PMP_ROUTING_USER_AGENT="ProfessionalMapPro/1.0 (mobile-team@company.example)"
$env:PMP_RELEASE_STORE_FILE="D:\\keys\\professional-map.jks"
$env:PMP_RELEASE_STORE_PASSWORD="..."
$env:PMP_RELEASE_KEY_ALIAS="professional-map"
$env:PMP_RELEASE_KEY_PASSWORD="..."
$env:PMP_VERSION_CODE="1"
$env:PMP_VERSION_NAME="1.0.0"
$env:PMP_TELEMETRY_DEFAULT_ENABLED="false"
.\gradlew.bat :app:bundleRelease :app:lintRelease
```

خروجی AAB:

```text
app/build/outputs/bundle/release/app-release.aab
```

Build release در صورت نبود signing یا استفاده از endpoint عمومی routing عمداً fail می‌شود.

## 11. انتشار Google Play

1. AAB را با Play App Signing بارگذاری کنید.
2. mapping file مربوط به R8 را نگه دارید.
3. permissionهای location و foreground service را در declarationهای Play Console توضیح دهید.
4. Data safety را با Firebase، routing backend، tile/style provider و logهای server تطبیق دهید.
5. تست internal track را روی Android 12 تا 16 و چند سازنده انجام دهید.

## 12. Debug و لاگ

- از Android Studio Logcat با package debug استفاده کنید.
- متن exception شبکه را مستقیماً log نکنید.
- bounds و Style URL دانلود آفلاین در WorkManager فقط به‌صورت ciphertext ذخیره می‌شوند؛ key یا payload را log نکنید.
- مختصات، route URL، bearer token، API key و session payload نباید در log ثبت شوند.
- Crashlytics/Analytics در debug و در buildهای بدون consent خاموش هستند.
- برای memory leak، dependency مربوط به LeakCanary فقط در debug وجود دارد.

## 13. مشکلات رایج

### Gradle distribution دانلود نمی‌شود

DNS، proxy و دسترسی به `services.gradle.org` را بررسی کنید. wrapper با checksum pin شده است؛ نسخه
یا JAR wrapper را از منبع نامعتبر جایگزین نکنید.

### نقشه سفید است

اتصال به style/tile provider، TLS، attribution و محدودیت شبکه را بررسی کنید. UI پس از خطای load
دکمه تلاش مجدد نشان می‌دهد و MapView state/lifecycle/low-memory را مدیریت می‌کند.

### ناوبری پس‌زمینه متوقف می‌شود

مجوز location، روشن‌بودن Location Services، محدودیت باتری سازنده و اعلان foreground service را
بررسی کنید. session رمزگذاری‌شده قابل restore است، ولی Force stop توسط کاربر قابل دورزدن نیست.

### Release به دلیل routing fail می‌شود

`PMP_OSRM_BASE_URL` را به backend production HTTPS تغییر دهید و User-Agent اختصاصی بدهید.

## 14. چک‌لیست قبل از release

- [ ] static checks، unit tests، lint و release gate سبز هستند
- [ ] تست Emulator و حداقل دو دستگاه واقعی انجام شده است
- [ ] ناوبری با خاموش‌شدن صفحه، rotation و نابودی Activity تست شده است
- [ ] process recreation و restore session بررسی شده است
- [ ] Android 12 تا 16 و مجوز notification/location بررسی شده‌اند
- [ ] تغییر زبان English/فارسی، حفظ locale پس از restart و RTL/LTR کامل بررسی شده‌اند
- [ ] تم System/Light/Dark، حفظ انتخاب پس از restart و کنتراست system barها بررسی شده‌اند
- [ ] RTL، font scaling، TalkBack و touch targetها بررسی شده‌اند
- [ ] routing، tile/style attribution و offline download تحت بار تست شده‌اند
- [ ] telemetry و Data safety تأیید شده‌اند
- [ ] keystore، نسخه، mapping و checksum آرشیو شده‌اند
