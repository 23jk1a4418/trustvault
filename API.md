# API access matrix

All `/api/**` routes use HTTP Basic authentication except `/api/health`. The server is authoritative for every listed permission. Unauthorized authenticated requests return 403; missing records return 404.

| Method and path | Allowed caller | Behavior |
|---|---|---|
| `GET /api/health` | Public | Health status |
| `GET /api/me` | Any signed-in user | Current username and role |
| `GET /api/documents` | Signed-in users | Own documents plus documents with a read grant; supports `name`, `owner`, `category`, `from`, `to` filters |
| `POST /api/documents?category=...` | Owner, administrator | Upload a validated file (max 10 MiB) |
| `GET /api/documents/{id}/download` | Owner or current read grantee | Audited file download; denied attempts are audited |
| `PUT /api/documents/{id}` | Owner or current edit grantee | Rename document |
| `DELETE /api/documents/{id}` | Owner only | Delete document and grants |
| `GET /api/documents/{id}/permissions` | Owner only | List grants |
| `PUT /api/documents/{id}/permissions` | Owner only | Grant named user read and optional edit |
| `DELETE /api/documents/{id}/permissions/{username}` | Owner only | Revoke named user's grant |
| `GET /api/admin/audit` | Administrator | Read audit events |
| `GET /api/admin/documents` | Administrator | Read document metadata |

Upload multipart field is `file`. Allowed content types are PDF, PNG, JPEG, plain text, and DOCX. Permission request JSON: `{"username":"bob","read":true,"edit":false}`. Rename JSON: `{"name":"Updated name.pdf"}`.
