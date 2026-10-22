# ProofLock

ProofLock is a standalone Android habit app for turning good intentions into daily proof. It is not connected to Study Sprint and has its own package, project, storage, UI, and brand.

The product idea is simple: choose one activity, complete it, submit a **fresh camera capture**, and earn access to distracting apps. ProofLock never accepts a photo picked from the gallery in the proof flow.

## Why this name

**ProofLock** is short, memorable, and explains the two core actions:

- **Proof** — show that the real-world activity happened.
- **Lock** — keep distracting phone access unavailable until the commitment is complete.

## v0.2 direction

1. Choose multiple habits including movement, reading, sketching, meditation, home tasks, lessons, and online focus tasks.
2. Keep the user in a protected phone mode while active habits are pending.
3. Capture proof only through the camera.
4. Send proof to an optional backend AI verifier with fail-closed results.
5. Choose distracting apps from installed launchable apps and suggested categories.
6. Escalate reminders and support consent-based screen sharing for online tasks.

The current slice includes a health-style multi-habit dashboard, habit history, app selection, Accessibility protection wiring, escalating reminders, a screen-sharing foreground-service flow, and a backend with nine AI capability slots. Each slot has a primary route and two backups. Configure `PROOFLOCK_API_URL` to connect the APK to the backend; without it, the APK uses clearly labelled local prototype mode.

## Backend

The online verifier is in `backend/`. It accepts fresh camera evidence and routes the `habit_proof` check through one primary AI route and two backups. The other eight capability slots are defined for future specialized checks without putting provider secrets in the APK. See [backend/README.md](backend/README.md).

## Direct APK release

This project is being prepared for direct APK distribution through GitHub Releases rather than Google Play publishing. See [PUBLISHING.md](PUBLISHING.md) for the workflow and tag commands. Before distributing a production APK, create a private release keystore and configure Gradle signing through GitHub Actions secrets. Never commit the keystore or passwords.

For a local development APK:

```sh
./gradlew assembleDebug
```

For a signed APK, configure a release signing block in `app/build.gradle.kts` using private environment variables, then run:

```sh
./gradlew assembleRelease
```

The GitHub workflow lives at `.github/workflows/android-release.yml`. It uses Java 17 on the runner, uploads a debug APK artifact, and publishes a GitHub Release whenever a `v*` tag is pushed. Add `PROOFLOCK_API_URL` as a GitHub Actions variable when building an online-enabled APK.

## Development

```sh
./gradlew assembleDebug
```

Install the debug APK from `app/build/outputs/apk/debug/app-debug.apk` on an Android 8.0+ device or emulator with a camera. `PRIVACY.md` contains the current privacy summary for the first APK release.
