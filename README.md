# what-is-my-public-server-ip

The purpose of this module is to allow the retrieval of the public IP used by a Jahia server to access Internet.

# Installation

- In Jahia, go to "Administration --> Server settings --> System components --> Modules"
- Upload the JAR **what-is-my-public-server-ip-X.X.X.jar**
- Check that the module is started

# Use

- Go to "Administration -> Server settings -> Configuration -> What is my public server IP"

# Known Limitations

- **External service dependency**: The module resolves the public IP by calling `https://checkip.amazonaws.com`. In air-gapped environments or when egress is blocked by a firewall, the query returns `"unknown"`.
- **Rate limiting**: The external service may impose rate limits. Repeated calls within a short window (e.g. rapid UI refreshes) could be throttled. The module caches the result for 60 seconds to reduce the number of outbound calls.
