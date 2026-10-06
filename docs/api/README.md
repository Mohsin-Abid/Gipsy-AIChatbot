# AI Tutor — API reference

What the Android app calls, and what it expects back. Nothing about how the backend produces an
answer — which model it uses, how it prompts, how it stores things — is the app's business and none
of it appears here.

[`openapi.yaml`](./openapi.yaml) beside this file is the same contract in machine-readable form.
Import it into Postman to try calls, or hand it to your backend developer as the spec to build to.

**Base URL** `https://api.<your-domain>/v1`
**Auth** `Authorization: Bearer <accessToken>` on every call except `/auth/google` and `/auth/refresh`
**Bodies** JSON, camelCase fields, SCREAMING_SNAKE_CASE enums
**Times** RFC 3339 UTC strings — `"2026-10-03T14:21:05Z"`

---

## Auth

### `POST /auth/google` — sign in

The app gets a Google ID token from Credential Manager and sends it here. The backend returns its
own tokens; the Google token is never used again.

```jsonc
// request
{ "idToken": "eyJhbGciOi..." }

// response 200
{
  "accessToken": "...",
  "refreshToken": "...",
  "expiresIn": 3600,
  "tokenType": "Bearer",
  "isNewUser": true,          // app runs profile setup when true, goes to Home when false
  "user": { /* the Me object below */ }
}
```

### `POST /auth/refresh` — get a new access token

Called when any request returns 401. Send the refresh token, get a fresh pair.

```jsonc
// request
{ "refreshToken": "..." }

// response 200
{ "accessToken": "...", "refreshToken": "...", "expiresIn": 3600, "tokenType": "Bearer" }
```

If this also fails, the app clears the session and shows the sign-in screen.

### `POST /auth/logout` — sign out

```jsonc
{ "refreshToken": "..." }      // → 204 No Content
```

### `DELETE /auth/account` — delete the account

No body. Google Play requires this for any app with in-app sign-up.

```jsonc
// response 200
{ "deleted": true, "activeSubscriptionRemains": false }
```

`activeSubscriptionRemains` tells the app to warn the user they still need to cancel in Play.

---

## User

### `GET /me` — everything the app needs on launch

One call after sign-in and on every cold start.

```jsonc
// response 200
{
  "userId": "usr_8f2",
  "email": "student@example.com",
  "displayName": "Mohsin",         // from the Google account, may be null
  "photoUrl": "https://...",       // may be null
  "profile": {
    "name": "Mohsin",
    "level": "SCHOOL",
    "grade": "GRADE_10",
    "subjects": ["MATH", "PHYSICS"],
    "goals": ["ACE_EXAMS"],
    "dailyTime": "HOUR_1",
    "reminderEnabled": true
  },
  "settings": {
    "language": "en",
    "remindersEnabled": true,
    "voiceInputEnabled": true,
    "themeMode": "SYSTEM"
  },
  "entitlement": { "plan": "FREE", "productId": null, "expiresAt": null },
  "quota": { "unlimited": false, "limit": 15, "remaining": 11, "resetsAt": "2026-10-04T00:00:00Z" },
  "createdAt": "2026-10-01T09:14:00Z"
}
```

### `PATCH /me/profile` — save the setup answers

Send only the fields that changed. Returns the updated profile.

```jsonc
{ "name": "Mohsin", "level": "SCHOOL", "grade": "GRADE_10",
  "subjects": ["MATH", "PHYSICS"], "goals": ["ACE_EXAMS"], "dailyTime": "HOUR_1" }
```

### `PATCH /me/settings` — language, reminders, voice, theme

```jsonc
{ "language": "ur", "remindersEnabled": false, "voiceInputEnabled": true, "themeMode": "DARK" }
```

### Enum values

These mirror the app's Kotlin enums exactly. Send and expect these strings:

| | |
|---|---|
| `level` | `SCHOOL` `COLLEGE` `UNIVERSITY` |
| `grade` | `GRADES_1_5` `GRADE_6` `GRADE_7` `GRADE_8` `GRADE_9` `GRADE_10` `GRADE_11` `GRADE_12` |
| `subjects` | `MATH` `PHYSICS` `CHEMISTRY` `BIOLOGY` `ENGLISH` `HISTORY` `GEOGRAPHY` `COMPUTER_SCIENCE` `ECONOMICS` `LANGUAGES` `ART` `OTHER` |
| `goals` | `ACE_EXAMS` `FINISH_HOMEWORK` `UNDERSTAND_TOPICS` `IMPROVE_WRITING` — max 2 |
| `dailyTime` | `MINUTES_15` `MINUTES_30` `HOUR_1` `HOURS_2_PLUS` |
| `themeMode` | `LIGHT` `DARK` `SYSTEM` |
| `language` | `en` `es` `fr` `de` `pt` `tr` `id` `hi` `ur` `ar` `bn` `zh` |
| `tool` | `CHATBOT` `HOMEWORK_SOLVER` `MATH_SOLVER` `NOTES_SUMMARIZER` `AI_TUTOR` `GRAMMAR_FIXER` `ESSAY_WRITER` `PARAPHRASING` `SUMMARY_GENERATOR` `CONCEPT_EXPLAINER` |
| `attachment.source` | `CAMERA` `GALLERY` `PDF` `WORD` |

---

## Chats

### `POST /chats` — start a conversation

Called when a tool is tapped, before the first question.

```jsonc
// request
{ "tool": "MATH_SOLVER", "title": "Algebra help", "subject": "Mathematics" }

// response 201
{ "chatId": "cht_4a1", "tool": "MATH_SOLVER", "title": "Algebra help", "subject": "Mathematics",
  "createdAt": "...", "updatedAt": "...", "messageCount": 0 }
```

`subject` is free text (a display label), not one of the `subjects` enum values.

### `GET /chats?cursor=&limit=20` — the History screen

```jsonc
// response 200
{
  "items": [ { "chatId": "cht_4a1", "tool": "MATH_SOLVER", "title": "Algebra help",
               "subject": "Mathematics", "updatedAt": "...", "messageCount": 6 } ],
  "nextCursor": "eyJpZCI6..."     // null on the last page
}
```

Pass `nextCursor` back as `cursor` for the next page.

### `GET /chats/{chatId}` — open a conversation

```jsonc
// response 200
{ "chat": { /* as above */ }, "messages": [ /* oldest → newest */ ], "nextCursor": null }
```

### `GET /chats/{chatId}/messages?cursor=` — scroll back for older messages

Same shape as the chat list: `{ "items": [...], "nextCursor": ... }`.

### `PATCH /chats/{chatId}` — rename

```jsonc
{ "title": "Quadratics" }
```

### `DELETE /chats/{chatId}` — delete the conversation
### `DELETE /chats/{chatId}/messages` — clear the messages, keep the chat

Both return 204.

---

## Asking a question

### `POST /chats/{chatId}/messages`

**The main call.** `multipart/form-data`, two parts — a JSON `payload` and an optional `file`.

```
POST /v1/chats/cht_4a1/messages
Authorization: Bearer <accessToken>
Idempotency-Key: 9f1c3e7a-...
Content-Type: multipart/form-data; boundary=X

--X
Content-Disposition: form-data; name="payload"
Content-Type: application/json

{"question":"Solve question 3 on this page",
 "tool":"MATH_SOLVER",
 "attachment":{"source":"CAMERA","fileName":"capture.jpg",
               "mimeType":"image/jpeg","sizeBytes":812344}}
--X
Content-Disposition: form-data; name="file"; filename="capture.jpg"
Content-Type: image/jpeg

<bytes>
--X--
```

Omit the `file` part and the `attachment` field for a text-only question.

```jsonc
// response 201
{
  "userMessage": { "messageId": "msg_7f1", "chatId": "cht_4a1", "role": "USER",
                   "text": "Solve question 3 on this page",
                   "attachment": { "source": "CAMERA", "fileName": "capture.jpg",
                                   "mimeType": "image/jpeg", "sizeBytes": 812344 },
                   "createdAt": "..." },
  "answer": {
    "messageId": "msg_7f2", "chatId": "cht_4a1", "role": "ASSISTANT", "createdAt": "...",
    "blocks": [
      { "type": "PARAGRAPH", "text": "Start by isolating the variable." },
      { "type": "BULLET", "term": "Subtract 5", "text": "Both sides, giving 2x = 12." },
      { "type": "BULLET", "term": "Divide by 2", "text": "x = 6." }
    ]
  },
  "quota": { "unlimited": false, "limit": 15, "remaining": 11, "resetsAt": "..." }
}
```

**`blocks` is the answer format — plain text, not Markdown.** Two shapes only:

- `{ "type": "PARAGRAPH", "text": "..." }`
- `{ "type": "BULLET", "term": "...", "text": "..." }` — `term` is the bolded lead-in

The app renders these directly and has no Markdown parser, so `**bold**` would show as literal
asterisks.

**File limits**, matching what the app enforces before uploading:

| | |
|---|---|
| Max size | 20 MB |
| Accepted | `image/jpeg`, `application/pdf`, `.docx` |
| Images | always JPEG, already downscaled to 2000px on the long edge |

**If the answer fails**, return `207` with the question stored and an error — the app keeps the
question in the thread and offers a retry, rather than showing a made-up answer:

```jsonc
{ "userMessage": { ... }, "error": { "code": "UPSTREAM_UNAVAILABLE", "message": "..." } }
```

### `POST /chats/{chatId}/messages/{messageId}/regenerate`

The Regenerate button. `messageId` is the **assistant** message to replace. No body; same response
shape as above.

### `GET /messages/{messageId}/attachment`

Downloads a message's file. Needed when a conversation is opened on a different device, where the
original file isn't on disk. Returns the bytes, or a redirect to a signed URL.

---

## Feedback

### `POST /messages/{messageId}/feedback` — thumbs up / down

```jsonc
{ "rating": "HELPFUL" }        // or "NOT_HELPFUL" → 204
```

Sending the same rating again clears it, so the buttons toggle.

### `POST /messages/{messageId}/report` — the Flag action

```jsonc
{ "reason": "INCORRECT", "detail": "..." }    // INCORRECT | HARMFUL | OFF_TOPIC | OTHER → 204
```

---

## Billing

The app talks to Google Play for the purchase itself, then sends the token here. **The app only ever
reads entitlement — it never sets it.**

### `POST /billing/play/verify` — after a successful purchase

```jsonc
// request
{ "purchaseToken": "...", "productId": "premium_yearly" }   // or "premium_monthly"

// response 200
{ "plan": "PRO", "productId": "premium_yearly",
  "expiresAt": "2027-10-03T00:00:00Z", "autoRenewing": true, "inGracePeriod": false }
```

### `POST /billing/restore` — the Restore purchases button

```jsonc
{ "purchaseTokens": ["...", "..."] }    // → the same entitlement object
```

### `GET /billing/entitlement` — re-check the plan

Returns the same object. Also included in `GET /me`, so this is only for a quick re-check after
buying.

---

## Quota

Free accounts have a daily question limit. It comes back on `GET /me` and on every answer:

```jsonc
{ "unlimited": false, "limit": 15, "remaining": 11, "resetsAt": "2026-10-04T00:00:00Z" }
```

For a Pro account, `unlimited` is `true` and the other three are `null`. The app displays
`remaining` and opens the paywall on `QUOTA_EXCEEDED`. Pick whatever `limit` suits you — the app
reads it from the response rather than hardcoding a number.

---

## Errors

Every failure uses the same envelope:

```jsonc
{
  "error": { "code": "QUOTA_EXCEEDED", "message": "Daily free limit reached.",
             "details": { "resetsAt": "2026-10-04T00:00:00Z" } },
  "requestId": "req_01JAV8K2M7"
}
```

The app switches on `code` and never reads `message` — it has its own translated strings. Keep
`message` in English for logs.

| Code | HTTP | What the app does |
|---|---|---|
| `UNAUTHENTICATED` | 401 | Refresh the token, retry once |
| `TOKEN_EXPIRED` | 401 | Refresh the token, retry once |
| `INVALID_GOOGLE_TOKEN` | 401 | Back to the sign-in screen |
| `REFRESH_TOKEN_REUSED` | 401 | Sign out |
| `FORBIDDEN` | 403 | Generic error |
| `NOT_FOUND` | 404 | Drop it from the local cache |
| `VALIDATION_FAILED` | 400 | Generic error |
| `QUOTA_EXCEEDED` | 429 | **Open the Pro paywall** |
| `RATE_LIMITED` | 429 | Back off, honour `Retry-After` |
| `FILE_TOO_LARGE` | 413 | "That file is larger than 20 MB" |
| `UNSUPPORTED_FILE_TYPE` | 415 | "Only PDF and .docx documents can be attached" |
| `FILE_UNREADABLE` | 422 | "That file could not be read" |
| `PAYMENT_VERIFICATION_FAILED` | 402 | Purchase error |
| `UPSTREAM_UNAVAILABLE` | 503 | "That didn't go through — try again" |
| `INTERNAL` | 500 | "That didn't go through — try again" |

`QUOTA_EXCEEDED` and `RATE_LIMITED` share a status but must stay separate codes — one sends the
user to the paywall, the other just asks them to wait.

---

## One setup note for the app side

Sign-in needs **two** Google OAuth client IDs: an **Android** one (package `com.aitutor.chatbot.app`
plus the SHA-1 of the debug and release keystores) and a **Web** one. The app sends the Web client
ID, and that is also what the backend checks the token against — so both sides need the same value.

---

## All endpoints

| Method | Path | |
|---|---|---|
| POST | `/auth/google` | Sign in with a Google ID token |
| POST | `/auth/refresh` | New access token |
| POST | `/auth/logout` | Sign out |
| DELETE | `/auth/account` | Delete the account |
| GET | `/me` | Profile, settings, entitlement, quota |
| PATCH | `/me/profile` | Save setup answers |
| PATCH | `/me/settings` | Language, reminders, voice, theme |
| GET | `/chats` | History list |
| POST | `/chats` | Start a conversation |
| GET | `/chats/{chatId}` | Open a conversation |
| PATCH | `/chats/{chatId}` | Rename |
| DELETE | `/chats/{chatId}` | Delete |
| GET | `/chats/{chatId}/messages` | Older messages |
| POST | `/chats/{chatId}/messages` | **Ask a question** |
| DELETE | `/chats/{chatId}/messages` | Clear the thread |
| POST | `/chats/{chatId}/messages/{messageId}/regenerate` | Answer again |
| GET | `/messages/{messageId}/attachment` | Download a file |
| POST | `/messages/{messageId}/feedback` | Thumbs up / down |
| POST | `/messages/{messageId}/report` | Report an answer |
| POST | `/billing/play/verify` | Verify a purchase |
| POST | `/billing/restore` | Restore purchases |
| GET | `/billing/entitlement` | Current plan |
