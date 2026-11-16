# FMHYPlay / AnantPlay — Arabic Movies & Series Sources

User-curated external site source list for **unmodified AnantPlay**. This repository does NOT modify the APK, UI, ExoPlayer, WARP, subtitles, downloads, or settings.

## Current configuration (2026-11-16)
- **18 sites for Movies**
- **The exact same 18 sites for TV/Series** (same order and addresses)
- **0 Anime sites** (the app's original Anime navigation tab still exists)
- The **same index URL and source ID** are retained for installed users.

### Install or refresh
1. Open **Settings → Sources** in AnantPlay.
2. If the index is not already added, select **Add an index by link** and paste `https://raw.githubusercontent.com/wasemabujoseph/my-chat/fmhyplay-sources/index.json`.
3. Add source **Arabic Movies & Series (18 sites)**.
4. If installed already, simply use **Settings → Sites → Refresh sources**; no second index is required. The app also periodically refreshes sources.

### Files
- `index.json`: app-compatible source index, with the prior stable `arabic-sites` source ID.
- `sites.json`: current sites, identical Movies and TV rows, empty Anime row.
- `SOURCE_REVIEW_2026-11-16.md`: **current** web research and individual link caveats.
- `SOURCE_REVIEW_2026-11-15.md` / `AUDIT_2026-11-15.md`: historical/superseded reports.

### Verification
Web checks confirm that sites displayed relevant movie/series catalogs and navigation or indexed title pages at review time, not that links are safe, officially owned, licensed, or playable in AnantPlay. Some sites are currently accessible only via category URLs; some may use unstable redirects, misleading ads or require login. Only watch content you are authorized to access, do not enter credentials on unexpected mirrors, and do not assume a site's claimed release dates are true.

Upstream spec: https://github.com/AnantPlay/index

### Search compatibility (2026-11-16)

See [`SEARCH_COMPATIBILITY_AUDIT_2026-11-16.md`](SEARCH_COMPATIBILITY_AUDIT_2026-11-16.md) before claiming full search support. Remote checks exposed a search input on 12/18 fetched pages; 6 remain unconfirmed. **No site was tested end-to-end in Android WebView**. The stock `autosearch.js` does not recognize Arabic-only text placeholders unless an input is typed as `search` or has an English search identifier. All original 18 site URLs remain in the source list.
