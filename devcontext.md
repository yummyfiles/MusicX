# MusicX Development Context

## Post-Commit Release Workflow

After completing and committing a MusicX change, **always run `python release.py`** from the repository root and monitor its output until it finishes.

The `release.py` script is responsible for all post-commit release operations, including:

- Pushing commits to GitHub
- Managing version tags
- Checking version continuity
- Building the release APK
- Creating or updating the GitHub Release
- Generating release notes
- Uploading the APK
- Verifying the release

**Do not manually perform these release operations yourself.**

Do not manually run commands such as:

- `git push`
- `git tag`
- `git push origin <tag>`
- `gh release create`
- `gh release upload`
- Manual APK release/upload commands

Instead, let `release.py` handle them.

After running the script:

1. Monitor its output.
2. Wait for it to finish.
3. If it succeeds, report the release result.
4. If it fails, inspect the error and report what went wrong.
5. Do not bypass the script by manually performing the failed release step unless explicitly instructed by the user.

The intended workflow is:

**Edit MusicX → test changes → commit changes → run `python release.py` → monitor → report result.**

Treat `release.py` as the **single source of truth for MusicX post-commit release automation**.
