# Source review — 2026-11-15

This document records why the 14 selected addresses were retained. It is a **web-navigation-only** check. **No playback on AnantPlay was tested**.

## Selection rules

1. Keep only sites that displayed a relevant catalog, movie/series categories or a search screen on web inspection.
2. Prefer a concrete catalog page to an intermediate gateway when its path is known and stable.
3. Use **identical URLs in identical order** under `rows.movies` and `rows.tv` so one website serves both Movies and TV/Series.
4. Keep dedicated Anime sites in `rows.anime`; preserve the original AnantPlay app and its parsing format.
5. Exclude sites that were inaccessible, timed out, acted only as a news portal, or presented an unresolved gateway when the review was performed.
6. Never claim that catalog availability establishes video playback, website security, permissions, or domain authenticity.

## Curated website list

| Website | Address | Review observation |
| --- | --- | --- |
| Fasel HD | https://www.fasel-hd.co/main | Both movies and TV; mixed homepage with titles |
| EgyBest | https://iegybest.cimawbas.tv/egy1/index.php | Both; direct catalog destination on the host supplied earlier |
| Akwam | https://akwam.ss/ | Both; movie and series menus plus search |
| CimaClub | https://cimaclub.live/ | Both; movies/series menu and categories |
| Cima4U | https://cima4u.college/ | Both; movie and series categories; individual media page timed out in one follow-up check |
| ShoFree | https://shoffree.cc/explore | Both; bypasses homepage welcome and shows film and series categories |
| ArabSeed | https://arabseeds.watch/main/ | Both; catalog/search page, but automatic search had trouble on old gateway address |
| ZaFlam | https://zaflam.com/ | Both; movies/series categories and search; login prompt may appear |
| Aflaam | https://aflaam.com/ | Both; film/TV categories and search |
| CimaLight | https://r.cimalight.co/ | Both; redirects to e.cimalight.co catalog; redirect may change |
| Qisat Ishq | https://qisatishq.com/ | Mostly Turkish series; catalog also lists films, but series focus |
| Anime3rb | https://anime3rb.com/ | Anime titles and categories, JavaScript required for full UI |
| AnimeDar | https://web.animedar.net/ | Anime catalog and search; redirect destination |
| AnimeHQ | https://animhq.com/ | Anime movies/series and search; some VIP-only content |

First 11 entries appear in **both Movies and TV**; last 3 appear in **Anime** only. Counts: 11 Movies, 11 TV, 3 Anime; 14 unique addresses.

## Reference pages inspected

- https://www.fasel-hd.co/main
- https://www.fasel-hd.co/series
- https://iegybest.cimawbas.tv/egy1/index.php
- https://akwam.ss/
- https://arabseeds.watch/main/
- https://cimaclub.live/
- https://cima4u.college/
- https://shoffree.cc/explore
- https://shoffree.cc/series
- https://zaflam.com/series
- https://aflaam.com/
- https://r.cimalight.co/
- https://qisatishq.com/
- https://anime3rb.com/
- https://animhq.com/
- https://web.animedar.net/

## Status caveats

- **Verified:** A webpage was accessible and displayed a movie/series/anime catalog or search affordance at review time.
- **Not verified:** Any particular title will play; extraction into ExoPlayer will succeed; the domain is official, safe, or appropriately licensed; the site will work from every location.
- A login screen or redirect could still interrupt automatic title search. If this occurs on-device, use the app's ability to reveal the page and test manually, or remove the source from the list after confirming failure.
- The older `AUDIT_2026-11-15.md` is historical (33-site candidate set) and is not the present source roster.

## Refresh

AnantPlay fetches sites added in **Settings → Sources** and refreshes the source periodically, roughly every 12 hours. Use **Settings → Sites → Refresh sources** to request an immediate update. The source index URL and source ID have not changed.

Source schema: https://github.com/AnantPlay/index
