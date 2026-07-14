# what-is-my-public-server-ip

Jahia OSGi module that displays the public IP address of the Jahia server (as seen from the internet) in the admin panel. Admin UI at `/jahia/administration/whatIsMyPublicServerIp`.

## Key Facts

- **artifactId**: `what-is-my-public-server-ip` | **version**: `2.0.5-SNAPSHOT`
- **Java package**: `org.jahia.community.whatismypublicserverid`
- **jahia-depends**: `serverSettings,graphql-dxm-provider,default`
- Query-only module — no mutation, no persistence

## Architecture

| Class | Role |
|-------|------|
| `PublicIp` | `Serializable`; opens `https://checkip.amazonaws.com`, reads up to 64 chars, trims, and validates the payload is an IP literal (rejects anything else). Result cached in a process-wide static for 60s |
| `WhatIsMyPublicServerIpQueryExtension` | GraphQL query extension |
| `WhatIsMyPublicServerIpGraphQLExtensionsProvider` | Registers the query with the DXM provider |

`PublicIp.get()` returns `"unknown"` on `IOException` (no exception propagation).

## GraphQL API

| Operation | Name | Notes |
|-----------|------|-------|
| Query | `whatIsMyPublicServerIp { ip }` → object with `ip: String` | Namespace container (`WhatIsMyPublicServerIpQuery`) exposing the `ip` field. Backed by a 60s process-wide cache, so repeated calls within the window do not re-hit `checkip.amazonaws.com` |

Requires the `publicServerIpAdmin` permission (`@GraphQLRequiresPermission("publicServerIpAdmin")`, resolved as a root-node ACL check). The shipped `what-is-my-public-server-ip-administrator` role (`src/main/import/roles.xml`) grants exactly that permission (+ `administrationAccess`).

```graphql
query {
  whatIsMyPublicServerIp {
    ip
  }
}
```

Note: the old flat `whatIsMyPublicServerIp` → String form no longer exists — the query now returns a nested object and the flat selection would fail schema validation.

## Admin UI

- **Route**: `administration-server-configuration:10`
- **Admin path**: `/jahia/administration/whatIsMyPublicServerIp`
- The component calls the query on mount and displays the returned IP string
- Simple read-only display — no form, no save button

## Build

```bash
mvn clean install
yarn build
yarn lint
```

## Tests (Cypress Docker)

```bash
cd tests
cp .env.example .env
yarn install
./ci.build.sh && ./ci.startup.sh
```

- Specs under `tests/cypress/e2e/`:
  - `01-whatIsMyPublicServerIp.cy.ts` — nested GraphQL query returns a valid IP (`data.whatIsMyPublicServerIp.ip`), admin UI renders the IP card, and UI value matches the API
  - `02-whatIsMyPublicServerIp-Permissions.cy.ts` — `publicServerIpAdmin` enforcement: a user WITHOUT the permission is DENIED (GraphQL + hidden admin panel); a user granted ONLY the shipped `what-is-my-public-server-ip-administrator` role (never `admin`) is ALLOWED
  - `03-whatIsMyPublicServerIp-NetworkOnly.cy.ts` — the admin component's `fetchPolicy: 'network-only'` fires the query on every mount
- `graphql-dxm-provider` + `serverSettings` come in via `jahia-depends`; `assets/provisioning.yml` is a log-only stub

## Gotchas

- `checkip.amazonaws.com` is an external service — if the Jahia server has no internet egress (air-gapped / firewalled environments), the query returns `"unknown"`
- Server-side, the IP is cached in a process-wide static for **60s** (`CACHE_TTL_MS`), so repeated queries within the window are served from cache and do not re-hit `checkip.amazonaws.com`. The admin UI uses Apollo `fetchPolicy: 'network-only'`, so it always issues a GraphQL round-trip on mount, but the server cache still shields the upstream service. The cache write is lock-free (two `volatile` fields); a concurrent cold-cache access may perform more than one upstream fetch, but the race is benign — every caller still gets a valid, validated IP
- In Docker test environments, the returned IP is the host's NAT/bridge IP, not the public internet IP — tests should assert the value is non-empty rather than checking the exact IP
