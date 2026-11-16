# FMHYPlay / AnantPlay — Search compatibility audit
## 2026-11-16

### Scope and result

This audit checks the **18 currently configured sites** listed in `sites.json` (Movies and TV rows are intentionally identical). It combines an **upstream Android source-code review** with **remote text-rendered website checks**.

- **18/18** sites reviewed individually.
- **12/18** pages exposed an explicit site search input in fetched/rendered text (this is *not* a visual or DOM-level assertion).
- **6/18** did **not** provide enough evidence to verify a discoverable site search input: EgyBest, WeCima, Cima4U, CimaWebas, ZaFlam and CimaLight.
- **0/18** sites had Android WebView/ExoPlayer playback or automatic-search end-to-end tests. This audit does NOT guarantee that the Search button appears visually or that movie/episode search succeeds.
- All 18 active entries remain in the app's JSON list. No unverified URLs were silently replaced or deleted.

### Why web search UI does not prove automatic title search works

The unmodified application uses `assets/autosearch.js`, invoked from `WebActivity.java`. The script:

1. Searches currently visible title links for a text match.
2. Looks for a **visible** `<input>` of type `search`, or whose placeholder/name/id/aria-label/title/class matches an English regexp approximately `search|find|title|movie|show|query|q`.
3. If no input is found, tries to click a button/icon whose labeling/class includes English `search`.
4. Enters the title, dispatches Enter/input events, waits for an exact-enough title match, and times out if there is no progress.

**Known limitation:** A field such as `<input type="text" placeholder="ابحث عن فيلم">` with no English identifying attribute is **not** recognized by that regexp. This was verified separately with a local JavaScript regular-expression test. A site may display a working Arabic search box yet AnantPlay's auto-search still be unable to use it.

The application can eventually dismiss its "Searching for the title…" overlay, exposing the web page for manual navigation. It does not currently provide per-site CSS search selectors in the external `sites.json` format.

### Per-site web check

| Site | Remote page evidence | Notes | 
|---|---|---|
| Fasel HD | Search input shown | A search field with Arabic placeholder is present in rendered text; actual DOM input type / visibility not verified |
| EgyBest | Search control not established | Catalog and login fields are present; no distinguishable site search field appeared in fetched text |
| WeCima | Unlabeled input / uncertain | An input appears without a descriptive label; cannot determine its type, visibility or whether native autosearch can find it |
| MyCima | Search input shown | Explicit 'ابحث عن فيلم، مسلسل أو ممثل...' input shown in page content |
| Akwam | Search input shown | Multiple search inputs and an explicit search button shown |
| ArabSeed | Search input shown | Site search prompt 'إبحث عن عمل او ممثل...' appears on main catalog |
| Cima4U | Search control not established | Movies and series navigation are available, but search input/button was not exposed in rendered text |
| CimaClub | Search input shown | Explicit search field 'الكلمات الدلاليه للبحث' and button 'بحث' shown |
| CimaWebas | Unconfirmed — timeout | Direct homepage fetch timed out; search-indexed catalog pages exist, but current search UI could not be inspected |
| ShoFree | Search input shown | Explicit 'البحث في شوف فري' field on /explore |
| CimaMix | Search input shown | Explicit search fields and buttons on homepage |
| Shahid4U | Search input shown | Explicit 'ابحث عن فيلم، مسلسل أو ممثل...' field on /arabic/ |
| CimaFlash | Search input shown | Explicit 'ابحث عن فيلم، مسلسل أو ممثل...' field on /arabic/ |
| CimaCima | Search input shown / JS warning | Explicit 'ابحث عن فيلم أو مسلسل...' field; website notes JavaScript required |
| ZaFlam | Unconfirmed — timeout | Direct homepage timed out; recently indexed series catalog loads but homepage search UI couldn't be inspected |
| Aflaam | Search input shown | Explicit 'ابحث هنا' field and movie/series navigation |
| CimaLight | Search control not established | Home redirects to e.cimalight.co/main31 with a catalog and login fields, but no explicit search field in parsed text |
| Qisat Ishq | Search input shown | Explicit 'ابحث عن مسلسل...' field in homepage |

**Classification note:** Search input text such as `[Input: ابحث...]` only confirms it was present in a remote parsed page. It does **not** establish the `input.type`, `getBoundingClientRect()`, whether the input is hidden behind an overlay, whether JavaScript clicking it works, or that returned results match the app's algorithm.

### Minimal safe path to a fully verified experience

1. **Keep the AnantPlay app unchanged** as requested. The existing site list can open sources as-is.
2. In an actual Android WebView session on the user's device, test one movie (e.g. **Interstellar**) and one series (e.g. **Breaking Bad**) for **each of the 18** sources: icon visible → query typed → results appear → title selected → playback (when the viewer has access).
3. For failures, inspect the site's search DOM **without changing access permissions**. If the site has only Arabic-labeled fields, a *future optional* minimal patch to `assets/autosearch.js` could include Arabic `بحث|ابحث|اسم الفيلم|المسلسل` in its search selectors and add per-site workarounds **only after testing**.
4. No change to `index.json` or `sites.json` alone can guarantee search-field visibility or make AnantPlay click an inaccessible site search icon.
5. Do not assume that a title shown in a catalog has a playable stream, or bypass paywalls, logins, DRM or access restrictions.

### Source code reviewed

- https://github.com/AnantPlay/android/blob/main/assets/autosearch.js
- https://github.com/AnantPlay/android/blob/main/src/me/champdev/weblock/WebActivity.java
- https://github.com/AnantPlay/index/blob/main/README.md

### Current configuration (unchanged)

- Source URL: https://raw.githubusercontent.com/wasemabujoseph/my-chat/fmhyplay-sources/index.json
- Site list: https://raw.githubusercontent.com/wasemabujoseph/my-chat/fmhyplay-sources/sites.json
- Movies = 18, Series = 18 (same URLs), Anime = 0.
