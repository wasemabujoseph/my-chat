# AnantPlay: FG Code input (prototype patch)

This patch is for the **original AnantPlay Android repository**, not for the external `sites.json` list.

## What the patch changes

- Adds an `FG Code` control next to the existing Stalker button in IPTV → Add a playlist.
- Accepts an FG identifier (e.g. `202122`) or a full `fgcode.org/202122` link.
- Converts it into the usual HTTPS FG playlist address.
- Passes it to the existing `Iptv.addPlaylist()` logic, preserving the original player, channel rows, metadata, grouping, refresh, history and favorites.
- Refuses malformed input, and explicitly warns if the code produces **zero M3U channels**.
- Adds a pure-Java URL normalizer and 16 unit-test cases.

No fallback to hidden Drama Live APIs, no fabricated M3U channels, no DRM/access-control bypass.

## Apply to upstream source

Obtain the upstream AnantPlay checkout, then download `AnantPlay_FG_Code_support.patch` from this directory.

```bash
git clone https://github.com/AnantPlay/android.git
cd android
git apply --check AnantPlay_FG_Code_support.patch
git apply AnantPlay_FG_Code_support.patch

mkdir -p /tmp/fg-code-test
javac -d /tmp/fg-code-test src/me/champdev/weblock/FgCode.java test/FgCodeTest.java
java -cp /tmp/fg-code-test me.champdev.weblock.FgCodeTest

# Follow the upstream Android / WARP build instructions for the actual APK:
# https://github.com/AnantPlay/android
```

## Compatibility limitations

The FG service says it provides playlists through M3U URLs. For those codes, the pre-existing AnantPlay importer is sufficient after URL normalization.

**Important:** The specific user-provided code `fgcode.org/202122` worked in Drama Live Web, but it has **not** been proven to expose a nonempty M3U playlist directly. Some Drama Live codes may use app-specific catalogs. This patch **does not guarantee** that the Drama Live channel list appears in AnantPlay when a code returns empty M3U data. An authorized M3U export or documented provider API would be required.

## Verification status

- URL parser and validation: **16/16 tests passed** (Java compilation and execution in a local environment).
- Patch format: checked for consistent unified-diff hunk sizes.
- Android build, app UI, network import and channel playback: **not run or verified**.
- GitHub main branch and existing AnantPlay APK: **unchanged**.
