# Spring Boot Starter

The Protify AI Spring Boot Starter provides auto-configuration for the Protify AI Java SDK in Spring Boot applications. It automatically creates `AIClient` beans, registers `@AIService` proxies as injectable Spring beans, and supports multiple named clients for multi-provider setups.

**Supports Spring Boot 2.x, 3.x, and 4.x.**

---

## Table of Contents

- [Getting Started](#getting-started)
- [Configuration](#configuration)
  - [Minimal Configuration](#minimal-configuration)
  - [Full Default Client Configuration](#full-default-client-configuration)
- [Multiple Providers (Named Clients)](#multiple-providers-named-clients)
  - [Configuring Named Clients](#configuring-named-clients)
  - [Using Named Clients with @AIService](#using-named-clients-with-aiservice)
  - [Direct Client Injection with @Qualifier](#direct-client-injection-with-qualifier)
  - [Using the AIClientRegistry](#using-the-aiclientregistry)
- [Auto-configured @AIService Beans](#auto-configured-aiservice-beans)
  - [How It Works](#how-it-works)
  - [Package Scanning](#package-scanning)
  - [Custom Scan Package](#custom-scan-package)
- [Auto-configured Beans](#auto-configured-beans)
- [Disabling Auto-Configuration](#disabling-auto-configuration)
- [Non-Boot Spring Applications](#non-boot-spring-applications)
- [Configuration Properties Reference](#configuration-properties-reference)
  - [Top-Level Properties](#top-level-properties)
  - [Client Properties](#client-properties)
  - [Retry Properties](#retry-properties)
  - [Provider Names](#provider-names)

---

## Getting Started

Add the starter dependency to your project:

**Gradle:**
```groovy
implementation 'ai.protify:protifyai-spring-boot-starter:0.1.8'
```

**Maven:**
```xml
<dependency>
    <groupId>ai.protify</groupId>
    <artifactId>protifyai-spring-boot-starter</artifactId>
    <version>0.1.8</version>
</dependency>
```

The starter transitively includes the core `protifyai` library. You do not need to add both.

---

## Configuration

### Minimal Configuration

```yaml
protify:
  ai:
    defaults:
      provider: anthropic
      model: claude-sonnet-4-6
```

The API key is read from the provider's standard environment variable (e.g., `ANTHROPIC_API_KEY`). See [API Key Resolution](#provider-names) for all environment variable names.

### Full Default Client Configuration

```yaml
protify:
  ai:
    defaults:
      # Provider & Model (required)
      provider: anthropic
      model: claude-sonnet-4-6
      # model-version: my-custom-model   # alternative to model, for custom model names

      # Authentication
      api-key: ${ANTHROPIC_API_KEY}

      # Model parameters
      temperature: 0.7
      max-output-tokens: 4096
      top-p: 0.9
      top-k: 40
      reasoning-effort: HIGH              # LOW, MEDIUM, HIGH
      instructions: "You are a helpful assistant."

      # Logging
      log-requests: true
      log-responses: false
      pretty-print: true
      truncate-long-request-inputs: true

      # Request timeout
      request-timeout-ms: 60000

      # Retry policy
      retry:
        max-retries: 3
        backoff-strategy: EXPONENTIAL     # FIXED or EXPONENTIAL
        delay-millis: 500
        jitter-millis: 200
        max-delay-millis: 10000
        max-elapsed-time-millis: 30000
        respect-retry-after: true

      # Cloud provider settings (when applicable)
      region: us-east-1
      project-id: my-gcp-project
      resource-name: my-azure-resource
      deployment-name: my-azure-deployment
      api-version: "2024-12-01-preview"

      # AWS Bedrock credentials
      aws-access-key-id: ${AWS_ACCESS_KEY_ID}
      aws-secret-access-key: ${AWS_SECRET_ACCESS_KEY}
      aws-session-token: ${AWS_SESSION_TOKEN}
```

---

## Multiple Providers (Named Clients)

Configure a default client plus any number of additional named clients. Each named client is fully independent with its own provider, model, and settings.

### Configuring Named Clients

```yaml
protify:
  ai:
    defaults:
      provider: anthropic
      api-key: ${ANTHROPIC_API_KEY}
      model: claude-sonnet-4-6
      temperature: 0.7

    clients:
      gpt:
        provider: openai
        api-key: ${OPENAI_API_KEY}
        model: gpt-4.1
        max-output-tokens: 2000

      gemini:
        provider: google
        api-key: ${GEMINI_API_KEY}
        model: gemini-2.5-flash
        temperature: 0.3

      bedrock-claude:
        provider: aws_bedrock
        model: anthropic.claude-sonnet-4-6-v1
        region: us-east-1
```

This creates four `AIClient` beans:
- A **default** `AIClient` (Anthropic Claude) -- injected with `@Autowired`
- A **gpt** `AIClient` -- injected with `@Qualifier("gpt")`
- A **gemini** `AIClient` -- injected with `@Qualifier("gemini")`
- A **bedrock-claude** `AIClient` -- injected with `@Qualifier("bedrock-claude")`

### Using Named Clients with @AIService

Use the `client` attribute on `@AIService` to bind a service to a specific named client:

```java
@AIService  // uses the default client (Anthropic)
@Instructions("You are a summarization expert.")
public interface SummaryService {
    @UserMessage("Summarize the following text:\n\n{{text}}")
    String summarize(@V("text") String text);
}

@AIService(client = "gpt")  // uses the "gpt" named client (OpenAI)
@Instructions("You are a professional translator.")
public interface TranslationService {
    @UserMessage("Translate the following to {{language}}:\n\n{{text}}")
    String translate(@V("text") String text, @V("language") String language);
}

@AIService(client = "gemini")  // uses the "gemini" named client (Google)
public interface ClassificationService {
    @UserMessage("Classify the sentiment of: {{text}}")
    String classifySentiment(@V("text") String text);
}
```

All three interfaces are auto-registered as Spring beans and can be injected normally:

```java
@RestController
public class MyController {

    @Autowired
    private SummaryService summaryService;

    @Autowired
    private TranslationService translationService;

    @Autowired
    private ClassificationService classificationService;

    @GetMapping("/summarize")
    public String summarize(@RequestParam String text) {
        return summaryService.summarize(text);
    }
}
```

### Direct Client Injection with @Qualifier

Inject named `AIClient` beans directly for programmatic use:

```java
@Service
public class AIOrchestrator {

    @Autowired
    private AIClient defaultClient;  // the default (Anthropic) client

    @Autowired
    @Qualifier("gpt")
    private AIClient gptClient;

    @Autowired
    @Qualifier("gemini")
    private AIClient geminiClient;

    public String askClaude(String question) {
        return defaultClient.newRequest()
                .addInput(question)
                .build().execute().text();
    }

    public String askGpt(String question) {
        return gptClient.newRequest()
                .addInput(question)
                .build().execute().text();
    }
}
```

### Using the AIClientRegistry

The `AIClientRegistry` bean provides programmatic access to all configured clients:

```java
@Service
public class DynamicProviderService {

    @Autowired
    private AIClientRegistry registry;

    public String ask(String clientName, String question) {
        AIClient client = registry.getClient(clientName);  // "" or null returns default
        return client.newRequest()
                .addInput(question)
                .build().execute().text();
    }

    public Set<String> availableClients() {
        return registry.getClientNames();  // e.g., ["gpt", "gemini", "bedrock-claude"]
    }
}
```

---

## Auto-configured @AIService Beans

### How It Works

The starter automatically scans for interfaces annotated with `@AIService` and registers them as Spring beans. Each bean is a JDK dynamic proxy (created by the core SDK's `ProtifyAI.create()`) that translates method calls into AI provider requests.

No manual `ProtifyAI.create()` calls are needed -- just define the interface, annotate it, and inject it.

### Package Scanning

By default, the starter scans the package of your `@SpringBootApplication` class and all sub-packages. For example, if your main class is in `com.example.myapp`, any `@AIService` interface in `com.example.myapp` or `com.example.myapp.ai` will be found.

### Custom Scan Package

Override the scan package if your `@AIService` interfaces are in a different package tree:

```yaml
protify:
  ai:
    service-scan-package: com.example.services
```

---

## Auto-configured Beans

The starter auto-configures the following beans:

| Bean | Type | Description |
|---|---|---|
| `protifyAIClient` | `AIClient` | Default client from `protify.ai.defaults.*` properties |
| `<name>AIClient` | `AIClient` | Named client for each entry in `protify.ai.clients.<name>`. Qualified with `@Qualifier("<name>")` |
| `protifyAIClientRegistry` | `AIClientRegistry` | Registry holding the default + all named clients |
| `protifyAIInitializer` | (internal) | Calls `ProtifyAI.initialize()` at startup |
| `<serviceName>` | `@AIService` proxy | One bean per scanned `@AIService` interface |

All beans are created with `@ConditionalOnMissingBean`, so you can override any of them by defining your own.

---

## Disabling Auto-Configuration

Disable the starter entirely:

```yaml
protify:
  ai:
    enabled: false
```

Or exclude it from auto-configuration:

```java
@SpringBootApplication(exclude = ProtifyAIAutoConfiguration.class)
public class MyApp { }
```

---

## Non-Boot Spring Applications

For Spring applications that don't use Spring Boot auto-configuration, use the `@EnableProtifyAI` annotation:

```java
@Configuration
@EnableProtifyAI
public class AppConfig {

    @Bean
    public AIClient defaultClient() {
        return AIClient.builder()
                .model(AIModel.CLAUDE_SONNET_4_6)
                .build();
    }

    @Bean
    public AIClientRegistry clientRegistry(AIClient defaultClient) {
        return new AIClientRegistry(defaultClient);
    }
}
```

---

## Configuration Properties Reference

### Top-Level Properties

| Property | Type | Default | Description |
|---|---|---|---|
| `protify.ai.enabled` | boolean | `true` | Enable/disable auto-configuration |
| `protify.ai.service-scan-package` | String | (auto-detected) | Base package to scan for `@AIService` interfaces |

### Client Properties

These properties are available under both `protify.ai.defaults.*` and `protify.ai.clients.<name>.*`:

| Property | Type | Default | Description |
|---|---|---|---|
| `provider` | String | -- | **Required.** Provider name (see [Provider Names](#provider-names)) |
| `model` | String | -- | **Required.** Model name/identifier |
| `model-version` | String | -- | Alternative to `model` for custom model version strings |
| `api-key` | String | (env var) | API key. Falls back to the provider's environment variable |
| `temperature` | Double | -- | Sampling temperature (0.0-2.0) |
| `max-output-tokens` | Integer | 4096 | Maximum output tokens |
| `top-p` | Double | -- | Top-P (nucleus) sampling |
| `top-k` | Integer | -- | Top-K sampling |
| `reasoning-effort` | String | -- | `LOW`, `MEDIUM`, or `HIGH` |
| `instructions` | String | -- | System prompt / instructions |
| `log-requests` | Boolean | `false` | Log outgoing request JSON |
| `log-responses` | Boolean | `false` | Log incoming response JSON |
| `pretty-print` | Boolean | `false` | Pretty-print logged JSON |
| `truncate-long-request-inputs` | Boolean | `true` | Truncate large inputs in logs |
| `request-timeout-ms` | Integer | 60000 | HTTP request timeout in milliseconds |
| `region` | String | -- | Cloud region (AWS, Azure, GCP) |
| `project-id` | String | -- | GCP project ID or Azure AI Foundry project |
| `resource-name` | String | -- | Azure resource name |
| `deployment-name` | String | -- | Azure OpenAI deployment name |
| `api-version` | String | -- | API version (Azure OpenAI) |
| `aws-access-key-id` | String | -- | AWS access key ID |
| `aws-secret-access-key` | String | -- | AWS secret access key |
| `aws-session-token` | String | -- | AWS session token (temporary credentials) |

### Retry Properties

Available under `protify.ai.defaults.retry.*` and `protify.ai.clients.<name>.retry.*`:

| Property | Type | Default | Description |
|---|---|---|---|
| `max-retries` | Integer | 0 | Max retry attempts (0-10) |
| `backoff-strategy` | String | `EXPONENTIAL` | `FIXED` or `EXPONENTIAL` |
| `delay-millis` | Long | 500 | Base delay between retries |
| `jitter-millis` | Long | 200 | Random jitter added to delay |
| `max-delay-millis` | Long | 10000 | Maximum delay cap |
| `max-elapsed-time-millis` | Long | 20000 | Total time budget for all retries |
| `respect-retry-after` | Boolean | `true` | Honor Retry-After HTTP headers |

### Provider Names

Use these values for the `provider` property. Both the enum name and display name are accepted (case-insensitive):

| Provider | `provider` value | API Key Environment Variable |
|---|---|---|
| OpenAI | `openai` or `open_ai` | `OPENAI_API_KEY` |
| Anthropic | `anthropic` | `ANTHROPIC_API_KEY` |
| Google Gemini | `google` or `gemini` | `GEMINI_API_KEY` |
| Mistral | `mistral` | `MISTRAL_API_KEY` |
| Groq | `groq` | `GROQ_API_KEY` |
| DeepSeek | `deepseek` or `deep_seek` | `DEEPSEEK_API_KEY` |
| Together | `together` | `TOGETHER_API_KEY` |
| Fireworks | `fireworks` | `FIREWORKS_API_KEY` |
| xAI | `xai` or `x_ai` | `XAI_API_KEY` |
| Azure OpenAI | `azure_open_ai` or `Azure OpenAI` | `AZURE_OPENAI_API_KEY` |
| Azure AI Foundry | `azure_ai_foundry` or `Azure AI Foundry` | `AZURE_AI_FOUNDRY_API_KEY` |
| Google Vertex AI | `vertex_ai` or `Vertex AI` | `VERTEX_AI_ACCESS_TOKEN` |
| AWS Bedrock | `aws_bedrock` or `AWS Bedrock` | `AWS_ACCESS_KEY_ID` + `AWS_SECRET_ACCESS_KEY` |
