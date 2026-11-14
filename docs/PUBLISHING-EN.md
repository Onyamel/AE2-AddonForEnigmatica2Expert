# Updating with GitHub Desktop

**English** | [Русский](PUBLISHING-RU.md)

1. Select your repository under **Current repository** and `main` under **Current branch**.
2. Review **Changes**. Commit your own unfinished work separately before replacing files. Click **Fetch origin**, then **Pull origin** if offered.
3. Choose **Repository → Show in Explorer** to open the local checkout.
4. Open the `repository` folder from the prepared `github-1.3.0` package. Copy its **contents** into the checkout root, replacing matching files. Do not nest the entire `repository` folder inside the checkout or remove the hidden `.git` directory.
5. Review **Changes** in Desktop. README.md is English and README-RU.md is Russian. Keep `src`, `docs`, `tools`, `gradle` and `.github` in the root. Do not add build output, test worlds, third-party jars or the entire transport ZIP.
6. Enter `Release 1.3.0: logic fixes and bilingual documentation` in **Summary**, click **Commit to main**, then **Push origin**. For a protected branch, use a separate branch and a pull request.
7. Choose **Repository → View on GitHub** to verify the updated README and version.

Copying replaces matching files while preserving Git history and your extra files. Older changelogs and reports may remain as release history. Do not use Discard Changes to apply an update: that command undoes local changes.

## Publishing the playable release

Pushing source changes does not update assets on an existing release. On GitHub, open **Releases → Draft a new release**. Create tag `v1.3.0` against the pushed `main` branch. Title: `AE2 Expert Addon 1.3.0 — Minecraft 1.12.2`.

Paste the bilingual `RELEASE_NOTES.md` and attach `ae2expert-1.3.0.jar`, `ae2expert-1.3.0-release.zip`, `ae2expert-1.3.0-sources.zip` and `SHA256SUMS.txt`. Do not upload the test jar. Review the draft and click **Publish release**. Keep older releases if desired.

A release in a private repository remains available only to people with repository access. These steps do not change visibility. Update the repository About text to: `AE2 addon for Enigmatica 2: Expert 1.93a. English and Russian supported.`

## Package contents and licensing

`repository` contains source files ready for Code; `*-sources.zip` contains the same source tree. `*-release.zip` contains the playable jar, instructions and licenses. `*-github-pack.zip` is the complete transport bundle and should not be committed wholesale.

Preserve LICENSE, NOTICE.md, NOTICE-RU.md and licenses: code is LGPL-3.0-only; AE2-derived assets are CC BY-NC-SA 3.0.

## GitHub documentation / Справка GitHub

- [Commit and push with Desktop](https://docs.github.com/en/desktop/making-changes-in-a-branch/committing-and-reviewing-changes-to-your-project-in-github-desktop)
- [Manage releases](https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository)
