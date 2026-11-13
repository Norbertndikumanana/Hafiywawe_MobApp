# HAFIYAWE — Android App

The native Android client for **HAFIYAWE**, a second-hand marketplace for Rwanda. Buyers browse
verified listings and contact sellers; sellers publish items with photos, pay for posting slots
and chat with buyers; administrators approve listings, verify mobile-money payments and handle
support.

This repository contains the **mobile app only**. It talks to the HAFIYAWE Express API over HTTP
and keeps a full offline copy of everything it fetches, so the marketplace, chats and
notifications stay readable with no connection.

---

## Languages and technologies

### Languages

| Language | Where it is used | Share of the codebase |
| --- | --- | --- |
| **Kotlin** | Everything: UI, navigation, state, repositories, database, networking | ~95% of source lines |
| **XML** | `AndroidManifest.xml`, strings/colours/themes, network security config, launcher icon | resources |
| **Gradle Kotlin DSL** (`.kts`) | Build scripts (`settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`) | build config |
| **SQL** | Room DAO queries (ranked feed, unread counts, filtering, search) | inside DAOs |
| **JSON** | API request/response payloads, serialized with kotlinx.serialization | wire format |

No Java, no JavaScript, no cross-platform framework — this is a pure native Android app.

### Libraries and tools

| Tool | Version / role |
| --- | --- |
| Jetpack Compose + Material 3 | Declarative UI, light and dark themes |
| Navigation Compose | Screen graph, bottom bar, deep links |
| Room (SQLite) | The embedded, offline database — 12 entities, 7 DAOs |
| KSP | Room's annotation processor |
| Retrofit + OkHttp | REST client, multipart photo upload, persistent cookie session |
| kotlinx.serialization | JSON binding (models double as DTOs) |
| Coil (+ SVG decoder) | Image loading and caching, including the demo artwork |
| Kotlin Coroutines + Flow | Asynchronous work; the UI observes database flows |
| AndroidX Lifecycle / ViewModel | Lifecycle-aware state |
| AGP 9.0.1 · Gradle 9.2 · Kotlin 2.2 | Build toolchain (AGP 9 has built-in Kotlin support) |
| minSdk 24 · targetSdk/compileSdk 36 | Runs on Android 7.0 and newer |

Architecture: a small manual dependency graph (`AppGraph`) feeds repositories, repositories read
and write Room and call the API, screens observe `Flow`s and render Compose. No Hilt, no kapt,
no annotation-processing beyond Room's KSP.

---

## Project structure

```
Android Studio version/
├── settings.gradle.kts            module + repository configuration
├── build.gradle.kts               root build: plugin versions, applied nowhere
├── gradle.properties              JVM args, AndroidX, AGP 9 Kotlin source-set switch
├── gradle/
│   ├── wrapper/                   wrapper 9.2 (jar + properties, committed)
│   └── gradle-daemon-jvm.properties   daemon JVM criteria
├── gradlew / gradlew.bat          Gradle wrapper scripts
├── local.properties               SDK path — machine specific, git-ignored
├── README.md                      this file
└── app/
    ├── build.gradle.kts           app module: SDK levels, Compose, Room/KSP, dependencies
    ├── proguard-rules.pro         release shrinking rules
    └── src/main/
        ├── AndroidManifest.xml    permissions, activity, hafiyawe:// deep links
        ├── res/                   strings, colours, themes (light/dark), network config, icons
        └── java/rw/hafiyawe/store/
            ├── HafiYaweApp.kt             Application: builds the graph, configures Coil
            ├── MainActivity.kt            entry point, theme, deep-link parsing
            ├── core/
            │   ├── AppGraph.kt            manual DI: database, session, API, repositories
            │   ├── SessionStore.kt        SharedPreferences: signed-in user, server address, language, theme, demo mode
            │   └── Format.kt              ISO dates, "21h ago", RWF money, plan days left
            ├── data/
            │   ├── Models.kt              12 models, each a Room entity *and* the JSON shape
            │   ├── local/
            │   │   ├── Database.kt        type converters, 7 DAOs, Room database (schema v1)
            │   │   └── DemoSeed.kt        offline seed: listings, chats, payments, help, ticket
            │   ├── remote/
            │   │   ├── ApiService.kt      every endpoint, multipart uploads, support-token header
            │   │   ├── ApiClient.kt       base-URL rewriting, error mapping, cookies
            │   │   └── PersistentCookieJar.kt  HttpOnly session cookie persisted across launches
            │   └── repo/
            │       ├── CoreRepositories.kt     auth, catalog, meta, notifications, payments
            │       └── SocialRepositories.kt   chats, support, admin
            └── ui/
                ├── theme/Theme.kt         colour scheme, typography, shapes
                ├── i18n/Strings.kt        English, Kinyarwanda and French dictionaries
                ├── components/Components.kt   buttons, fields, cards, sheets, empty/error states
                ├── navigation/Navigator.kt    route constants and navigation helpers
                ├── AppState.kt            signed-in user, config, unread badges, prefs
                ├── AppRoot.kt             scaffold, navigation graph, bottom bar, toast host
                └── screens/
                    ├── HomeScreen.kt          announcements, categories, learning-ranked feed
                    ├── BrowseScreen.kt        search, category/condition/province filters, sort
                    ├── ProductDetailScreen.kt photos, price, seller card, favourite, cart, chat
                    ├── AuthScreens.kt         sign in, register, forgot, reset, verify, demo mode
                    ├── SellScreens.kt         create a listing (6 photos) + my listings
                    ├── InboxScreens.kt        chats, conversation, notifications
                    ├── SavedScreens.kt        favourites and cart
                    ├── PaymentsScreen.kt      plans, manual receipt submission, history
                    ├── SupportScreens.kt      help centre, contact form, ticket thread
                    ├── AccountScreens.kt      profile, settings, account status, about/terms/privacy
                    ├── AdminScreen.kt         admin workspace (7 sections)
                    └── Shared.kt              shared screen widgets (bars, pickers, notices)
```

---

## What the app does

Home (announcements, category grid, ranked feed) · Browse with filters and sort · Listing detail
with photo pager · Sign in / register / forgot / reset / verify email · Profile and editing ·
Sell (up to 6 photos) · My listings (off market, restore, delete) · Favourites · Cart ·
Chats and conversation · Notifications · Payments (plans, receipt, history) · Help centre ·
Contact support and ticket thread · Account status with ban reason and appeal · Settings
(server address, language, theme, demo mode) · About / trust / terms / privacy ·
**Admin workspace** (overview, listings, payments, people, support, announcements, help
articles, audit log).

Three languages: **English, Kinyarwanda, French**. Light and dark themes.

### Rules enforced by the app

* Two roles only: `user` and `admin` — one account buys and sells.
* Publishing needs a verified email and a complete profile (phone, 16-digit national ID,
  province, district).
* Posting slots: **2 free listings forever**, then **100 RWF** per listing or **2,000 RWF** for
  30 days. A plan is used first, then a free post, then a credit.
* Every listing is **pending** until an administrator approves it. Rejection, deletion or going
  off market never refunds a slot.
* Payments are **manual**: transfer mobile money, submit the reference and a receipt, an
  administrator verifies the actual transfer. If the server has no merchant instructions the
  payment form is hidden.
* HAFIYAWE **never handles the item price** — the cart is a contact list, not a checkout.
* A banned account can still sign in, read the reason and use support.

---

## The embedded database and on-device learning

Two things live only on the phone:

1. **Offline-first cache.** Every list the app renders comes from Room, so the marketplace,
   chats and notifications work with no connection; a refresh writes the server's answer back
   into the database. Seven DAOs cover products, favourites/cart, chats and messages,
   notifications, payments, support, and metadata/announcements.
2. **An interest model.** Each listing view — and each favourite, weighted higher — is recorded
   as a `view_events` row. The home feed asks SQL for the categories with the highest total
   weight and floats those listings to the top, newest first within each category. Nothing is
   uploaded, and clearing app data resets it.

**Demo mode** (sign in screen → *Explore in demo mode*) seeds that database with 12 listings,
2 conversations, 3 notifications, 2 payments, 4 help articles, an announcement and a support
ticket, then signs you in as a local administrator so every screen — including the admin
workspace — is reachable with no server at all. The local account is
`demo@hafiyawe.local` and has no password; it never exists on the server.

---

## Build and run

Requirements: Android Studio (Ladybug or newer) with Android SDK 36, and a JDK — the bundled
JBR works.

```bash
./gradlew assembleDebug     # app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug      # install on a running emulator or device
```

`local.properties` holds the SDK path and is git-ignored; Android Studio recreates it on another
machine.

**Toolchain note.** This project uses AGP 9.0.1 with built-in Kotlin support. Do **not** apply
`org.jetbrains.kotlin.android` — AGP 9 provides Kotlin itself. `android.disallowKotlinSourceSets=false`
in `gradle.properties` is required so Room's KSP-generated sources are visible.

### Pointing the app at the server

The app defaults to `http://10.0.2.2:3001`, which is *your computer* as seen from the **emulator**.

On a **real phone** that address means nothing. Three steps:

1. Start the API so it listens on the network, not just loopback:

   ```bash
   HOST=0.0.0.0 PORT=3001 npm run dev
   ```

   `netstat -ano | grep :3001` should show `0.0.0.0:3001 LISTENING`.
2. Find the computer's LAN address with `ipconfig` (the Wi-Fi adapter's IPv4).
3. In the app, tap the **Server address** line on the sign-in screen and enter
   `http://YOUR-PC-IP:3001`, then **Save** and sign in. Phone and computer must be on the same
   Wi-Fi, and the same address is editable later in **Account → Settings**.

### Development accounts

Created by `npm run db:seed` on the server in development:

| Role | Email | Password |
| --- | --- | --- |
| Administrator | `admin@hafiyawe.test` | `DemoPass123!` |
| Seller | `seller@hafiyawe.test` | `DemoPass123!` |
| Buyer | `buyer@hafiyawe.test` | `DemoPass123!` |

A production server has no default administrator: set `BOOTSTRAP_ADMIN_EMAIL` and a strong
`BOOTSTRAP_ADMIN_PASSWORD`, run the seed once, then remove the password from the environment.
Registration always creates an ordinary `user`.

### Deep links

`hafiyawe://verify?token=…` and `hafiyawe://reset?token=…` are handled in `MainActivity`, so the
verification and password-reset links the API prints in development open the right screen.

---

## Status

Verified on an Android 37 emulator:

* `./gradlew assembleDebug` succeeds and produces the debug APK.
* Home renders the live listings from the running server, which exercises Retrofit, the cookie
  jar, JSON parsing and the Room cache.
* Browse loads with filters and reports the server's result count; a listing opens with photos,
  price, condition and seller card.
* Demo mode seeds the embedded database with no server, signs in as the local administrator and
  shows the sample feed.
* No `FATAL EXCEPTION` in logcat during the above.

Not verified: Google/Apple sign-in (the server has no provider credentials), real mobile-money
approval (needs merchant instructions and an actual transfer), and push notifications — the app
polls like the web client because there is no FCM project.
