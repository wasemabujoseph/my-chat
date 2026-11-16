# FMHYPlay Arabic Sources — Research & Verification (2026-11-16)

## Scope

A curated **movies and TV/series** list for the **unaltered** AnantPlay/FM​HYPlay Android app. Only external source files changed, not the app.

- **18 unique sites**
- **18 entries in `rows.movies` and an exactly identical 18 entries in `rows.tv`**
- **`rows.anime` = []**. Removing anime sources does **not** remove AnantPlay's built-in Anime tab.
- All seven newly added sites were checked through current (2026-11-16) web results and opened catalog/detail pages where accessible; **none has been playback-tested on Android**.
- **Same index URL and source ID** as before: `https://raw.githubusercontent.com/wasemabujoseph/my-chat/fmhyplay-sources/index.json` and `arabic-sites`.

## Candidate criteria

1. Publicly available, indexed catalog for both films and series; actual title/episode pages where available.
2. Prefer a usable title search and obvious Movies/Series categories.
3. Prefer a known top-level home page; when the home page could not load in the checker, use a confirmed category entry page and mark as *limited*.
4. Reject unrelated directories, anime-only sites, dead / looping redirects and multiple same-brand copycat variants when there is no clear benefit.
5. Keep the 11 existing sites the user previously tested (some already reported working), even if some web checks are inconclusive.

**Critical limitations:** An indexed webpage or a clickable title is **not** evidence that streaming servers work, that a site is licensed or safe, that it is an authentic 'official' domain, that content has been released, or that AnantPlay's automatic search and ExoPlayer will support it. Do not bypass authentication, DRM or other access controls.

## Newly added (7)

| Site | URL in configuration | Web check | Caution |
|---|---|---|---|
| WeCima | https://wecima.cx/ | Homepage + distinct movies/series navigation + TV title detail page | Web page content; **stream not verified** |
| MyCima | https://www.mycima.si/ | Homepage, title search, movie & series navigation, 2026 title detail page | Playback not verified; name is used by many lookalike sites |
| CimaWebas | https://cimawebas.com/ | Search indexing and series category pages with recent episodes | Direct homepage fetch **timed out** in one checker run; device test required |
| CimaMix | https://cimamix.net/ | Homepage, dedicated shows page, episode detail | One detail displayed **"loading servers"**; playback not confirmed |
| Shahid4U | https://www.shahid4u.si/arabic/ | Arabic films & series index, search and title categories loaded | Site **root timed out**; specific /arabic page works in web checker |
| CimaFlash | https://www.cimaflash.si/arabic/ | Arabic films & series index plus film detail | Site **root timed out**; specific /arabic page works in web checker |
| CimaCima | https://cimacima.online/ | JS-driven movie and TV index; series page with episodes | **Requires JavaScript**; player interaction not tested |

## Retained existing (11)

Fasel HD (`www.fasel-hd.co/main`), EgyBest (`iegybest.cimawbas.tv/egy1/index.php`), Akwam (`akwam.ss`), ArabSeed (`arabseeds.watch/main/`), Cima4U (`cima4u.college`), CimaClub (`cimaclub.live`), ShoFree (`shoffree.cc/explore`), ZaFlam (`zaflam.com`), Aflaam (`aflaam.com`), CimaLight (`r.cimalight.co`), Qisat Ishq (`qisatishq.com`).

## Investigated but not activated

- `www.faselhd.si` and `www.egybesti.si`: duplicate brands to sources already in the list; potential lookalikes.
- `wecimas.watch`, `wecima.mobile`, `s.weecima.website`: duplicate WeCima branding / redirects; selected a single WeCima domain.
- `fusshaaar.cyou`: redirect to `fusshaaar.xyz` did not complete in checker.
- `e.ifushaar.com`, `fushaar.com`: focused mainly on films, while the user requires **same sites for Movies and Series**.
- `cima-flix.com`: root did not reliably load.
- Various one-page directories and sites publishing unverified titles as already available.

## Operational test checklist

For each site on Android:

1. Open **Movies → website** from source list and confirm the catalog appears.
2. Use **Movies → title → Watch on website**; note whether automatic title search finds a real result.
3. On the site, open a permitted video and see if the stock AnantPlay player opens (audio, captions, seeking).
4. Repeat for a **series episode** in the **TV** tab on the **same domain**.
5. Check the domain shown in the address bar after redirections; avoid unexpected logins and pop-ups.

If a site stays on **Searching for the title**, tap or Back once to inspect the page. This often indicates the site's search/landing page is incompatible rather than broken ExoPlayer.

## Sources checked

- https://github.com/AnantPlay/index
- https://github.com/AnantPlay/android
- https://wecima.cx/series/khamsin-khamsin-2026-series
- https://www.mycima.si/movie/hokum-1430077/
- https://cimawebas.com/series/?ncp_view=episodes
- https://cimamix.net/watch-show/333101-treasure-island-2026/s1/e6
- https://www.shahid4u.si/arabic/
- https://www.cimaflash.si/arabic/
- https://cimacima.online/

**Summary:** web/catalog availability screened for 18; confirmed video playback: **0/18 tested** (not a failure count).
