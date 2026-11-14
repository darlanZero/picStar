<p align="center">
  <img src="favicon.ico" alt="PicStar Icon" width="72" height="72" />
</p>

<h1 align="center">PicStar</h1>

<p align="center">
  <strong>A modern alternative Pixiv Android client built with Kotlin &amp; Jetpack Compose</strong><br />
  Inspired by PixEz Flutter's API architecture, featuring a custom UI focused on artist discovery.
</p>

---

## Overview

**PicStar** is a native Android application built with **Kotlin**, **Jetpack Compose**, and **Material 3**, communicating directly with Pixiv's official App-API (`app-api.pixiv.net`) via **OAuth2 PKCE** authentication.

The project delivers a modern, fluid browsing experience while preserving Pixiv's core discovery workflows — especially the **"Other artists to follow"** drawer triggered whenever you follow a creator.

---

## Key Features

### Authentication & Session
- Integrated **OAuth2 PKCE login** via WebView (`pixiv://account/login?code=...`) with automatic *refresh token* and *access token* renewal.
- **Authentication gate**: access to the app is locked behind a dedicated login screen until a valid Pixiv session is active.

### Home (`For You` & `Following`)
- **For You**:
  - **Pixiv Highlights / Pixivision** carousel.
  - **Recommended Artists** carousel (capped at 10 cards with a **View more** modal for the full list), showing each artist's avatar on the right and their **5 latest illustrations** inside the card.
  - **Other artists to follow** suggestions (`/v1/user/related`) expanded whenever you follow an artist.
  - Infinite-scrolling **Recommended Illustrations** feed.
- **Following**:
  - Toggle between **latest works** from followed artists and a **By followed artists** view.

### Popular
- Pixiv rankings organized by mode:
  - `Daily`, `For Male`, `For Female`, `Original`, `Rookie`, `Weekly`, `Monthly`, `AI`, `R-18`, `R-18 AI`, `R-18 Weekly`, `R-18G`.

### Favorites
- Bookmarked/liked illustrations with dynamic tag filtering.

### Advanced Search
- Floating search button accessible across main tabs, featuring **recent search history**, **recommended tags**, and **live autocomplete** after 2+ characters.
- Navigable sort chips on search results: `Newest`, `Popular ⌄` (with dropdown for *All*, *Male*, and *Female*), and `Oldest`.
- Floating filter modal on search results:
  - **Work type** (Illustrations, Manga, Ugoira, Novels, and combinations)
  - **Posting date** (Any time, Past 24 hours, Past week, Past month, Past 6 months, Past year)
  - **Like counts**, **AI-generated** toggle, and **Mature content (R-18)** toggle.

### Post Detail & Creator Profile
- **Post Detail**:
  - **Single tap** opens a **Fullscreen Image Viewer** with multi-page swiping and pinch/double-tap zoom.
  - **Long press** downloads illustrations or Ugoira `.zip` archives directly to your configured folder (enabled only after setting a download folder in **Settings > Download settings** via Android SAF / `OpenDocumentTree`).
  - Native frame-by-frame **Ugoira** animation playback.
  - Clickable **translated tags** (`translated_name` alongside `#tag`).
  - Clickable **bookmark count** to browse the users who bookmarked the post.
  - Collapsible **Comments dropdown**, with direct navigation to any commenter's profile.
  - Infinite-scrolling **More Like This** recommendations.
- **Creator / User Profile**:
  - Bio, follower/MyPixiv counts, clickable social links (**Website**, **Twitter/X**), and follow button with related artist suggestions.
  - Tabs: **Works** (filterable by `All`, `Illustrations`, `Manga`, and `Manga Series`), **Likes**, **Following**, **MyPixiv**, and **Followers**.

### Settings
- **Profile**, **Preferences**, **Download settings** (SAF download folder picker, image quality, file naming pattern, and `pximg` host/mirror), **Theme**, and **About**.

---

## Tech Stack & Project Structure

- **Language**: Kotlin (JVM 17)
- **UI**: Jetpack Compose + Material 3 (`compileSdk = 35`, `minSdk = 26`)
- **Networking & Media**: `HttpURLConnection` with Pixiv client headers (`Referer: https://app-api.pixiv.net/`, `X-Client-Time`, `X-Client-Hash`), direct bitmap decoding, and `ZipInputStream` for Ugoira frames.
- **Storage**: `SharedPreferences` + Android Storage Access Framework (`DocumentsContract`) for downloads.

```text
app/src/main/java/com/picstar/app/
├── api/          # PixEzApiClient (OAuth2 PKCE + Pixiv App-API)
├── data/         # PicStarViewModel and PicStarRepository
├── model/        # Domain models, filters, and UI state
├── ui/
│   ├── components/ # Cards, PximgNetworkImage, UgoiraAnimationPlayer
│   ├── screens/    # Home, Popular, Favorites, Search, Settings, Modals
│   └── theme/      # Themes (Midnight, etc.) and typography
└── MainActivity.kt
```

---

## Building & Installing

```bash
# Run unit tests and build the debug APK
./gradlew testDebugUnitTest assembleDebug --no-daemon

# Install on a connected Android device via ADB
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
