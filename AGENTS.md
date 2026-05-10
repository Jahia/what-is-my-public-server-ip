# what-is-my-public-server-ip

Jahia OSGi module that displays the public IP address of the Jahia server (as seen from the internet) in the admin panel. Admin UI at `/jahia/administration/whatIsMyPublicServerIp`.

## Key Facts

- **artifactId**: `what-is-my-public-server-ip` | **version**: `2.0.3-SNAPSHOT`
- **Java package**: `org.jahia.modules.whatismypublicserverid`
- **jahia-depends**: `serverSettings,graphql-dxm-provider,default`
- Query-only module — no mutation, no persistence

## Architecture

| Class | Role |
|-------|------|
| `PublicIp` | `Serializable`; opens `http://checkip.amazonaws.com` and reads the first line |
| `WhatIsMyPublicServerIpQueryExtension` | GraphQL query extension |
| `WhatIsMyPublicServerIpGraphQLExtensionsProvider` | Registers the query with the DXM provider |

`PublicIp.get()` returns `"unknown"` on `IOException` (no exception propagation).

## GraphQL API

| Operation | Name | Notes |
|-----------|------|-------|
| Query | `whatIsMyPublicServerIp` → String | Calls `checkip.amazonaws.com` on every invocation; no caching |

Requires `admin` permission.

```graphql
query {
  whatIsMyPublicServerIp
}
```

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

- Tests: `tests/cypress/e2e/01-whatIsMyPublicServerIp.cy.ts`
- Tests cover: GraphQL query returns a non-empty string, admin UI displays the IP
- `assets/provisioning.yml` installs `graphql-dxm-provider` + `serverSettings`

## Gotchas

- `checkip.amazonaws.com` is an external service — if the Jahia server has no internet egress (air-gapped / firewalled environments), the query returns `"unknown"`
- The IP is fetched on **every query** — there is no caching; repeated calls from the UI may trigger rate limits on `checkip.amazonaws.com`
- In Docker test environments, the returned IP is the host's NAT/bridge IP, not the public internet IP — tests should assert the value is non-empty rather than checking the exact IP
