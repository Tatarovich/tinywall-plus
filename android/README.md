# Исходники TinyWall+

Java + стандартные View, без Gradle и AndroidX. Сборка вручную через `aapt2`, `javac`, `d8`,
`zipalign`, `apksigner`.

## Что нужно

- JDK 11+ (проверено на JDK 25);
- Android SDK: `platforms;android-28`, `build-tools;35.0.0`;
- `cwebp` (из пакета webp) — для сжатия пользовательских фонов.

## Сборка

```bash
KEYSTORE_PASS="пароль от ключа" SDK=/tmp/dsh-sdk bash android/build.sh
```

`build.sh` копирует проект в `/tmp/tihiy-build`, собирает и кладёт подписанный APK
`../dist/TinyWallPlus-0.10.apk` в `../dist/`.
Если SDK лежит в другом месте, задайте `SDK=/путь/к/sdk`.
Без `KEYSTORE_PASS` соберётся неподписанный APK — для проверки правок этого достаточно.

Обои уже лежат в `assets/` как WebP; сцены и двуязычные подписи —
в `assets/scenes.txt` (`id|English|Русский`). Автоматический импорт из
`../backgrounds/` в этой сборке не запускается: ассеты подготовлены вручную.
Погода использует Open-Meteo geocoding и wttr.in, нужен INTERNET.

## Подпись

Ключ `keystore.jks` в репозитории не хранится — он в `.gitignore`. Положите его рядом
с `build.sh` и передайте пароль через переменную окружения `KEYSTORE_PASS`.
Alias по умолчанию — `tihiy`; путь к ключу и alias можно переопределить
через `KEYSTORE=/путь/keystore.jks` и `KEY_ALIAS=...`.

Для обновлений нужен тот же ключ, иначе Android не поставит новую версию поверх старой.
Ключ не публикуйте: с ним кто угодно может выпустить APK, который система примет
за обновление TinyWall+.

## Структура

- `MainActivity.java` — главный экран, настройки, сетка приложений, фокус, выбор домашнего;
- `BackgroundController.java` — две ImageView, кроссфейд, медленный Ken Burns, scrim;
- `AppRepository.java` — список установленных приложений и автоподбор шести;
- `Ui.java`, `Palette.java`, `ToggleView.java` — размеры, цвета, переключатель;
- `assets/bg_*.webp` — фоны 1920×1080.
