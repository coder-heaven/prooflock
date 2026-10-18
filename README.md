# ProofLock

ProofLock is a standalone Android habit app for turning good intentions into daily proof. It is not connected to Study Sprint and has its own package, project, storage, UI, and brand.

The product idea is simple: choose one activity, complete it, submit a **fresh camera capture**, and earn access to distracting apps. ProofLock never accepts a photo picked from the gallery in the proof flow.

## Why this name

**ProofLock** is short, memorable, and explains the two core actions:

- **Proof** — show that the real-world activity happened.
- **Lock** — keep distracting phone access unavailable until the commitment is complete.

## MVP direction

1. Create one daily activity.
2. Keep the user in a protected phone mode while the activity is pending.
3. Capture proof only through the camera.
4. Check that the capture happened today.
5. Unlock the user's chosen app group after proof passes.
6. Add optional AI vision verification after the local flow is validated.

The current first slice includes a redesigned onboarding flow, Today dashboard, streak/history view, settings, protected-state dashboard, and fresh camera proof capture. Android Accessibility enforcement and server-side AI verification are intentionally separate follow-up milestones so the basic user experience can be tested first.

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

The GitHub workflow lives at `.github/workflows/android-release.yml`. It uses Java 17 on the runner, uploads a debug APK artifact, and publishes a GitHub Release whenever a `v*` tag is pushed.

## Development

```sh
./gradlew assembleDebug
```

Install the debug APK from `app/build/outputs/apk/debug/app-debug.apk` on an Android 8.0+ device or emulator with a camera. `PRIVACY.md` contains the current privacy summary for the first APK release.
