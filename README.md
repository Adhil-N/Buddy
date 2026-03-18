# Buddy 📱

Buddy is a modern, secure, and privacy-focused Android companion app designed to help you manage your digital and financial life all in one place. Built entirely with Kotlin and Jetpack Compose.

## ✨ Features
* **At-a-Glance Dashboard:** A live-updating home screen summarizing your pending finances, saved notes, and secured passwords.
* **Financial Tracker:** Keep track of pending loans, EMIs, and debts with visual progress indicators.
* **Secure Vault:** A localized password manager to keep your credentials safe.
* **Smart Notes:** Quickly jot down ideas with full edit, delete, and undo capabilities.
* **Invisible Cloud Sync:** Securely and silently backs up your Room database directly to your personal Google Drive (AppData folder) using the Google Drive REST API.
* **Smart Merge Technology:** Restoring from the cloud seamlessly merges your backed-up data with your current device data without destructive overwriting.
* **Secure Authentication:** One-tap Google Sign-In with encrypted local session management via DataStore.

## 🛠 Tech Stack
* **UI:** Jetpack Compose, Material Design 3
* **Language:** Kotlin
* **Local Storage:** Room Database (SQLite), Preferences DataStore
* **Asynchrony:** Coroutines & Kotlin Flows
* **Cloud & Auth:** Google Drive REST API, Google Identity Services, Google Account Credential
* **Architecture:** MVVM (Model-View-ViewModel)

## 🚀 Getting Started
To clone and run this project locally, you will need to supply your own Google Client ID.
1. Clone the repository.
2. Create a project in the Google Cloud Console and enable the **Google Drive API**.
3. Generate an OAuth 2.0 Client ID for Android.
4. Add a `local.properties` file to the root of the project and add your key:
   `GOOGLE_CLIENT_ID=your_client_id_here.apps.googleusercontent.com`
5. Build and run in Android Studio!
