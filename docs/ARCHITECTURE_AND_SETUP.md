# SECURE STUDY — Architecture, Security & Deployment Guide

## 1. Platform Architecture
This Android application implements **SECURE STUDY** natively in **Kotlin + Jetpack Compose** with:
- **Yellow Liquid UI**: Custom dark theme (`#101010` / `#1C1C1C`), Bright Liquid Yellow (`#FFD600`) & Golden Yellow (`#FFC400`), local bundled `Outfit`, `Plus Jakarta Sans`, and `JetBrains Mono` fonts, animated morphing Canvas waves, and adaptive `WindowSizeClass` navigation (`NavigationBar` / `NavigationRail`).
- **Authentication**: Google Sign-In via Jetpack `CredentialManager` (`GetSignInWithGoogleOption` + `GetGoogleIdOption`), paired with Firebase Authentication and email verification status checks.
- **Cloud Database & Zero-Trust Rules**: Cloud Firestore (`firebase_applet_config.xml`) with hardened `firestore.rules` enforcing:
  - Per-user isolation on `users`, `subscriptions`, `purchases`, and `pdf_access_logs`.
  - Server-side entitlement gating on `pdf_payloads/{docId}` (`hasActiveSubscription(request.auth.uid)` and active student status check).
  - Admin-only writes on `categories`, `pdf_documents`, `pdf_payloads`, `subscription_plans`, `subscriptions`, and `audit_logs`.
- **Native Biometric Vault**: AndroidX `BiometricPrompt` with hardware & enrollment checks (`BIOMETRIC_STRONG`, `BIOMETRIC_WEAK`, `DEVICE_CREDENTIAL`), configurable inactivity auto-lock, and zero cloud transmission of biometric templates.
- **Protected In-App PDF Reader**:
  - Android `PdfRenderer` inside Compose with pinch/pan/double-tap zoom, page scrubber, reading progress persistence, and in-document keyword search.
  - `WindowManager.LayoutParams.FLAG_SECURE` enabled while the reader is active to block standard screenshots, screen recording, and recent-apps previews (note: no software can prevent external physical cameras, so a dynamic repeating account watermark is overlaid on every page).
  - `EncryptedPdfCacheManager` encrypts temporary PDFs with AES-256-GCM via Android Keystore and zeroes + deletes transient session files on close or background (`ON_STOP`).
- **Subscription & Payments**:
  - Google Play Billing Library 7 (`PlayBillingManager`) for localized product prices, native billing flow, purchase acknowledgement, and Restore Purchases.
  - PhonePe UPI QR checkout (`phonepe_upi_qr.jpg`) with UTR submission into `purchases` (`pending_verification`) for admin verification.
  - Zero client-side premium unlocking.

## 2. Google Play Console Setup
1. Create an application with package name `com.aistudio.securestudy.vxqmrk`.
2. In **Monetize -> Subscriptions**, create subscription products matching the Admin Console IDs:
   - `secure_study_monthly` (Monthly)
   - `secure_study_quarterly` (Quarterly)
   - `secure_study_yearly` (Yearly)
3. Upload an internal testing AAB (`gradle :app:bundleRelease` or Android Studio export) and configure License Testers in Google Play Console to test subscriptions and pending/cancelled/restored states.

## 3. Testing
- **Firestore Security Rules (JS)**: `FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js`
- **Kotlin Repository & Rules Integration (Robolectric)**: `gradle :app:testDebugUnitTest --tests '*Rule*'`
