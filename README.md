# Professional Map Pro

![Static verification](https://img.shields.io/badge/static%20checks-passing-blue)
![Platform](https://img.shields.io/badge/platform-Android-green)
![License](https://img.shields.io/badge/license-AGPL--3.0-orange)

Professional Map Pro یک اپلیکیشن native **Android** برای نمایش نقشه، دریافت موقعیت زنده،
مسیریابی، ناوبری پس‌زمینه، راهنمای صوتی و بسته‌های آفلاین است. رابط کاربری آن با Jetpack Compose
ساخته شده و MapLibre مسئول رندر نقشه است. تنها پلتفرم و target پروژه Android است.

## ویژگی‌های اصلی

- نمایش نقشه و چند style مبتنی بر MapLibre با رندر تفاضلی GeoJSON
- مارکرهای دسته‌بندی‌شده با رنگ، برچسب، clustering، انتخاب لمسی، کارت جزئیات و مقصدگذاری مستقیم
- نمایش مبدا، مقصد، نقاط مانور، موقعیت خام/منطبق‌شده و مسیر طی‌شده/باقی‌مانده در لایه‌های مستقل
- مسیریابی با backend سازگار با OSRM، مسیرهای جایگزین قابل انتخاب از روی نقشه و snap مبتنی بر bearing/accuracy
- ناوبری مستقل از UI داخل foreground service با GPS، progress تطبیقی، camera look-ahead و reroute ضدنوسان
- ذخیره رمزگذاری‌شده session ناوبری با Android Keystore و بدون ذخیره موقعیت زنده
- دانلود و مدیریت ناحیه‌های آفلاین با WorkManager، payload رمزگذاری‌شده و بدون polling یک‌ثانیه‌ای
- انتخاب زبان فارسی و انگلیسی از داخل منو با تغییر کامل RTL/LTR و ذخیره انتخاب کاربر
- تم حرفه‌ای سیستم، روشن و تاریک با رنگ‌بندی کامل Material 3 و حفظ تنظیم بعد از اجرای مجدد
- سبک تاریک مستقل برای خود نقشه، در کنار styleهای روشن موجود
- layout تطبیقی، کنترل‌های قابل دسترس و پشتیبانی از font scaling
- رابط بصری بازطراحی‌شده با dock متحرک، cockpit اختصاصی رانندگی، کنترل دنبال‌کردن موقعیت، کارت‌های وضعیت و پالت حرفه‌ای روشن/تاریک
- بهینه‌سازی مسیر رندر با scene memoization، مدل‌های پایدار Compose، callbackهای memoized، source cache بدون allocation تکراری، layer reuse و پنل‌های lazy

## تکنولوژی‌ها

Kotlin، Jetpack Compose، Material 3، StateFlow، Coroutines، MapLibre Native، Ktor،
Google Play services Location، WorkManager، Android Keystore و Firebase اختیاری.

## ساختار سریع

- `app`: ورودی اپ، theme، تنظیمات release و observability
- `feature:map`: UI، ViewModel و orchestration صفحه نقشه
- `core:service`: runtime مستقل ناوبری و foreground service
- `core:routing`: قرارداد و adapter مسیریابی
- `core:location`, `core:progress`, `core:offline`: زیرسیستم‌های موقعیت، پیشرفت و آفلاین
- `core:observability`: مرز telemetry و پاک‌سازی داده حساس
- `benchmark`: Macrobenchmark راه‌اندازی سرد و مولد Baseline Profile روی API 33

جزئیات لایه‌ها و dependencyها در [ARCHITECTURE.md](ARCHITECTURE.md) آمده است.


## مارکرها و تجربه ناوبری

POIها در یک source خوشه‌بندی‌شده نگهداری می‌شوند تا با افزایش تعداد نقاط، به‌جای ساخت viewهای متعدد،
رندر داخل موتور نقشه باقی بماند. رنگ و نماد marker از category می‌آید، marker انتخاب‌شده هسته و halo
مستقل دارد، لمس cluster به zoom مناسب می‌رود و لمس marker کارت اطلاعات دو‌زبانه با عملیات مسیریابی را
باز می‌کند. یک hit target عریض و نامرئی نیز انتخاب مسیرهای جایگزین را روی صفحه کوچک قابل اعتماد می‌کند.

در ناوبری، انتخاب segment فقط براساس نزدیک‌ترین فاصله نیست. جهت حرکت، accuracy GPS و پیشرفت قبلی
در امتیازدهی دخالت دارند تا روی خیابان‌های موازی یا مسیر رفت‌وبرگشتی پرش کمتری رخ دهد. آستانه off-route
با دقت GPS تطبیق پیدا می‌کند و reroute پس از شواهد تکرارشونده، debounce و cooldown انجام می‌شود.
دوربین navigation کمی جلوتر از خودرو را هدف می‌گیرد و zoom، bearing و tilt را با سرعت و مسیر تنظیم می‌کند.

## زبان و ظاهر

از دکمه **Menu / منو** وارد بخش **Settings / تنظیمات** شوید. در این بخش می‌توان زبان برنامه را بین `English` و `فارسی` تغییر داد و حالت نمایش را روی `System`، `Light` یا `Dark` گذاشت. تغییر زبان همه متن‌های صفحه و جهت چیدمان را به‌روزرسانی می‌کند. انتخاب زبان توسط AppCompat و انتخاب تم در SharedPreferences خصوصی برنامه ذخیره می‌شود. زبان راهنمای صوتی هنگام تغییر زبان برنامه همگام می‌شود، اما همچنان از بخش Voice Guidance قابل تغییر مستقل است.

## ظاهر و کارایی

رابط نقشه از یک design system مشترک برای shape، فاصله، elevation، رنگ وضعیت و actionها استفاده می‌کند.
کنترل‌های اصلی داخل dock شناور آیکن‌دار قرار دارند و تغییر حالت آن‌ها با motion کوتاه و کنترل‌شده دیده
می‌شود. هنگام ناوبری، HUD عمومی با cockpit رانندگی جایگزین می‌شود تا دستور بعدی، فاصله مانور، زمان و
درصد پیشرفت در یک نگاه خوانده شوند. کنترل مستقل دنبال‌کردن موقعیت، کارت POI متحرک و برچسب پرکنتراست
marker انتخاب‌شده نیز بدون ورود به منو در دسترس‌اند. رنگ‌ها فقط تزئینی نیستند؛ وضعیت فعال، هشدار، خطا،
مبدا، مقصد، مسیر و انتخاب marker semantic مستقل دارند.

برای کم‌کردن jank، `MapScene` فقط با تغییر داده‌های مرتبط با نقشه دوباره ساخته می‌شود. `MapLibreView`
به‌جای restart کردن effect برای هر state، آخرین scene را با `snapshotFlow` جمع‌آوری می‌کند. sourceهای GeoJSON
به‌صورت async و فقط هنگام تغییر داده به‌روزرسانی می‌شوند، layerها برای هر style یک بار آماده می‌شوند و پنل
بزرگ کنترل‌ها با `LazyColumn` فقط بخش‌های موردنیاز را compose می‌کند. dock پایین به‌جای دریافت کل
`MapUiState` فقط booleanهای موردنیاز را می‌گیرد و callbackهای منو/لمس نقشه memoize شده‌اند؛ بنابراین
آپدیت‌های پرتعداد GPS بخش‌های ثابت UI را بی‌دلیل وارد recomposition نمی‌کنند. endpoint و route alternative
cache نیز بدون ساخت `List` و `Pair` موقت در هر frame بررسی می‌شود. دوربین navigation updateهای بیش‌ازحد
نزدیک را محدود می‌کند.

## نصب و اجرا

پیش‌نیازها، تنظیم Android SDK، buildهای debug/release، signing، متغیرهای محیطی، Firebase و
رفع خطاهای رایج در [SETUP.md](SETUP.md) توضیح داده شده‌اند.

برای کنترل اولیه پروژه:

```bash
./scripts/verify-static.sh
./scripts/verify-test-suite.sh
./scripts/verify-build-environment.sh
./gradlew :app:assembleDebug
# روی دستگاه یا Gradle Managed Device:
./gradlew :benchmark:pixel6Api33BenchmarkAndroidTest
```

## حریم خصوصی و telemetry

- پیام‌های خام Ktor، URL مسیریابی، token و مختصات دقیق وارد UI یا telemetry نمی‌شوند.
- session ناوبری به‌صورت AES/GCM در `noBackupFilesDir` نگهداری می‌شود.
- محدوده، Style URL و تنظیمات دانلود آفلاین پیش از ورود به دیتابیس WorkManager با Android Keystore رمز می‌شوند.
- Firebase تنها با `app/google-services.json` واقعی فعال می‌شود.
- collection در debug خاموش است و در release نیز پیش‌فرض خاموش می‌ماند؛ فعال‌سازی نیازمند
  `PMP_TELEMETRY_DEFAULT_ENABLED=true` و تصمیم صریح تیم محصول/حریم خصوصی است.
- قبل از انتشار، فرم Data safety فروشگاه باید با رفتار واقعی build نهایی تطبیق داده شود.

## محدودیت‌های عملیاتی

endpoint عمومی `router.project-osrm.org` فقط برای توسعه محلی است و build release آن را رد می‌کند.
در production باید سرویس مسیریابی تحت کنترل سازمان، محدودیت نرخ، monitoring و سیاست نگهداری
لاگ مشخص داشته باشد. style و tile provider نیز باید مجوز استفاده production و attribution معتبر
داشته باشند.

## License

This project is licensed under the GNU Affero General Public License v3.0 (AGPL-3.0) - see the
[LICENSE.md](LICENSE.md) file for details.

### What this means

- You can use, study, modify, and distribute the software under the license terms.
- Distributed modified works must preserve notices and use the same license where required.
- Network use of a modified version triggers the corresponding-source obligations in AGPL section 13.
- The license includes no warranty; review it with legal counsel before commercial distribution.
