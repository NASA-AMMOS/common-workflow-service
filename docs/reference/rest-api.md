# REST API

CWS exposes a REST API for launching and managing processes, querying history,
managing workers, and more. The running CWS console serves **interactive
Swagger UI** documentation for these endpoints, generated directly from the
live server so it always matches the deployed version.

## Accessing the API documentation

On a running CWS instance, open the Swagger UI at:

```
https://<your-cws-console-host>:<ssl-port>/cws-ui/api-docs
```

You can also reach it from the console's **Documentation** page. The raw
OpenAPI specification is available at:

```
https://<your-cws-console-host>:<ssl-port>/cws-ui/v3/api-docs
```

!!! info "Why isn't the API rendered here?"
    The OpenAPI specification is built dynamically by the running server from
    the live controller definitions, so it reflects exactly the endpoints and
    version of your deployment. Rather than ship a snapshot that could drift
    out of date, this site points you at the interactive docs served by your
    own instance.

## Authenticating requests

The API is protected by a `cwsToken` API key sent as a header. To obtain your
token from a browser session that is already logged in to CWS:

1. Open the CWS console and log in.
2. Open your browser's developer tools → **Application** tab.
3. Under **Cookies**, select the CWS console URL.
4. Copy the value of the **`cwsToken`** cookie.

In Swagger UI, use the authorization control to supply this value as the
`cwsToken` header. For scripted requests, send it as a header:

```bash
curl -H "cwsToken: <your-token>" \
  "https://<your-cws-console-host>:<ssl-port>/cws-ui/rest/..."
```

Tokens expire after a configurable period (see
[`cws_token_expiration_hours`](configuration.md#security-authentication)),
after which you must re-authenticate to obtain a new one.

## Enabling remote (CORS) access

By default the API is intended to be called from the CWS console origin. To
call it from other hosts, CORS must be enabled for the console. See
[Web Integration & CORS](../developer/web-integration.md).
