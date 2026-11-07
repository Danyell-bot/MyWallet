# Changelog

All notable changes to MyWallet are listed here, newest first.

## v1.1.0 (2026-11-07)

### Added
- **Streak flames that change with your streak.** Eight different flame icons: Cold, Spark, Flame, Blaze, Blue flame, Violet flame, Rose flame and Legend (0, 1, 3, 7, 14, 30, 60 and 100+ days). The flame goes grey when you haven't logged today.
- **Streak reminder notification.** With a streak of 2 days or more, the daily reminder says "Your N-day streak ends tonight" if you haven't logged yet. It goes away once you log something. It can be switched off in Reminders.
- **Debts on Home.** Open debts now show on Home with the owed-to-you and you-owe totals, the 3 most urgent debts, and an overdue count. It only appears when you have a debt.
- **E-wallet account type** (GCash, Maya and similar).
- **Brand-style account cards** for common banks and wallets (Landbank, BDO, BPI, GCash, Maribank, GoTyme and more). The colors are approximations, not official logos.
- **Calculator button inside the Amount field.** The calculator only opens when you press it. Tapping the field or empty space goes back to the normal keyboard.
- **Full-screen receipt viewer.** Tap a receipt photo to open the whole image, with tap-to-zoom.
- **Welcome screen and CSV import** for first-time setup.
- **Auto-lock timeout** with a cleaner App Lock screen.
- **Debt due dates** with overdue warnings and reminders.
- **Receipts gallery** to browse all receipt photos.
- **Backup reminder** banner on Home.
- **Subscriptions summary** and **learned categories** (the app remembers which category you pick for a note).
- **Calendar view** for transactions.
- **Bank tools:** fees avoided, danger calendar for low-balance days, and payday set-aside.
- **Streak freeze and levels.** One missed day per week won't break your streak.
- **Year in review** recap and **PDF statement** export.
- **New app icon and splash screen.**
- **Soft notification sound** (a gentle chime instead of the loud default).
- **Developer mode.** Tap "MyWallet v1.1.0" in Menu 7 times to unlock it. Tools include:
  - time travel;
  - fake streak and level values;
  - set any account balance;
  - add sample records;
  - unlock or reset all badges;
  - add or clear sample debts;
  - delete all records;
  - fake an old backup;
  - lock the app now;
  - run recurring records now;
  - a reminder preview;
  - a storage summary;
  - an editor for all app data (JSON).

### Fixed
- Calendar overflowing the screen, and day letters not lining up with the dates.
- Bank screen layout problems and a label that was styled as a pale bar.
- Calculator keypad opening by itself and bleeding into the rest of the screen.
- "Unlock all badges" in developer mode did nothing.
- Restoring a cut-off backup now says the backup is incomplete instead of just "invalid".
- Various small layout and logic fixes found in a full overflow check at 320, 360 and 412 px widths, in light and dark mode.

### Notes
- This is the WebView build: the app is a single `index.html` shown inside an Android WebView, with native Kotlin for reminders, the widget and printing.
- When updating, sign the APK with the same keystore as v1.0.0 so it installs over the old version.

## v1.0.0 (pre-release)

- First release: money tracking with accounts, records, categories, budgets, goals, bills and recurring records.
- Bank minimum-balance tracking with a safe-to-spend amount.
- Debts and split bills, savings challenge, badges and streaks.
- Reminders, home-screen widget, app lock with passcode and fingerprint, backup and restore.
