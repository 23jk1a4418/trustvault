# AI prompt journal

Keep this journal as the work happens. Dates and commit links below must describe real work; do not backfill fictional prompt history. Session prompt source: user supplied the Snovasys Fresher Hiring Challenge 2026 TrustVault brief and asked for a clean React frontend and Java backend in the existing folder, with understandable code.

## 2026-10-22 — Initial application scaffold

- Asked: implement the supplied TrustVault challenge in the existing project using React for frontend and Java for backend, with clear files and folders.
- Produced: Spring Boot API, React workspace UI, per-document owner/grant checks, private file path, linked audit events, demo seed accounts, API/threat docs, and starter authorization tests.
- What needed correction/checking: first scaffold did not include functioning page components yet; the authorization rule had to live in backend services, not just the React page.
- What I did: added `DocumentAccessService` and routed downloads through it. Commit: pending; this folder has no Git repository or remote yet.

## 2026-10-22 — Authorization review: viewer role

- Asked: review the permission path for a viewer granted `edit=true`; verify the three role model holds even if a broad grant is accidentally stored.
- Produced: the first check allowed edits whenever a grant had its edit bit set, without also considering that the account was a viewer.
- What I did instead: made viewer role read-only in the service authorization decision and changed the test to deliberately create an edit grant for Bob, then assert edit is denied. Commit: pending; no repository history exists yet.

## 2026-10-22 — Audit integrity review

- Asked: inspect whether each event hash can be recomputed from the timestamp and fields actually stored in the audit row.
- Produced: the first implementation called `Instant.now()` separately for the hash input and entity timestamp, so the stored row did not contain the precise timestamp used for its digest.
- What I did instead: capture one `Instant` and use it for both the digest input and saved event. Documented that database-resident SHA-256 chaining still cannot stop a privileged operator from rewriting the full chain. Commit: pending; no repository history exists yet.

## 2026-10-22 — Backend test review

- Asked: run the authorization tests, then fix issues the tests expose rather than weakening assertions.
- Produced: the first run found that derived grant deletion had no active transaction, so revocation and cleanup failed with `TransactionRequiredException`.
- What I did instead: added transaction boundaries to grant delete repository methods and reran the suite. Then added HTTP tests for direct uninvited downloads, admin download denial, and the denial audit record. Commit: pending; no repository history exists yet.

## 2026-10-22 — Frontend build review

- Asked: build the React production bundle and fix compiler findings.
- Produced: Vite showed that the initial generated `App.jsx` lacked a default export and was incomplete after the sign-in view.
- What I did instead: rewrote the UI as small named components and reran `npm run build`; Vite completed successfully. Commit: pending; no repository history exists yet.

## Reusable context

- Keep authorization in backend service/controller paths; never treat hidden UI actions as access control.
- Check direct download and revocation behavior, and record denied attempts. AI suggestions must never grant access without an owner's confirmation.
- Threat entries should be mapped to executable tests and must state accepted gaps honestly.

Continue this file as the project changes. The entries above summarize real checks and fixes from the initial implementation session. Add new entries on the actual days work happens, link actual corrective commits, and keep the Git history spread across the challenge week; the current folder has no Git remote and these same-day entries do not satisfy the week-long history requirement yet.
