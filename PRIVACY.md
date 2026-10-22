# ProofLock privacy summary

ProofLock's first APK release is local-first.

- Commitment names, completion dates, and proof timestamps are stored on the device.
- Proof photos are captured through the camera. If an online backend URL is configured, the selected proof image is sent to that backend for verification; otherwise it stays in the local prototype flow.
- The app does not include advertising, analytics, accounts, or third-party tracking.
- Camera access is used only when the user taps **Capture fresh proof**.
- Accessibility access is optional and is used only to detect selected foreground apps so ProofLock can redirect them while protection is active.
- Screen sharing is optional, requires Android's system consent dialog, and runs as a visible foreground service while active.
- Users can remove local data by clearing the app's storage or uninstalling the app.

AI providers are configured server-side. Provider API keys are never included in the APK. Backend deployments must use approved retention, no-training, encryption, and deletion policies for uploaded evidence. Screen-sharing data is not uploaded by the current service implementation; it only creates a local projection session for an online task.
