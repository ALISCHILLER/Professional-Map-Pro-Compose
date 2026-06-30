# راهنمای Release و Deploy پروژه ProfessionalMapPro

این فایل چک‌لیست release برای پروژه Android/Kotlin است. تا وقتی build واقعی، test matrix، lint و تست device انجام نشده باشد، نباید خروجی را production-ready قطعی فرض کرد.

- قبل از release، علاوه بر assemble/test واقعی، guardrail جدید WorkManager را هم پاس کنید؛ finished WorkInfo نباید در polling دائمی دوباره پردازش شود.

## شرایط لازم قبل از release

قبل از ساخت release، همه موارد زیر باید برقرار باشد:

- `gradle/wrapper/gradle-wrapper.jar` تولید و commit شده باشد.
- `./scripts/verify-build-environment.sh` پاس شود.
- `./scripts/verify-static.sh` پاس شود.
- `./scripts/verify-architecture.sh` پاس شود.
- `./scripts/verify-test-suite.sh` پاس شود.
- full Gradle test matrix پاس شود.
- lint برای ماژول‌های Android پاس شود.
- مسیر routing production و providerهای map/tile از نظر SLA، quota، privacy و license تأیید شده باشند.
- تست real device برای GPS، offline map، foreground service و notification انجام شده باشد.

## Build production

ابتدا environment را بررسی کنید:

```bash
./scripts/verify-build-environment.sh
./scripts/verify-static.sh
./scripts/verify-architecture.sh
./scripts/verify-test-suite.sh
```

سپس تست‌ها:

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

Lint:

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

ساخت release APK:

```bash
./gradlew :app:assembleRelease --stacktrace
```

اگر برای انتشار Play Store نیاز به AAB دارید:

```bash
./gradlew :app:bundleRelease --stacktrace
```

## تنظیم signing

Release signing از Gradle properties یا environment variables خوانده می‌شود:

```text
PMP_RELEASE_STORE_FILE
PMP_RELEASE_STORE_PASSWORD
PMP_RELEASE_KEY_ALIAS
PMP_RELEASE_KEY_PASSWORD
```

نمونه اجرای release با env:

```bash
PMP_RELEASE_STORE_FILE="/secure/path/release.jks" \
PMP_RELEASE_STORE_PASSWORD="***" \
PMP_RELEASE_KEY_ALIAS="release" \
PMP_RELEASE_KEY_PASSWORD="***" \
./gradlew :app:assembleRelease --stacktrace
```

فایل‌های keystore، `.env`، `secrets.properties`، `release-signing.properties` و `app/google-services.json` واقعی نباید commit شوند.

## تنظیم routing و providerها برای production

برای production، از endpoint دارای مالکیت یا قرارداد استفاده کنید:

```bash
PMP_OSRM_BASE_URL="https://routing.your-company.example" \
PMP_ROUTING_USER_AGENT="ProfessionalMapPro/1.0 production" \
./gradlew :app:assembleRelease --stacktrace
```

قبل از release بررسی کنید:

- endpoint فقط demo/public بدون SLA نباشد.
- HTTPS فعال باشد.
- cleartext traffic برای release لازم نباشد.
- rate limit و quota کافی باشد.
- policy مربوط به location/privacy تایید شده باشد.
- tile/map provider اجازه استفاده offline و caching داشته باشد.

## Firebase / Crashlytics

اگر release به Firebase نیاز دارد:

1. فایل واقعی `app/google-services.json` را فقط در محیط CI یا سیستم release قرار دهید.
2. مطمئن شوید فایل واقعی وارد repository نمی‌شود.
3. قوانین data collection و consent داخلی سازمان را بررسی کنید.
4. اگر minify فعال است، mapping upload را در pipeline release بررسی کنید.

اگر `google-services.json` وجود نداشته باشد، پلاگین‌های Firebase apply نمی‌شوند و build بدون Firebase ادامه می‌دهد.

## Database migration

این پروژه backend و دیتابیس application-level ندارد و migrationهایی مثل Flyway/Liquibase در آن دیده نمی‌شود. Offline map storage توسط MapLibre/Android مدیریت می‌شود. اگر در آینده storage داخلی اضافه شد، migrationها باید backward-compatible و همراه rollback plan باشند.

## Security checklist

قبل از انتشار:

- [ ] `android:allowBackup="false"` حفظ شده است.
- [ ] `android:usesCleartextTraffic="false"` حفظ شده است.
- [ ] notification lock-screen اطلاعات مسیر را public نمایش نمی‌دهد.
- [ ] release signing از env/secret store می‌آید.
- [ ] secret در log، docs، source و artifact وجود ندارد.
- [ ] routing provider production از HTTPS استفاده می‌کند.
- [ ] Firebase و analytics با policy سازمان هماهنگ است.
- [ ] location permission، foreground service و notification permission روی Androidهای هدف تست شده‌اند.
- [ ] ProGuard/R8 خروجی crash-free smoke test شده است.

## Device validation checklist

روی device واقعی تست کنید:

- start/stop navigation
- reroute هنگام قطع اینترنت یا failure provider
- preview-only fallback و غیرفعال بودن start navigation برای آن
- GPS drift و off-route detection
- arrival detection
- notification pause/resume/stop
- lock-screen notification privacy
- voice guidance فارسی و انگلیسی
- offline download، progress، cancel، resume و delete
- app process death در حین offline download
- battery saver و background restrictions
- Android 13+ notification permission

## خروجی‌های release

بعد از build موفق، مسیرهای معمول خروجی:

```text
app/build/outputs/apk/release/*.apk
app/build/outputs/bundle/release/*.aab
app/build/outputs/mapping/release/mapping.txt
app/build/reports/lint-results-*.html
```

هیچ‌کدام از خروجی‌های build نباید داخل release source ZIP commit شوند.

## Rollback plan

برای rollback:

1. versionCode/versionName release قبلی را مشخص کنید.
2. artifact قبلی و mapping file همان نسخه را نگه دارید.
3. اگر routing endpoint جدید باعث مشکل شده، endpoint را از configuration pipeline به مقدار قبلی برگردانید و rebuild کنید.
4. اگر Crashlytics افزایش crash نشان داد، انتشار phased rollout را متوقف کنید.
5. اگر مشکل مربوط به provider/tile/offline license است، release را از کانال انتشار خارج کنید تا provider production اصلاح شود.

## Release blockers

در این شرایط release نکنید:

- `gradle/wrapper/gradle-wrapper.jar` وجود ندارد.
- full Gradle build/test/lint اجرا و پاس نشده است.
- signing release تنظیم نشده است.
- routing production endpoint مشخص نیست.
- public/demo provider بدون SLA استفاده شده است.
- notification privacy روی lock screen تست نشده است.
- offline download روی device واقعی fail می‌شود.
- foreground service از notification قابل کنترل نیست.
- secret واقعی داخل repository یا artifact دیده می‌شود.

## UI Release Checklist

در build نهایی، UI باید روی موبایل کوچک، landscape، tablet و حالت RTL/Persian چک شود. HUD باید quick actionهای permission/GPS، route، navigation و offline را بدون باز کردن پنل‌های عمیق نشان دهد؛ کنترل‌های جزئی باید داخل section cardهای scroll-safe باقی بمانند.

قبل از انتشار، صفحه نقشه را در Light/Dark/Dynamic Color، فارسی RTL و انگلیسی LTR، با permission denied/approximate/precise، مسیر فعال، reroute، offline download و حالت خطا بررسی کنید. پنل پایین نباید کل نقشه را قفل کند و همه actionهای اصلی باید touch target واضح و متن قابل فهم داشته باشند.

## چک UI قبل از Release

قبل از Release، حالت‌های compact و expanded را بررسی کنید: HUD باید خوانا باشد، `MapRouteFocusCard` روی مسیر فعال درست نمایش داده شود، پنل پایین روی موبایل scroll-safe باشد، و side panel روی صفحه‌های بزرگ‌تر از 900dp بدون پوشاندن بیش از حد نقشه کار کند.
- UI release check: در موبایل bottom control panel، در tablet/desktop side panel، HUD، status strip، Route Focus Card، Quick Actions، حداقل touch targetها و RTL/LTR را قبل از انتشار دستی بررسی کنید.

## کنترل نهایی UI قبل از Release

قبل از release، `scripts/verify-static.sh` باید guardrailهای UI را پاس کند. این guardrailها وجود `MapControlPanelHeader`، `MapSignalBanner`، `MapQuickActionBar`، tokenهای adaptive layout و ممنوعیت برگشت اکشن‌های footer به Button خام را بررسی می‌کنند.

## کنترل نهایی کد قبل از Release

قبل از release، علاوه بر build و lint، اجرای `scripts/verify-static.sh` باید تأیید کند که اکشن‌های UI فقط از سیستم مشترک `MapQuickActions` استفاده می‌کنند و observerهای WorkManager پس از finished state خودشان را self-cancel نمی‌کنند. این دو مورد برای پایداری UX و جلوگیری از رفتارهای غیرمنتظره در teardown/cleanup مهم هستند.
