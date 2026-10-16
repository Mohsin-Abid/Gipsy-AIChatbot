# AI Tutor — API reference

What the Android app calls, and what it expects back. Nothing about how the backend produces an
answer — which model it uses, how it prompts, how it stores things — is the app's business and none
of it appears here.

[`openapi.yaml`](./openapi.yaml) beside this file is the same contract in machine-readable form.
Import it into Postman to try calls, or hand it to your backend developer as the spec to build to.
The published page "AI Tutor API Plan" shows the same eleven endpoints in a reading layout.

**Base URL** `https://api.<your-domain>/v1`
**Auth** `Authorization: Bearer <Firebase ID token>` on every call except `GET /config`
**Bodies** JSON, camelCase fields, SCREAMING_SNAKE_CASE enums
**Times** RFC 3339 UTC strings — `"2026-10-03T14:21:05Z"`

**Changes since the 7 October plan.** Sign-in moved to Firebase, so `/auth/google`, `/auth/refresh`
and `/auth/logout` are gone, and account deletion is now `DELETE /me`. The Daily challenge endpoint,
`challengeId` and "Clear messages" (`?messagesOnly`) are gone because the app no longer has them. The
user id is the Firebase `uid`, and "Skip for now" creates an anonymous account. 15 endpoints became 11.

---

## Sign-in is not an endpoint

Firebase signs people in, so this API has no login endpoints. It only checks the token on each call.

- **Every call carries the Firebase ID token.** `Authorization: Bearer <ID token>` on every endpoint
  except `GET /config`. The token lasts one hour and the Firebase library in the app renews it, so
  there is no refresh endpoint.
- **Check it on your server.** Use the Firebase Admin SDK, or verify the JWT yourself: the signature
  against Google's public keys, `iss` equal to `https://securetoken.google.com/<firebase-project-id>`,
  `aud` equal to `<firebase-project-id>`, and not expired. The `sub` claim is the user id (the Firebase
  `uid`). It is a string of up to 128 characters, so do not assume a prefix or a format.
- **There is no sign-up call.** The first request from a new `uid` creates the account, and
  `GET /me` returns `setupCompleted: false`.
- **A 401 means one fresh token.** The app asks Firebase for a new ID token and tries the call again. A
  second 401 signs the person out and shows the sign-in screen.
- **There are two kinds of account.** "Continue with Google" gives a token with `email`, `name` and
  `picture`. "Skip for now" gives an anonymous account: a real `uid` and a valid token, but no email or
  name. The token's `firebase.sign_in_provider` says which one: `google.com` or `anonymous`.
- **Skipping, then signing in later, keeps the account.** The app links the Google account to the
  anonymous one, so the `uid` stays the same and nothing is lost. If that Google account already exists
  from another phone, the app switches to it and the `uid` changes. The chats of the skipped account
  are not merged. Logging out of an anonymous account abandons it for good.

First launch, in order:

```
App → GET /config                      required version, legal links
App → Google / Firebase                Sign in with Google, or Skip for now → Firebase ID token
App → GET /me                          profile, plan, quota, setupCompleted
App → PATCH /me                        only if setupCompleted is false: the setup answers
App → POST /chat                       first question
```

When `setupCompleted` is true the app goes straight to Home. That is how someone who signs in on a new
phone skips setup. When it is false the app opens setup with the name field filled in from the first
word of `displayName`. The person can change it or leave it empty, because the name is optional.

---

## 1–3 · Account and profile

### 1. `GET /me` — everything the app needs

Called on every cold start and right after sign-in. The first call for a new `uid` creates the account.

```jsonc
// response 200
{
  "userId": "kQ3xZ7r1mTb0a9L2sVwYhN4eUoD3",   // the Firebase uid
  "isAnonymous": false,                       // true for "Skip for now"
  "email": "alex@example.com",                // null when anonymous
  "displayName": "Alex Morgan",               // from Google, null when anonymous
  "photoUrl": "https://...",                  // from Google, can be null
  "setupCompleted": true,
  "profile": {
    "name": "Alex",                           // optional: may be an empty string
    "level": "COLLEGE",
    "grade": "GRADE_11",
    "subjects": ["MATH", "PHYSICS"],
    "goals": ["ACE_EXAMS"],
    "dailyTime": "MINUTES_30"
  },
  "settings": {
    "language": "en",
    "remindersEnabled": false,
    "themeMode": "SYSTEM",
    "voiceInputEnabled": true
  },
  "plan": { "plan": "FREE", "productId": null, "expiresAt": null, "autoRenewing": null },
  "quota": { "unlimited": false, "limit": 15, "remaining": 11, "resetsAt": "2026-10-04T00:00:00Z" }
}
```

`profile.name` is what the student typed in setup. It can differ from the Google `displayName`, and it
can be empty. The profile fields can be empty before setup is done.

### 2. `PATCH /me` — profile and settings

Send only what changed. The response is the full `/me` again.

```jsonc
{ "profile": { "name": "Sam" } }                    // the Name edit screen
{ "settings": { "language": "ur" } }                // Language

// the end of first-time setup: every answer, once
{
  "profile": { "name": "Alex", "level": "COLLEGE", "grade": "GRADE_11",
               "subjects": ["MATH", "OTHER"], "goals": ["ACE_EXAMS"], "dailyTime": "MINUTES_30" },
  "settings": { "remindersEnabled": false },
  "setupCompleted": true
}
```

More than two `goals` is a `VALIDATION_FAILED` error. The app caps it at two, so it should not normally
happen. `setupCompleted` is only ever sent as `true`.

### 3. `DELETE /me` — delete the account

No body. Google Play requires this for any app that creates accounts.

```jsonc
// response 200
{ "deleted": true, "activeSubscriptionRemains": false }
```

Removes the user, profile, chats, messages and files, and the Firebase account itself (Admin SDK
`deleteUser`). The app then signs out on the phone. It does not cancel a Play subscription, so
`activeSubscriptionRemains` tells the app to warn the user they still need to cancel in Play.

---

## 4 · Chat — one endpoint for every tool

### 4. `POST /chat`

The tool is a word in the request, so a new tool later is a new word on the backend and no new
endpoint. The app still needs an update to show a new tool's name and icon.

| Field | Required | What it is |
|---|---|---|
| `tool` | Yes | One of the ten tool words below. |
| `chatId` | No | Empty to start a new chat. The backend creates it, titles it from the first question (from the file name when only a file is sent), cut at 60 characters, and returns the id. The app sends nothing until a first question exists, so opening a tool and leaving creates no chat. |
| `message` | Yes, unless a file is attached | The question, 1 to 8000 characters. |
| `file` | No | A JPEG, PDF or .docx up to 20 MB. When present the request is `multipart/form-data` and the JSON goes in a part named `payload`. |
| `regenerateMessageId` | No | The answer to replace, for the Regenerate button. `message` can be empty. |
| `Idempotency-Key` header | Yes | A fresh UUID per question, reused if the app retries. The same key must return the same answer and count once. |

```jsonc
// text only: a JSON body
{ "tool": "MATH_SOLVER", "chatId": null, "message": "Solve 2x + 5 = 17" }

// response 201
{
  "chatId": "cht_4a1",
  "userMessage": { "messageId": "msg_7f1", "text": "Solve 2x + 5 = 17" },
  "answer": {
    "messageId": "msg_7f2",
    "blocks": [
      { "type": "PARAGRAPH", "text": "Isolate the variable first." },
      { "type": "BULLET", "term": "Subtract 5", "text": "Both sides: 2x = 12." },
      { "type": "BULLET", "term": "Divide by 2", "text": "x = 6." }
    ]
  },
  "quota": { "unlimited": false, "limit": 15, "remaining": 11, "resetsAt": "2026-10-04T00:00:00Z" }
}
```

With a file, the same fields go in a JSON `payload` part and the bytes in a `file` part:

```
POST /v1/chat
Authorization: Bearer <Firebase ID token>
Idempotency-Key: 9f1c3e7a-...
Content-Type: multipart/form-data; boundary=X

--X
Content-Disposition: form-data; name="payload"
Content-Type: application/json

{"tool":"MATH_SOLVER","chatId":"cht_4a1","message":"Solve question 3 on this page"}
--X
Content-Disposition: form-data; name="file"; filename="capture.jpg"
Content-Type: image/jpeg

<bytes>
--X--
```

**The ten tool words**

| Word | Name in the app | History filter |
|---|---|---|
| `CHATBOT` | AI Chatbot | Chats |
| `HOMEWORK_SOLVER` | Homework Solver | Solved |
| `MATH_SOLVER` | Math Solver | Solved |
| `NOTES_SUMMARIZER` | Notes Summarizer | Summaries |
| `AI_TUTOR` | AI Tutor | Only under All |
| `GRAMMAR_FIXER` | Grammar Fixer | Writing |
| `ESSAY_WRITER` | Essay Writer | Writing |
| `PARAPHRASING` | Paraphrasing | Writing |
| `SUMMARY_GENERATOR` | Summary Generator | Summaries |
| `CONCEPT_EXPLAINER` | Concept Explainer | Chats |

**What the backend has to do**

- Remember the conversation. The app never sends earlier messages, only the new one.
- Answer in the user's saved language (`settings.language`) and at their level (`profile.level` and
  `profile.grade`).
- **Return answers as `blocks` of plain text, not Markdown.** The app has no Markdown renderer, so
  `**bold**` would show as asterisks. `PARAGRAPH` has `text`; `BULLET` has `term` (the bold lead-in) and
  `text`.
- Count one question against the daily quota for each answer made. Regenerating counts. A failed
  answer does not.
- When a file is attached, read it as part of the question. The app does not read files itself.

**File limits**, matching what the app enforces before uploading:

| | |
|---|---|
| Max size | 20 MB |
| Accepted | `image/jpeg`, `application/pdf`, `.docx` |
| Images | always JPEG, already downscaled to 2000px on the long edge |

**If no answer can be made**, return `207` with the question stored. The app keeps the question in the
thread and offers a retry, and never shows a made-up answer. No quota is used.

```jsonc
{ "userMessage": { ... }, "error": { "code": "UPSTREAM_UNAVAILABLE", "message": "..." } }
```

---

## 5–8 · History

### 5. `GET /chats` — the History list

Newest first by last activity. Home calls it with `?limit=1` for "Continue learning" and `?limit=3` for
"Recent activity".

| Query | |
|---|---|
| `cursor` | The `nextCursor` from the last page. Leave out for the first page. |
| `limit` | 1 to 50. Default 20. |
| `q` | Search text. Matches chat titles and message text. |
| `category` | `ALL`, `CHATS`, `SOLVED`, `SUMMARIES` or `WRITING`. Which tools belong to each is in the table above. |

```jsonc
{ "items": [ { "chatId": "cht_4a1", "tool": "MATH_SOLVER", "title": "Solve 2x + 5 = 17",
               "subject": "Mathematics", "updatedAt": "2026-10-05T09:12:00Z", "messageCount": 6 } ],
  "nextCursor": "eyJpZCI6..." }          // null on the last page
```

### 6. `GET /chats/{id}` — open a conversation

Messages come oldest to newest. For older ones, pass `?before=` with the `nextCursor`. File links
expire after about ten minutes, so the app fetches the chat again to get fresh ones.

```jsonc
{
  "chat": { "chatId": "cht_4a1", "tool": "MATH_SOLVER", "title": "Solve 2x + 5 = 17",
            "subject": "Mathematics", "updatedAt": "2026-10-05T09:12:00Z", "messageCount": 2 },
  "messages": [
    { "messageId": "msg_7f1", "role": "USER", "text": "Solve question 3",
      "attachment": { "fileName": "capture.jpg", "mimeType": "image/jpeg",
                      "sizeBytes": 812344, "url": "https://files.../capture.jpg?sig=..." },
      "createdAt": "2026-10-05T09:11:40Z" },
    { "messageId": "msg_7f2", "role": "ASSISTANT",
      "blocks": [ { "type": "PARAGRAPH", "text": "..." } ],
      "rating": "HELPFUL", "createdAt": "2026-10-05T09:12:00Z" }
  ],
  "nextCursor": null
}
```

### 7. `PATCH /chats/{id}` — rename

```jsonc
{ "title": "Quadratics" }       // → the updated chat item, as in the list
```

One to 100 characters. The app sends at most 61: up to 60 typed, or a 60-character cut of the first
question with an ellipsis.

### 8. `DELETE /chats/{id}` — delete the conversation

Deletes the chat, its messages and their files. The app asks the person to confirm first. Returns 204.

---

## 9 · Feedback

### 9. `POST /messages/{id}/feedback`

`{id}` is the answer's `messageId`. Send a rating, a report, or both. `NONE` clears an earlier rating,
so the thumb buttons can switch off.

```jsonc
{ "rating": "NOT_HELPFUL" }                       // HELPFUL | NOT_HELPFUL | NONE
{ "report": { "reason": "INCORRECT", "detail": "Step 2 is wrong" } }
                                                  // INCORRECT | HARMFUL | OFF_TOPIC | OTHER
```

Returns 204.

---

## 10 · Billing

The app talks to Google Play for the purchase itself, then sends the token here. **The app only ever
reads the plan. It never sets it.** Your backend checks each token with Google Play and is the only
thing that can grant Pro.

### 10. `POST /billing/verify`

After a purchase, send that one purchase. For "Restore purchase", send every active one. Needs the
`Idempotency-Key` header. Product IDs are `premium_yearly` and `premium_monthly`.

```jsonc
// request
{ "purchases": [ { "productId": "premium_yearly", "purchaseToken": "..." } ] }

// response 200
{ "plan": "PRO", "productId": "premium_yearly", "expiresAt": "2027-10-05T00:00:00Z", "autoRenewing": true }
```

A failed check is `PAYMENT_VERIFICATION_FAILED` (402).

---

## 11 · App content

### 11. `GET /config` — no login needed

If the installed version is lower than `minAppVersion`, the app shows an update screen. This is the
only way to stop old app versions from using a changed API. The app works without it, so keep it only
if you want that control. The Privacy and Help links on Profile come from `urls`.

```jsonc
{
  "minAppVersion": "1.0.0",
  "latestAppVersion": "1.2.0",
  "maintenance": { "active": false, "message": null },
  "urls": { "terms": "https://...", "privacy": "https://...", "support": "https://...",
            "accountDeletion": "https://..." },
  "freeDailyLimit": 15,
  "tools": [ { "tool": "MATH_SOLVER", "enabled": true, "isNew": false } ]
}
```

---

## Values the app and backend share

These are the exact words the app sends and expects. They match its code.

| | |
|---|---|
| `level` | `SCHOOL` `COLLEGE` `UNIVERSITY` |
| `grade` | `GRADES_1_5` `GRADE_6` `GRADE_7` `GRADE_8` `GRADE_9` `GRADE_10` `GRADE_11` `GRADE_12` `OTHER` |
| `subjects` | `MATH` `PHYSICS` `CHEMISTRY` `BIOLOGY` `ENGLISH` `HISTORY` `GEOGRAPHY` `COMPUTER_SCIENCE` `ECONOMICS` `LANGUAGES` `ART` `OTHER` |
| `goals` | `ACE_EXAMS` `FINISH_HOMEWORK` `UNDERSTAND_TOPICS` `IMPROVE_WRITING` — at most 2 |
| `dailyTime` | `MINUTES_15` `MINUTES_30` `HOUR_1` `HOURS_2_PLUS` |
| `themeMode` | `LIGHT` `DARK` `SYSTEM` |
| `language` | `en` `es` `fr` `de` `pt` `tr` `id` `hi` `ur` `ar` `bn` `zh` — also the language of AI answers |
| `plan` | `FREE` `PRO` |

`subject` on a chat is free text (a display label), not one of the `subjects` values.

### Quota

Free accounts have a daily question limit. It comes back on `GET /me` and on every answer. For a Pro
account `unlimited` is `true` and the other three fields are `null`. The app displays `remaining` and
opens the Pro screen on `QUOTA_EXCEEDED`. Pick whatever `limit` suits you — the app reads it from the
response rather than hardcoding a number.

---

## Errors

Every failure has the same shape. The app reads `code` and ignores `message`, so keep `message` in
English for your logs. `requestId` helps when someone reports a problem.

```jsonc
{
  "error": { "code": "QUOTA_EXCEEDED", "message": "Daily free limit reached.",
             "details": { "resetsAt": "2026-10-04T00:00:00Z" } },
  "requestId": "req_01JAV8K2M7"
}
```

| Status | Code | What the app does |
|---|---|---|
| 401 | `UNAUTHENTICATED`, `TOKEN_EXPIRED` | Asks Firebase for a fresh ID token and tries again, once |
| 401 | `INVALID_ID_TOKEN` | The token failed your check, even when fresh. Signs out and shows the sign-in screen |
| 404 | `NOT_FOUND` | Removes the item from the screen |
| 400 | `VALIDATION_FAILED`, `UNKNOWN_TOOL` | Shows a general error |
| 429 | `QUOTA_EXCEEDED` | **Opens the Pro screen** |
| 429 | `RATE_LIMITED` | Waits, using the `Retry-After` header |
| 413 | `FILE_TOO_LARGE` | "That file is larger than 20 MB" |
| 415 | `UNSUPPORTED_FILE_TYPE` | "Only PDF and .docx documents can be attached" |
| 422 | `FILE_UNREADABLE` | "That file could not be read" |
| 402 | `PAYMENT_VERIFICATION_FAILED` | Shows a purchase error |
| 503 / 500 | `UPSTREAM_UNAVAILABLE`, `INTERNAL` | "That didn't go through — try again" |

`QUOTA_EXCEEDED` and `RATE_LIMITED` share a status but must stay separate codes: one sends the person
to the Pro screen, the other just asks them to wait.

---

## What is not an API

These need no endpoint.

- **Signing in and out, renewing the token.** Firebase does all three.
- **Voice to text.** Runs on the phone. The audio never leaves it.
- **Read aloud.** Uses the phone's own speech engine.
- **Copy, select text, text size, Share chat.** Local actions. Text size (A− and A+) is saved on the
  phone. Share opens Android's share sheet with the conversation as plain text.
- **New chat.** Nothing exists until the first question is sent. `POST /chat` with no `chatId` then
  creates it.
- **Daily reminder.** A notification scheduled on the phone.
- **Onboarding, tool names and icons, Home ask bar and chips.** Shipped inside the app. A chip picks a
  tool and puts a question in the composer. Sending it is a normal `POST /chat`.
- **Prices.** Google Play supplies them. The backend only verifies purchases.
- **Rate the app.** Opens the Play Store page.
- **Picking a file.** The file travels inside `POST /chat`.
- **Downloading old attachments.** Each message carries a short-lived file link.
- **Study statistics.** The streak and question counts were removed from the screens. A `GET /stats`
  would be needed only if they return.
- **Removed from the app.** The Daily challenge card, "Clear messages" and the notification bell. None
  of them has an endpoint.

---

## Setup the app side needs

- In the Firebase console, turn on the **Google** and **Anonymous** sign-in providers. "Skip for now"
  fails without Anonymous.
- Add the **SHA-1 and SHA-256** fingerprints of the debug and release keystores to the Android app
  (package `com.aitutor.chatbot.app`), then download `google-services.json` again. Without the
  fingerprints Google sign-in finds no account to offer.
- The backend needs the Firebase **project ID** to check tokens, and a service account if it uses the
  Admin SDK. Keep that key on the server only.

---

## Decisions still open

1. **One chat endpoint, four history endpoints.** Recommended: keep it.
2. **"Skip for now" accounts.** Recommended: let them chat on the free quota, but require Google sign-in
   before they can buy Pro, because a purchase on an anonymous account is lost when the person logs out.
   That means `POST /billing/verify` answers `403 ACCOUNT_REQUIRED` for an anonymous token. The app does
   not handle that code yet. Also decide whether to merge a skipped account's chats when the person later
   signs in to a Google account that already exists. The app does not merge today.
3. **Theme and voice-input switches.** They are in `PATCH /me` now. They are per-device by nature.
   Recommended: either works; local is simpler.
4. **`GET /config`.** The only way to force an update on old app versions. Recommended: keep it.

---

## All endpoints

| # | Method | Path | |
|---|---|---|---|
| 1 | GET | `/me` | Profile, settings, plan, quota. Creates the account on first call |
| 2 | PATCH | `/me` | Profile and settings |
| 3 | DELETE | `/me` | Delete the account |
| 4 | POST | `/chat` | **Ask a question, any tool** |
| 5 | GET | `/chats` | History list |
| 6 | GET | `/chats/{id}` | Open a conversation |
| 7 | PATCH | `/chats/{id}` | Rename |
| 8 | DELETE | `/chats/{id}` | Delete |
| 9 | POST | `/messages/{id}/feedback` | Rate or report an answer |
| 10 | POST | `/billing/verify` | Verify a purchase, or restore |
| 11 | GET | `/config` | Version gate, legal links, tools |
