# Configuration Guide

Protify AI supports configuration at four levels: base (file/environment), pipeline, client, and request. Settings at lower levels override higher ones, giving you fine-grained control.

---

## Base Configuration

Base configuration is loaded once at startup when you call `ProtifyAI.initialize()`. It provides defaults for all clients and requests.

### Properties File

Create a `protifyai.properties` file on your classpath (e.g., `src/main/resources/protifyai.properties`):

```properties
# Client defaults
clientDefaults.maxOutputTokens=1000
clientDefaults.temperature=0.75
clientDefaults.instructions=You are a helpful assistant.

# Logging
logging.json.prettyPrint=true
logging.logRequests=true
logging.logResponses=false
logging.truncateLargeInput=false

# Request timeout
request.timeoutMillis=60000

# Retry policy
request.retryPolicy.maxRetries=3
request.retryPolicy.backoffStrategy=EXPONENTIAL
request.retryPolicy.delayMillis=500
request.retryPolicy.jitterMillis=200
request.retryPolicy.maxDelayMillis=10000
request.retryPolicy.maxElapsedTimeMillis=30000
request.retryPolicy.retryOnHttpStatusCodes=429,500,502,503,504
request.retryPolicy.respectRetryAfter=true

# Response caching
response.cache.maxEntries=1000
response.cache.ttlSecs=3600

# API key webhook (optional)
providers.apiKeyUrl=https://my-vault.example.com/keys
providers.apiKeyUrlTimeoutMs=5000
providers.apiKeyCacheTtlSecs=300
```

### Profiles

Use profiles to maintain separate configurations per environment. Set the `PROTIFY_CFG_PROFILE` environment variable:

```bash
export PROTIFY_CFG_PROFILE=staging
```

The SDK will look for files in this order:
1. `protifyai-staging.properties` (profile-specific)
2. `protifyai.properties` (base)

Profile-specific properties override base properties.

### Custom File Path

Point to a config file outside the classpath:

```bash
export PROTIFY_CFG_FILE_PATH=/etc/myapp/config
```

The SDK will look for `{path}/protifyai-{profile}.properties` and `{path}/protifyai.properties` on the filesystem before falling back to the classpath.

---

## Client-Level Configuration

Set defaults for all requests made through a specific client:

```java
AIClient client = AIClient.builder()
        .model(AIModel.GPT_5_1)
        .instructions("You are a helpful assistant.")
        .temperature(0.7)
        .maxOutputTokens(500)
        .topP(0.9)
        .topK(40)
        .retryPolicy(RetryPolicy.builder().maxRetries(3).build())
        .apiKey("sk-...")
        .logRequests(true)
        .logResponses(true)
        .prettyPrint(true)
        .truncateLongRequestInputs(true)
        .overridePipelineConfig(false)
        .build();
```

Client settings override base configuration.

---

## Request-Level Configuration

Override client settings for a single request:

```java
AIResponse response = client.newRequest()
        .addInput("Write a creative story.")
        .instructions("You are a novelist.")
        .temperature(0.9)
        .maxOutputTokens(2000)
        .topP(0.95)
        .topK(50)
        .retryPolicy(RetryPolicy.builder().maxRetries(5).build())
        .build()
        .execute();
```

Request settings override client settings.

---

## Pipeline-Level Configuration

Set defaults for all steps within a pipeline:

```java
AIPipeline pipeline = AIPipeline.builder()
        .instructions("Always respond in formal English.")
        .temperature(0.3)
        .maxOutputTokens(1000)
        .retryPolicy(RetryPolicy.builder().maxRetries(2).build())
        .logRequests(true)
        .logResponses(true)
        .prettyPrint(true)
        .truncateLongRequestInputs(true)
        .withInitialStep(() -> client.newRequest()
                .addInput("Begin the analysis.")
                .build())
        .build();
```

### Sub-Pipeline Inheritance

Use `inheritParentConfig()` to carry parent pipeline settings into a sub-pipeline:

```java
AIPipeline child = AIPipeline.builder()
        .inheritParentConfig()      // inherits parent's temperature, retryPolicy, etc.
        .temperature(0.9)           // overrides just this one setting
        .withInitialStep(...)
        .build();
```

---

## Configuration Hierarchy

Settings are resolved in this order (highest priority first):

1. **Request-level** -- set on `AIRequestBuilder`
2. **Client-level** -- set on `AIClientBuilder`
3. **Pipeline-level** -- set on `AIPipelineBuilder` (only within a pipeline)
4. **Base configuration** -- loaded from `protifyai.properties`

### Overriding Pipeline Config

By default, pipeline settings sit between client and base in the hierarchy. A client or request can force its settings to always win over pipeline settings:

```java
// Client always overrides pipeline
AIClient client = AIClient.builder()
        .model(AIModel.GPT_5_1)
        .temperature(0.5)
        .overridePipelineConfig(true)
        .build();

// Or per-request
client.newRequest()
        .addInput("...")
        .overridePipelineConfig(true)
        .build();
```

---

## Retry Policies

Configure automatic retry behavior for transient failures:

```java
RetryPolicy policy = RetryPolicy.builder()
        .maxRetries(3)                              // 0-10, required
        .backoffStrategy(RetryBackoffStrategy.EXPONENTIAL) // FIXED or EXPONENTIAL
        .delayMillis(500L)                          // base delay between retries
        .maxDelayMillis(10000L)                     // cap for exponential backoff
        .jitterMillis(200L)                         // random jitter added to delay
        .maxElapsedTimeMillis(30000L)               // total time budget for all retries
        .retryOnHttpStatusCodes(Set.of(429, 500, 502, 503, 504))
        .retryOnExceptions(Set.of(RuntimeException.class))
        .respectRetryAfter(true)                    // honor Retry-After headers
        .build();
```

**Defaults** (when `maxRetries` is 0, no retries occur):
- `backoffStrategy`: EXPONENTIAL
- `delayMillis`: 500
- `maxDelayMillis`: 10,000
- `jitterMillis`: 200
- `maxElapsedTimeMillis`: 20,000
- `retryOnHttpStatusCodes`: 408, 429, 500, 502, 503, 504
- `retryOnExceptions`: RuntimeException
- `respectRetryAfter`: true

Retry policies can be set at the client, request, or pipeline level. They can also be configured in the base properties file.

---

## Cloud Provider Configuration

Cloud providers require additional settings beyond an API key.

### Azure OpenAI

```java
AIClient client = AIClient.builder()
        .provider(ProtifyAIProvider.AZURE_OPEN_AI)
        .explicitModelVersion("gpt-4o")
        .resourceName("my-azure-resource")
        .deploymentName("my-gpt4o-deployment")
        .apiKey("my-azure-api-key")  // or AZURE_OPENAI_API_KEY env var
        .build();
```

### Google Vertex AI

```java
AIClient client = AIClient.builder()
        .model(AIModel.GEMINI_2_5_PRO_VERTEX)
        .region("us-central1")
        .projectId("my-gcp-project")
        .apiKey("my-oauth-token")  // or VERTEX_AI_ACCESS_TOKEN env var
        .build();
```

### AWS Bedrock

```java
AIClient client = AIClient.builder()
        .model(AIModel.CLAUDE_SONNET_4_6_BEDROCK)
        .region("us-east-1")
        // Uses AWS_ACCESS_KEY_ID + AWS_SECRET_ACCESS_KEY env vars
        // Or set explicitly:
        // .awsAccessKeyId("AKIA...")
        // .awsSecretAccessKey("wJalr...")
        // .awsSessionToken("FwoG...")  // optional, for temporary credentials
        .build();
```

---

## API Key Resolution

API keys are resolved in this order:

1. **Environment variable** -- each provider has a standard env var
2. **Explicit configuration** -- set via `AIClientBuilder.apiKey("sk-...")`
3. **Webhook** -- configure `providers.apiKeyUrl` in base config for dynamic retrieval

If both an environment variable and explicit key are set, the environment variable takes precedence.

| Provider | Environment Variable |
|---|---|
| OpenAI | `OPENAI_API_KEY` |
| Anthropic | `ANTHROPIC_API_KEY` |
| Google Gemini | `GEMINI_API_KEY` |
| Mistral | `MISTRAL_API_KEY` |
| Groq | `GROQ_API_KEY` |
| DeepSeek | `DEEPSEEK_API_KEY` |
| Together | `TOGETHER_API_KEY` |
| Fireworks | `FIREWORKS_API_KEY` |
| xAI | `XAI_API_KEY` |
| Azure OpenAI | `AZURE_OPENAI_API_KEY` |
| Vertex AI | `VERTEX_AI_ACCESS_TOKEN` |
| AWS Bedrock | `AWS_ACCESS_KEY_ID` + `AWS_SECRET_ACCESS_KEY` |

---

## Logging

Enable request/response logging for debugging:

```java
AIClient client = AIClient.builder()
        .model(AIModel.GPT_5_1)
        .logRequests(true)                   // log outgoing request JSON
        .logResponses(true)                  // log incoming response JSON
        .prettyPrint(true)                   // pretty-print logged JSON
        .truncateLongRequestInputs(true)     // truncate large inputs in logs
        .build();
```

These can also be set in `protifyai.properties`:

```properties
logging.logRequests=true
logging.logResponses=true
logging.json.prettyPrint=true
logging.truncateLargeInput=true
```

---

## All Configuration Properties

| Property Key | Type | Default | Description |
|---|---|---|---|
| `clientDefaults.maxOutputTokens` | int | 4096 | Maximum output tokens |
| `clientDefaults.temperature` | double | -- | Sampling temperature |
| `clientDefaults.instructions` | String | -- | System prompt |
| `request.timeoutMillis` | int | 60000 | HTTP request timeout |
| `request.retryPolicy.maxRetries` | int | 0 | Max retry attempts (0-10) |
| `request.retryPolicy.backoffStrategy` | String | EXPONENTIAL | FIXED or EXPONENTIAL |
| `request.retryPolicy.delayMillis` | long | 500 | Base retry delay |
| `request.retryPolicy.jitterMillis` | long | 200 | Random jitter on delay |
| `request.retryPolicy.maxDelayMillis` | long | 10000 | Max delay cap |
| `request.retryPolicy.maxElapsedTimeMillis` | long | 20000 | Total retry time budget |
| `request.retryPolicy.retryOnHttpStatusCodes` | String | 408,429,500,502,503,504 | Comma-separated HTTP codes |
| `request.retryPolicy.respectRetryAfter` | boolean | true | Honor Retry-After headers |
| `response.cache.maxEntries` | int | 1000 | Response cache size |
| `response.cache.ttlSecs` | int | 3600 | Response cache TTL |
| `logging.json.prettyPrint` | boolean | false | Pretty-print JSON logs |
| `logging.logRequests` | boolean | false | Log outgoing requests |
| `logging.logResponses` | boolean | false | Log incoming responses |
| `logging.truncateLargeInput` | boolean | true | Truncate large inputs in logs |
| `providers.apiKeyUrl` | String | -- | Webhook URL for API key retrieval |
| `providers.apiKeyUrlTimeoutMs` | int | 5000 | Webhook timeout |
| `providers.apiKeyCacheTtlSecs` | int | 300 | Webhook response cache TTL |
