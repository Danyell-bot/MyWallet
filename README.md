# MyWallet

A simple, private money tracker for Android. Everything stays on your phone: no account, no ads, no sync.

## Features
- Records for expenses, income and transfers, with a built-in calculator keypad
- Cash, card and bank accounts, with **bank minimum-balance tracking** (average daily, daily minimum or month-end rules), a "what if" tool, fees-avoided counter and a day-by-day danger calendar
- Safe-to-spend, payday allowance (with a set-aside percentage), budgets, goals and bills
- Debts with due dates, recurring records and a subscriptions summary
- Receipt photos (camera or gallery) with a receipts gallery
- Calendar view of your records, monthly recap, year in review and a printable PDF statement
- Badges, levels and streaks (with a weekly streak freeze)
- App lock (passcode + fingerprint) with an auto-lock timer
- CSV import/export and JSON backup/restore
- Soft daily reminders for bills, debts and bank top-ups

## How it is built
The interface is a single HTML/JS file (`app/src/main/assets/index.html`) shown in an Android WebView.
Kotlin handles the native parts: fingerprint, camera/gallery, file saving, notifications, home-screen widget and printing.

## Build
1. Open the project in Android Studio.
2. Put `index.html` in `app/src/main/assets/` and the Kotlin files in `app/src/main/java/com/example/mywallet/`.
3. Run on a device or emulator.

## Privacy
No network access is needed. Your data is stored in the app's local storage on your device.
Use **Menu → Backup** to save a copy.

## License
MIT. See [LICENSE](LICENSE).
