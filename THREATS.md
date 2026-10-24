# Threat register

This is a challenge starter threat assessment, not a claim that the demo is production-hardened. Revisit each item after deployment and add tests for every security decision.

| Threat | Mitigation in this build | Accepted gap / next step |
|---|---|---|
| Guessing a document UUID or calling download directly | `DocumentAccessService.requireRead` checks owner or a live named grant before the file is read; authorization is server-side on every request. | Add HTTP-level tests for direct downloads and all verbs, not only service tests. |
| Revoked user reuses an old link | No bearer download links are issued; each download re-reads the grant table. | Concurrent revoke/download has ordinary request race semantics; define linearization and test it under load. |
| Viewer changes/deletes file or grants self permission | Edit requires an edit grant; delete and grant/revoke require ownership. | Expand negative endpoint tests for every role and action. |
| Unauthorized access attempts disappear from evidence | Denied document access calls the audit service before returning 403. | Ensure every denial path, including list, metadata, and permission endpoints, is recorded consistently. |
| Audit rows are edited or removed by a database operator | Each event hashes the previous event hash, actor, action, document, details, and timestamp. | Chain is not anchored outside the same database; a privileged operator can rewrite it. Export signed checkpoints to WORM/object-lock storage and add chain verification. Hash construction should use canonical structured encoding and a secret/keyed signature in production. |
| Stored files are read from the host or backup | Files are outside the web root and filenames use random UUIDs; app routes require authorization. | At-rest encryption, isolated storage identity, backup encryption, and separate storage permissions are not implemented. |
| Malicious upload, path traversal, oversized request | 10 MiB cap, MIME allowlist, generated storage key, basename-only original display name. | MIME is supplied by client and is not content-sniffed or malware scanned. Add magic-byte validation, antivirus, and archive bomb defenses. |
| Credential interception or brute force | BCrypt hashes; require HTTPS at deployment. | Basic auth is used for demo simplicity; no rate limiting, lockout, session lifecycle, MFA, or account provisioning. Move to short-lived secure sessions/OIDC and throttle login. |
| Cross-site request forgery / browser credential reuse | No cookie session; browser sends Basic credentials for requests. | CSRF is disabled. Deploy with strict CORS, HTTPS, CSP, and assess browser credential behavior; prefer a hardened session/OIDC model. |
| Database/storage failure leaves inconsistent deletion | Application removes file then metadata/grants. | Not transactional across DB and filesystem; implement soft delete/outbox/recovery and reconcile orphan files. |
| Admin becomes a universal data reader | Admin can inspect metadata and audit but download still requires owner/read grant. | Add explicit tests for admin direct download denial and audit access controls. |
| Race conditions undermine audit chain ordering | Local synchronized writer serializes one application instance. | Synchronization is not cluster-wide; use DB locking/sequence or dedicated append-only audit service before scaling. |

## Deliberately omitted

No groups, folder inheritance, external guest links, document versioning, recovery, retention engine, encryption-at-rest key management, malware service, or production identity provider. The current small scope keeps grants explicit and testable. Add these only with a reviewed authorization and audit design.
