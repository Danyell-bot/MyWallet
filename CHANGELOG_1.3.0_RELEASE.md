# MyWallet 1.3.0: Fully Native

MyWallet is now a **real Android app** written in Kotlin and Jetpack Compose, not a web page inside a WebView. It is faster, smoother, smaller on disk and more secure, and your data still stays on your phone.

> **Upgrading from 1.2.0?** Make a backup first (**Menu > Backup**), install 1.3.0 over the old app, then import that backup once. 1.2.0 backups open as they are.

---

## ✨ New

### Look and feel
- Glass-style design with a soft animated background and a floating dock
- New **Today / Money / Plan** pages
- Smooth screen and pop-up animations
- **Hide amounts** now covers every home page

### Rewards and motivation
- 16 animated streak flames, from day one up to 365 days
- Endless **level system** with 10 titles and small gaps between levels
- 14 **badges**, with a confetti celebration when you unlock one
- **Savings challenge**

### Tools
- **Monthly Rewind**: your month as animated story slides
- **Receipts gallery** with a full-size viewer
- **Recurring records** that add themselves
- **Tags** and tag search
- **Split a bill**
- **Payday** screen
- **Spending alerts** when a category runs hot
- **Monthly statement** as a PDF (Backup > Monthly statement)
- **CSV** export, share and import with a guided import wizard
- **Backup reminder** banner
- **Start fresh** option on first launch
- **Delete all my data** button
- Hidden **Developer mode** (tap the version text 7 times): time travel, sample data, flame and UI galleries

### Reminders and lock
- **Gentle reminders**: one switch, a few random nudges a day, never showing amounts
- **App lock** with a passcode, plus biometric unlock

### Accounts
- Edit, rename, ignore or delete any account
- Money tab gear button for editing, and press and hold any account to edit it

---

## 🔄 Revamped
- **Analysis** now includes Trends as a tab (the separate Trends screen is gone)
- **Menu** reorganised into Plan, Insights, Manage, and Settings and data
- **Challenge** moved under Plan
- **Badges** open from the gold award button on the profile card
- **Bank tracker** opens by tapping the bank account on the Money tab
- Level card shows your progress and the next title
- Backups are now compressed `.mwbackup` files

---

## ⚡ Speed and storage
- Receipt photos are stored as separate files, so saving is instant
- Saving runs in the background, so the screen never waits for it
- Data file is compressed, so it stays light even with thousands of records
- Cached calculations and lighter list drawing for smooth scrolling
- Animated background pauses when another screen covers it
- Photo thumbnails load quickly and use little memory
- Release build is shrunk with R8 and limited to phone chip types for a smaller APK

## 🔒 Security
- Data file and receipt photos are **encrypted** with the Android Keystore
- Backups can be **password protected** (PBKDF2 + AES-GCM)
- Passcode is **salted and slow-hashed**, and old passcodes upgrade automatically
- **Wait time after wrong tries** (5 wrong attempts, then a growing delay)
- Optional **screenshot and app-preview blocking**
- Old unencrypted data and in-file photos migrate automatically on first launch

---

## 🛠️ Fixed
- Menu scroll position is now kept when you open a screen and come back
- Backup, restore and migration paths hardened so a bad file can't damage your data
- Cleanup of orphaned receipt photos (**Backup > Clean up unused photos**)
- Photos are deleted together with their record or account

---

## ✅ Kept from 1.2.0
- Bank minimum-balance tracking and **Safe to spend**
- Bills, debts, goals, budgets, categories and quick add
- Backup and restore of records, receipts and settings

---

## 🧹 Removed
- Separate **Year** screen (Analysis already covers it)
- Separate **Streak** button in the rewards area
- Separate **Trends** screen (now inside Analysis)
- Profiles and sample-data seeding from the web version

---

## 📦 Install
Download **MyWallet-v1.3.0.apk** below and open it on your phone. Requires Android 7.0 or newer.
