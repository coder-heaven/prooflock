# ProofLock AI backend

This backend is the online verification boundary for the phone app. The APK never contains provider keys or fallback mappings.

It exposes:

- `GET /health`
- `POST /v1/verify` with `habitId`, `habitTitle`, `proofPrompt`, `capturedAt`, and `imageBase64`

The backend defines **nine AI capability slots**. Each slot has one primary route and two backup routes. The current verifier uses the `habit_proof` slot and fails closed when all routes are unavailable.

Configure the route URL/model pairs with environment variables such as:

```sh
PROOFLOCK_AI_HABIT_PROOF_URL=https://your-provider.example/v1/chat/completions
PROOFLOCK_AI_HABIT_PROOF_MODEL=your-vision-model
PROOFLOCK_AI_HABIT_PROOF_BACKUP_1_URL=https://backup-one.example/v1/chat/completions
PROOFLOCK_AI_HABIT_PROOF_BACKUP_1_MODEL=backup-vision-model
PROOFLOCK_AI_HABIT_PROOF_BACKUP_2_URL=https://backup-two.example/v1/chat/completions
PROOFLOCK_AI_HABIT_PROOF_BACKUP_2_MODEL=backup-vision-model
PROOFLOCK_AI_TOKEN=server-only-secret
```

Use a secret manager in deployment. Do not put any of these values in the APK, GitHub repository, or client-visible configuration.

## Run locally

```sh
npm start
```
