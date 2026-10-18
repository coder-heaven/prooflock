# Publishing ProofLock with GitHub Actions

ProofLock is configured to publish a direct-download APK through GitHub Releases.

## First-time setup

1. Create an empty GitHub repository, for example `prooflock`.
2. Add this local repository as its `origin` remote.
3. Push the `main` branch.
4. In GitHub, open **Actions** and run **Build and publish APK** manually once.

## Publish a versioned APK

From the repository root:

```sh
git add .
git commit -m "feat: launch ProofLock habit app"
git push -u origin main
git tag v0.1.0
git push origin v0.1.0
```

Pushing the `v0.1.0` tag runs the workflow with Java 17, builds `app-debug.apk`, and attaches it to a GitHub Release. The same APK is also retained as a workflow artifact.

## Important release note

The first workflow publishes a debug-signed APK for early testers. It is suitable for direct installation and product validation, but it is not the final production signing setup. Before a public production release, create a private release keystore, store its values as GitHub Actions secrets, and change the workflow to run `assembleRelease`.

Never commit a keystore, signing password, API key, or other private credential.
