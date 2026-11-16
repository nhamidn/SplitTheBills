# App languages

Split the Bills supports English, French, and Spanish. It follows the device language by
default, with English as the fallback for other languages.

To choose a language just for this app, open **Profile → Language** and select **English**,
**Français**, **Español**, or **System default**. In French, the path is **Profil → Langue**;
in Spanish, **Perfil → Idioma**. The choice is saved on the device and applies to all accounts.
Android 13 and newer also expose the same choice under **Settings → Apps → Split the Bills → Language**.

Navigation, forms, dialogs, reminders, activity/notification labels, validation messages,
and accessibility descriptions are translated. Participant names, group names, expense
titles, and notes remain the text their authors entered. Reminder drafts use the selected
language and remain editable before sharing.

Amounts and dates follow the app language. Amount and percentage fields accept `12.34`
or `12,34`; do not use thousands separators in input fields. Financial calculations still
use integer minor units, and API dates remain `yyyy-MM-dd`. CSV exports keep their stable
column names, decimal points, and ISO dates for compatibility with spreadsheets and imports.

## Maintaining translations

- `app/src/main/res/values/strings.xml` contains the English defaults.
- `values-fr/strings.xml` and `values-es/strings.xml` contain matching translations.
- `res/xml/locales_config.xml` declares supported app languages. AppCompat stores the choice
  on older Android versions and synchronizes with the system setting on newer versions.
- `ui/UiStrings.kt` resolves resources and formats amounts, dates, enum labels, and shared text.
  Keep filter values, backend event types, currency codes, and navigation tokens independent
  of their translated labels.
- `ui/UiText.kt` keeps messages as resource IDs and arguments until displayed, so a pending
  message also changes language after activity recreation. Resolve Android system prompt
  text directly; map server/SDK errors to translated messages instead of displaying raw errors.
- Use positional placeholders such as `%1$s` and Android plurals. French and Spanish include
  the `many` quantity required for counts such as one million.

Verify resource coverage, format arguments, and comma decimal inputs with unit tests, then
exercise all screens, reminders, language changes, dark mode, and 200% text on a device:

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.dts.splitthebill.LocalizationTest,com.dts.splitthebill.AppLanguageTest
```

The device tests restore the original app language. Layout previews are saved on the device
under `Android/data/com.dts.splitthebill/files/localization-previews/`.

Android's [per-app language guidance](https://developer.android.com/guide/topics/resources/app-languages)
explains the platform and AppCompat behavior.
