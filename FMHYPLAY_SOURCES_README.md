# FMHYPlay / AnantPlay — Curated Arabic Sources

A focused external source index for the **unchanged upstream AnantPlay app**.

Last link/catalog review: **2026-11-15**.

## What is included

- **11 shared movie-and-series websites**, listed in both the Movies and TV tabs. The two sections contain exactly the same URLs in the same order.
- **3 anime websites** in the Anime tab.
- **14 unique website URLs** in total.
- Only external source configuration changed: no Android APK, player, styling, TV controls, update system, downloads, subtitles, or WARP code was modified.

## Install / refresh

1. In AnantPlay, open **Settings → Sources**.
2. If not already installed, select **Add an index by link** and paste:

   `https://raw.githubusercontent.com/wasemabujoseph/my-chat/fmhyplay-sources/index.json`

3. Select **Arabic Movies, Series & Anime** and tap **Add**.
4. If already installed, do **not** add another index: use **Settings → Sites → Refresh sources**. The upstream application also refreshes source files periodically (normally every 12 hours).

## Repository files

| File | Purpose |
| --- | --- |
| `index.json` | AnantPlay-compatible source index, preserving the same URL and source ID as the previous version |
| `sites.json` | App-compatible `rows` structure: same 11 sites in both `movies` and `tv`, plus 3 anime |
| `SOURCE_REVIEW_2026-11-15.md` | Selection criteria, what was tested, and source-by-source caveats |
| `AUDIT_2026-11-15.md` | Historical audit of the **older 33-site** list; **superseded** |

## Verification standard

A site is included only if a web check on November 15, 2026 showed an accessible relevant catalog/search or recognizable film/series categories.

**Catalog verification is not playback verification.** Website access can differ by country, device, IP or time, and page redirects may change. The verification does not establish that the publisher is legitimate, secure, licensed, or free of risky ads. Only use content you have the right to view; do not enter private credentials on unexpected mirrors or download third-party apps from them.

The upstream code and source format are documented here:
- https://github.com/AnantPlay/android
- https://github.com/AnantPlay/index
