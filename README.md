# Treblle - Runtime Intelligence Platform

[Website](https://treblle.com/) • [Documentation](https://docs.treblle.com/) • [Pricing](https://treblle.com/pricing)

Discover, Govern, and Secure APIs, Agents, and AI Across Any Cloud, Gateway or Technology.

## Treblle Spring Boot SDK

The Treblle Spring Boot SDK is a Spring Boot auto-configuration starter that captures every API
request and response on your application, masks sensitive data before anything leaves your server,
and ships the data to Treblle asynchronously. All expensive work happens after the response has been
flushed to the client, so the SDK adds no measurable latency and — by design — can never throw into
or break your API.

## Requirements

- Java 17 or newer (tested on JDK 17 and 21)
- Spring Boot 3.x (Spring MVC / Servlet stack)
- A Treblle account with an **SDK Token** and **API Key** from the [Treblle dashboard](https://app.treblle.com)

> Spring Boot 2.x is not supported: it predates the Jakarta (`jakarta.*`) servlet namespace this SDK
> is built on. WebFlux (reactive) applications are not supported in this release.

## Installation

### 1. Install the SDK

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.treblle</groupId>
    <artifactId>treblle-spring-boot-starter</artifactId>
    <version>3.0.0</version>
</dependency>
```

Or, for Gradle (`build.gradle`):

```groovy
implementation 'com.treblle:treblle-spring-boot-starter:3.0.0'
```

That is all the wiring you need — the starter auto-configures itself. There are no filters, beans,
or annotations to register manually.

### 2. Configure the SDK

Add your credentials to `application.properties`:

```properties
treblle.sdk-token=YOUR_SDK_TOKEN
treblle.api-key=YOUR_API_KEY
```

Or `application.yml`:

```yaml
treblle:
  sdk-token: YOUR_SDK_TOKEN
  api-key: YOUR_API_KEY
```

Every option can also be supplied through a `TREBLLE_*` environment variable (Spring Boot relaxed
binding). Explicitly declared properties take precedence over environment variables:

```bash
export TREBLLE_SDK_TOKEN=YOUR_SDK_TOKEN
export TREBLLE_API_KEY=YOUR_API_KEY
```

#### All configuration options

| Property | Env var | Type | Required | Default | Description |
|---|---|---|---|---|---|
| `treblle.sdk-token` | `TREBLLE_SDK_TOKEN` | String | **Yes** | — | SDK Token from the Treblle dashboard. Sent as `sdk_token` and as the `x-api-key` header. |
| `treblle.api-key` | `TREBLLE_API_KEY` | String | **Yes** | — | API Key from the Treblle dashboard. Sent as `api_key`. |
| `treblle.debug` | `TREBLLE_DEBUG` | boolean | No | `false` | Enables verbose local logging of all SDK activity. |
| `treblle.masked-keywords` | `TREBLLE_MASKED_KEYWORDS` | List&lt;String&gt; | No | `password, pwd, secret, password_confirmation, cc, card_number, ccv, ssn, authorization, api_key` | Keys whose values are masked before sending. **Masked by default** — override to customize, or set to empty to disable masking entirely. |
| `treblle.excluded-paths` | `TREBLLE_EXCLUDED_PATHS` | List&lt;String&gt; | No | `[]` | Route paths the SDK must not track. Exact paths and `/prefix/*` wildcards. Case-sensitive. |
| `treblle.ingress-endpoint` | `TREBLLE_INGRESS_ENDPOINT` | String | No | `https://ingress.treblle.com` | Treblle Ingress URL. Point at a regional or self-hosted endpoint. |
| `treblle.enabled` | `TREBLLE_ENABLED` | boolean | No | `true` | Master switch. When `false`, the SDK sends nothing. |

A complete example:

```properties
treblle.sdk-token=YOUR_SDK_TOKEN
treblle.api-key=YOUR_API_KEY
treblle.debug=false
treblle.enabled=true
treblle.ingress-endpoint=https://ingress.treblle.com
treblle.masked-keywords=password,pwd,secret,password_confirmation,cc,card_number,ccv,ssn,authorization,api_key
treblle.excluded-paths=/health,/actuator/*,/admin/*
```

> If `treblle.sdk-token` or `treblle.api-key` is missing, the SDK silently disables itself and never
> throws. Enable `treblle.debug=true` to see a warning explaining why nothing is being sent.

### 3. Verify it works

1. Start your application and make a request to any of your API endpoints.
2. Open your API in the [Treblle dashboard](https://app.treblle.com) — the request should appear
   within a few seconds.
3. If nothing shows up, set `treblle.debug=true` and restart. The SDK will log its resolved
   configuration, initialization status, each captured request, every outgoing send, and Treblle's
   responses under the `com.treblle` logger — enough to pinpoint any configuration problem.

## Data masking

Masking happens entirely on your server, before any data is sent to Treblle. The
`treblle.masked-keywords` list drives it:

- **Secure by default.** A standard set of sensitive keys (`password`, `pwd`, `secret`,
  `password_confirmation`, `cc`, `card_number`, `ccv`, `ssn`, `authorization`, `api_key`) is masked
  out of the box. Override `treblle.masked-keywords` to customize it, or set it to empty to disable
  masking entirely (a warning is logged in debug mode when masking is off).
- Matching is **case-insensitive** and applies to request bodies, response bodies, request headers,
  and response headers. Query-string values are masked in the captured `query` map, and the raw URL
  is recorded without its query string so no unmasked query value can leak through it.
- Each character of a matched value is replaced with `*`, preserving the original length
  (`"secret123"` → `"*********"`).
- Nested objects and arrays are handled recursively; array items are masked individually while the
  array structure is preserved.
- For `Authorization`-style headers, the auth scheme is preserved and only the credentials are
  masked (`"Bearer secret123"` → `"Bearer *********"`).
- `null` and empty values are left untouched.

```properties
treblle.masked-keywords=password,authorization,credit_card,ssn,cvv
```

Given a request body of:

```json
{ "email": "alice@example.com", "password": "s3cr3t", "card": { "cvv": "123" } }
```

Treblle receives:

```json
{ "email": "alice@example.com", "password": "******", "card": { "cvv": "***" } }
```

## Excluding paths

Use `treblle.excluded-paths` to stop tracking specific routes. It accepts exact paths and wildcard
prefixes, and matching is case-sensitive:

```properties
# Exact path
treblle.excluded-paths=/health
# Wildcard prefix: excludes everything under /admin/
treblle.excluded-paths=/health,/admin/*,/actuator/*
```

In addition to your configured exclusions, the SDK automatically skips static and infrastructure
resources so they never reach Treblle: images, CSS, JavaScript, fonts, media, `favicon.ico`,
`.well-known/` paths, `.env` files, and similar. Non-JSON request/response bodies are still captured
but replaced with a short "not valid JSON" message rather than the raw content.

## Per-request metadata

Attach custom key/value metadata to the current request from anywhere on the request thread
(controller, service, interceptor) using the static `TreblleMetadata` helper:

```java
import com.treblle.springboot.metadata.TreblleMetadata;
import org.springframework.web.bind.annotation.*;

@RestController
public class OrderController {

    @PostMapping("/orders")
    public Order create(@RequestBody OrderRequest request) {
        TreblleMetadata.add("tenant", request.getTenantId());
        TreblleMetadata.add("plan", "enterprise");
        TreblleMetadata.add("is_trial", false);
        return orderService.create(request);
    }
}
```

Metadata accepts `String`, `Number`, and `Boolean` values, and is sent in the payload's `metadata`
object. It is cleared automatically once the request completes.

> **Metadata is NOT masked.** Never put secrets, credentials, tokens, or personal data in metadata.

## Debug mode

Enable debug mode to diagnose SDK issues:

```properties
treblle.debug=true
```

When enabled, the SDK logs (under the `com.treblle` logger):

- Resolved configuration and initialization status, so you can confirm the SDK is enabled.
- Warnings when required configuration is missing, or when masking is disabled because
  `masked-keywords` is empty.
- Data-collection events and skipped/excluded paths.
- Every outgoing send to Treblle, Treblle's responses, circuit-breaker state transitions, dropped
  payloads, and any exceptions or HTTP errors that occur while sending.

If data isn't appearing in your dashboard, turn on debug mode first — the logs will tell you whether
the SDK is disabled, whether your credentials are set, and whether Ingress is responding.

## Running the tests

```bash
mvn verify
```

The suite covers configuration handling, masking, payload building and schema conformance, path
exclusion, transport safety, and the circuit breaker. All tests run against a mocked transport and
never call the real Treblle Ingress.

## Support

- Documentation: [https://docs.treblle.com/](https://docs.treblle.com/)
- Report issues: [GitHub Issues](https://github.com/Treblle/treblle-spring/issues)

## License

The MIT License (MIT). See [LICENSE](LICENSE) for details.
