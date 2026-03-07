# Protify AI - Java SDK

A lightweight, provider-agnostic Java SDK for building AI-powered applications. Supports 13 providers — OpenAI, Anthropic, Google Gemini, Mistral, Groq, DeepSeek, Together, Fireworks, xAI, Azure OpenAI, Azure AI Foundry, Google Vertex AI, and AWS Bedrock — with a unified API for single requests, multi-step pipelines, streaming, parallel execution, and conditional branching.

---

## Using with AI Coding Agents

The [`docs/AI-AGENT-GUIDE.md`](docs/AI-AGENT-GUIDE.md) file is a compact reference designed for AI coding agents like Claude Code, Codex, Cursor, and Windsurf. It contains the full API surface with correct package names, copy-paste patterns, and common mistakes — everything an agent needs to write correct Protify AI code on the first attempt without exploring the source tree.

### Claude Code

Add a reference to your project's `CLAUDE.md` file:

```markdown
When writing code that uses the Protify AI SDK, refer to docs/AI-AGENT-GUIDE.md for the API reference.
```

Claude Code reads `CLAUDE.md` automatically and will pull in the guide when relevant.

### OpenAI Codex

Add to your `AGENTS.md` or `codex-instructions.md` file:

```markdown
For the Protify AI Java SDK, see docs/AI-AGENT-GUIDE.md for API patterns, imports, and usage examples.
```

### Cursor / Windsurf

Add the file as a project-level doc or reference it in your rules file (`.cursorrules` or `.windsurfrules`):

```
@docs/AI-AGENT-GUIDE.md
```

### Why This Matters

Without the reference doc, an AI agent has to explore the codebase to figure out the API — reading source files, inferring usage from method signatures, guessing at package names. This is slow, burns context window, and often produces subtly wrong code (incorrect imports, wrong method order, using internal classes). The agent guide gives the agent the right answer immediately, so it writes correct code on the first try.

---

## Table of Contents

- [Quick Start](#quick-start)
- [Initialization](#initialization)
- [AI Clients](#ai-clients)
  - [Built-in Models](#built-in-models)
  - [Cloud Providers](#cloud-providers)
  - [Custom Models and Providers](#custom-models-and-providers)
  - [Client Configuration](#client-configuration)
- [Requests](#requests)
  - [Text Input](#text-input)
  - [File Input](#file-input)
  - [Request Configuration](#request-configuration)
- [Responses](#responses)
- [Structured Output](#structured-output)
- [Declarative AI Services](#declarative-ai-services)
  - [Basic Usage](#basic-usage)
  - [Annotations](#annotations)
  - [Return Types](#return-types)
  - [Streaming and Async](#streaming-and-async)
  - [Configuration Overrides](#configuration-overrides)
- [Streaming](#streaming)
- [Async Execution](#async-execution)
- [Pipelines](#pipelines)
  - [Basic Pipeline](#basic-pipeline)
  - [Request Steps](#request-steps)
  - [Transformation Steps](#transformation-steps)
  - [Parallel Steps](#parallel-steps)
  - [Conditional Steps](#conditional-steps)
  - [Safe Steps (Error Handling)](#safe-steps-error-handling)
  - [Loop Steps](#loop-steps)
  - [Composable Pipelines (Pipeline as a Step)](#composable-pipelines-pipeline-as-a-step)
  - [Pipeline Streaming](#pipeline-streaming)
  - [Pipeline Configuration](#pipeline-configuration)
- [Configuration Hierarchy](#configuration-hierarchy)
- [Retry Policies](#retry-policies)
- [Conversations](#conversations)
  - [In-Memory Conversation](#in-memory-conversation)
  - [Persistent Store](#persistent-store)
  - [Conversations with Tools](#conversations-with-tools)
  - [File Inputs in Conversations](#file-inputs-in-conversations)
  - [Conversation State](#conversation-state)
- [Reasoning Models](#reasoning-models)
  - [Reasoning Effort](#reasoning-effort)
  - [Reasoning Content in Responses](#reasoning-content-in-responses)
  - [Parameter Validation](#parameter-validation)
  - [Reasoning in Declarative AI Services](#reasoning-in-declarative-ai-services)
- [API Key Resolution](#api-key-resolution)
- [Documentation](#documentation)

---

## Quick Start

```java
import ai.protify.core.AIClient;
import ai.protify.core.AIModel;
import ai.protify.core.ProtifyAI;
import ai.protify.core.response.AIResponse;

public class QuickStart {
    public static void main(String[] args) {
        ProtifyAI.initialize();

        AIClient client = AIClient.builder()
                .model(AIModel.GPT_5_1)
                .build();

        AIResponse response = client.newRequest()
                .addInput("What is the capital of France?")
                .build()
                .execute();

        System.out.println(response.text());
    }
}
```

---

## Initialization

Call `ProtifyAI.initialize()` once at application startup before using any other API:

```java
ProtifyAI.initialize();
```

---

## AI Clients

An `AIClient` is a reusable, configured entry point for making AI requests. Each client is bound to a specific model and provider.

### Built-in Models

**Anthropic:**

| Constant | Model ID |
|---|---|
| `AIModel.CLAUDE_OPUS_4_6` | `claude-opus-4-6` |
| `AIModel.CLAUDE_SONNET_4_6` | `claude-sonnet-4-6` |
| `AIModel.CLAUDE_HAIKU_4_5` | `claude-haiku-4-5` |

**OpenAI:**

| Constant | Model ID |
|---|---|
| `AIModel.GPT_5_4` | `gpt-5.4` |
| `AIModel.GPT_5_4_PRO` | `gpt-5.4-pro` |
| `AIModel.GPT_5_2` | `gpt-5.2` |
| `AIModel.GPT_5_2_PRO` | `gpt-5.2-pro` |
| `AIModel.GPT_5_2_CODEX` | `gpt-5.2-codex` |
| `AIModel.GPT_5_1` | `gpt-5.1` |
| `AIModel.GPT_5_1_CODEX` | `gpt-5.1-codex` |
| `AIModel.GPT_5_1_CODEX_MAX` | `gpt-5.1-codex-max` |
| `AIModel.GPT_5_MINI` | `gpt-5-mini` |
| `AIModel.GPT_5_NANO` | `gpt-5-nano` |
| `AIModel.GPT_4_1` | `gpt-4.1` |

**Google Gemini:**

| Constant | Model ID |
|---|---|
| `AIModel.GEMINI_3_1_PRO_PREVIEW` | `gemini-3.1-pro-preview` |
| `AIModel.GEMINI_3_FLASH_PREVIEW` | `gemini-3-flash-preview` |
| `AIModel.GEMINI_2_5_PRO` | `gemini-2.5-pro` |
| `AIModel.GEMINI_2_5_FLASH` | `gemini-2.5-flash` |
| `AIModel.GEMINI_2_5_FLASH_LITE` | `gemini-2.5-flash-lite` |

**Mistral:**

| Constant | Model ID |
|---|---|
| `AIModel.MISTRAL_LARGE` | `mistral-large-latest` |
| `AIModel.MISTRAL_MEDIUM` | `mistral-medium-latest` |
| `AIModel.MISTRAL_SMALL` | `mistral-small-latest` |
| `AIModel.CODESTRAL` | `codestral-latest` |
| `AIModel.DEVSTRAL` | `devstral-latest` |
| `AIModel.MAGISTRAL_MEDIUM` | `magistral-medium-latest` |
| `AIModel.MAGISTRAL_SMALL` | `magistral-small-latest` |

**Groq:**

| Constant | Model ID |
|---|---|
| `AIModel.LLAMA_4_SCOUT` | `meta-llama/llama-4-scout-17b-16e-instruct` |
| `AIModel.LLAMA_3_3_70B` | `llama-3.3-70b-versatile` |
| `AIModel.LLAMA_3_1_8B` | `llama-3.1-8b-instant` |
| `AIModel.GPT_OSS_120B` | `openai/gpt-oss-120b` |
| `AIModel.QWEN_3_32B_GROQ` | `qwen/qwen3-32b` |

**DeepSeek:**

| Constant | Model ID |
|---|---|
| `AIModel.DEEPSEEK_CHAT` | `deepseek-chat` |
| `AIModel.DEEPSEEK_REASONER` | `deepseek-reasoner` |

**Together:**

| Constant | Model ID |
|---|---|
| `AIModel.LLAMA_4_MAVERICK_TOGETHER` | `meta-llama/Llama-4-Maverick-17B-128E-Instruct-FP8` |
| `AIModel.LLAMA_3_3_70B_TOGETHER` | `meta-llama/Llama-3.3-70B-Instruct-Turbo` |
| `AIModel.DEEPSEEK_V3_1_TOGETHER` | `deepseek-ai/DeepSeek-V3.1` |

**Fireworks:**

| Constant | Model ID |
|---|---|
| `AIModel.LLAMA_3_3_70B_FIREWORKS` | `accounts/fireworks/models/llama-v3p3-70b-instruct` |
| `AIModel.DEEPSEEK_V3_FIREWORKS` | `accounts/fireworks/models/deepseek-v3p1` |
| `AIModel.QWEN_3_8B_FIREWORKS` | `accounts/fireworks/models/qwen3-8b` |

**xAI:**

| Constant | Model ID |
|---|---|
| `AIModel.GROK_4_1_FAST` | `grok-4-1-fast-reasoning` |
| `AIModel.GROK_4_1_FAST_NON_REASONING` | `grok-4-1-fast-non-reasoning` |
| `AIModel.GROK_4` | `grok-4` |
| `AIModel.GROK_3_MINI` | `grok-3-mini` |
| `AIModel.GROK_CODE_FAST` | `grok-code-fast` |

**Azure OpenAI:**

| Constant | Model ID |
|---|---|
| `AIModel.GPT_5_2_AZURE` | `gpt-5.2` |
| `AIModel.GPT_5_1_AZURE` | `gpt-5.1` |
| `AIModel.GPT_5_MINI_AZURE` | `gpt-5-mini` |
| `AIModel.GPT_5_NANO_AZURE` | `gpt-5-nano` |
| `AIModel.GPT_4_1_AZURE` | `gpt-4.1` |
| `AIModel.GPT_4O_AZURE` | `gpt-4o` |
| `AIModel.GPT_4O_MINI_AZURE` | `gpt-4o-mini` |
| `AIModel.O3_AZURE` | `o3` |
| `AIModel.O3_MINI_AZURE` | `o3-mini` |
| `AIModel.O4_MINI_AZURE` | `o4-mini` |

**Azure AI Foundry:**

Azure AI Foundry provides a unified API across multiple model vendors. A single `resourceName` and API key gives you access to models from Anthropic, OpenAI, Mistral, Meta, and more — all through the same chat completions interface.

| Constant | Model ID | Vendor |
|---|---|---|
| `AIModel.CLAUDE_SONNET_4_6_FOUNDRY` | `claude-sonnet-4-6` | Anthropic |
| `AIModel.CLAUDE_HAIKU_4_5_FOUNDRY` | `claude-haiku-4-5` | Anthropic |
| `AIModel.GPT_5_2_FOUNDRY` | `gpt-5.2` | OpenAI |
| `AIModel.GPT_5_1_FOUNDRY` | `gpt-5.1` | OpenAI |
| `AIModel.GPT_5_MINI_FOUNDRY` | `gpt-5-mini` | OpenAI |
| `AIModel.GPT_4O_FOUNDRY` | `gpt-4o` | OpenAI |
| `AIModel.GPT_4O_MINI_FOUNDRY` | `gpt-4o-mini` | OpenAI |
| `AIModel.MISTRAL_LARGE_FOUNDRY` | `mistral-large-latest` | Mistral |
| `AIModel.MISTRAL_SMALL_FOUNDRY` | `mistral-small-latest` | Mistral |
| `AIModel.LLAMA_3_3_70B_FOUNDRY` | `Meta-Llama-3.3-70B-Instruct` | Meta |
| `AIModel.LLAMA_4_SCOUT_FOUNDRY` | `Meta-Llama-4-Scout-17B-16E-Instruct` | Meta |
| `AIModel.LLAMA_4_MAVERICK_FOUNDRY` | `Meta-Llama-4-Maverick-17B-128E-Instruct-FP8` | Meta |

**Google Vertex AI:**

| Constant | Model ID |
|---|---|
| `AIModel.GEMINI_2_5_PRO_VERTEX` | `gemini-2.5-pro` |
| `AIModel.GEMINI_2_5_FLASH_VERTEX` | `gemini-2.5-flash` |

**AWS Bedrock:**

The Bedrock Converse API provides a unified interface across all Bedrock-supported model providers.

| Constant | Model ID | Vendor |
|---|---|---|
| `AIModel.CLAUDE_OPUS_4_6_BEDROCK` | `anthropic.claude-opus-4-6-v1` | Anthropic |
| `AIModel.CLAUDE_SONNET_4_6_BEDROCK` | `anthropic.claude-sonnet-4-6` | Anthropic |
| `AIModel.CLAUDE_HAIKU_4_5_BEDROCK` | `anthropic.claude-haiku-4-5-20251001-v1:0` | Anthropic |
| `AIModel.LLAMA_4_MAVERICK_BEDROCK` | `meta.llama4-maverick-17b-instruct-v1:0` | Meta |
| `AIModel.LLAMA_4_SCOUT_BEDROCK` | `meta.llama4-scout-17b-instruct-v1:0` | Meta |
| `AIModel.LLAMA_3_3_70B_BEDROCK` | `meta.llama3-3-70b-instruct-v1:0` | Meta |
| `AIModel.MISTRAL_LARGE_BEDROCK` | `mistral.mistral-large-2411-v1:0` | Mistral |
| `AIModel.MISTRAL_SMALL_BEDROCK` | `mistral.mistral-small-2503-v1:0` | Mistral |
| `AIModel.AMAZON_NOVA_PRO_BEDROCK` | `amazon.nova-pro-v1:0` | Amazon |
| `AIModel.AMAZON_NOVA_LITE_BEDROCK` | `amazon.nova-lite-v1:0` | Amazon |
| `AIModel.AMAZON_NOVA_MICRO_BEDROCK` | `amazon.nova-micro-v1:0` | Amazon |
| `AIModel.COHERE_COMMAND_R_PLUS_BEDROCK` | `cohere.command-r-plus-v1:0` | Cohere |
| `AIModel.COHERE_COMMAND_R_BEDROCK` | `cohere.command-r-v1:0` | Cohere |

All built-in models use auto-updating aliases where available. For pinned versions, use `AIModel.custom()`.

```java
AIClient client = AIClient.builder()
        .model(AIModel.CLAUDE_SONNET_4_6)
        .build();
```

### Cloud Providers

Cloud providers require additional configuration beyond an API key.

**Azure OpenAI:**

Azure OpenAI Service requires a resource name and deployment name. Each deployment maps to a specific model in your Azure portal.

```java
// Using a built-in Azure OpenAI model
AIClient client = AIClient.builder()
        .model(AIModel.GPT_4O_AZURE)
        .resourceName("my-azure-resource")
        .deploymentName("my-gpt4o-deployment")
        .apiKey("my-azure-api-key")  // or AZURE_OPENAI_API_KEY env var
        .build();

// Or using explicitModelVersion for custom deployments
AIClient client = AIClient.builder()
        .provider(ProtifyAIProvider.AZURE_OPEN_AI)
        .explicitModelVersion("gpt-4o")
        .resourceName("my-azure-resource")
        .deploymentName("my-gpt4o-deployment")
        .build();
```

**Azure AI Foundry:**

Azure AI Foundry provides a unified chat completions API across multiple model vendors (Anthropic, OpenAI, Mistral, Meta, and more). Unlike Azure OpenAI, it does not require a deployment name — models are accessed by name through a single endpoint.

```java
// Claude on Azure AI Foundry
AIClient client = AIClient.builder()
        .model(AIModel.CLAUDE_SONNET_4_6_FOUNDRY)
        .resourceName("my-foundry-resource")
        .apiKey("my-foundry-api-key")  // or AZURE_AI_FOUNDRY_API_KEY env var
        .build();

// GPT on Azure AI Foundry
AIClient client = AIClient.builder()
        .model(AIModel.GPT_4O_FOUNDRY)
        .resourceName("my-foundry-resource")
        .build();

// Llama on Azure AI Foundry
AIClient client = AIClient.builder()
        .model(AIModel.LLAMA_4_SCOUT_FOUNDRY)
        .resourceName("my-foundry-resource")
        .build();
```

The API version defaults to `2024-12-01-preview`. Override it with `.apiVersion()` if needed.

**Google Vertex AI:**

```java
AIClient client = AIClient.builder()
        .model(AIModel.GEMINI_2_5_PRO_VERTEX)
        .region("us-central1")
        .projectId("my-gcp-project")
        .apiKey("my-oauth-token")  // or VERTEX_AI_ACCESS_TOKEN env var
        .build();
```

**AWS Bedrock:**

```java
AIClient client = AIClient.builder()
        .model(AIModel.CLAUDE_SONNET_4_6_BEDROCK)
        .region("us-east-1")  // or set AWS_REGION env var
        // Uses AWS_ACCESS_KEY_ID + AWS_SECRET_ACCESS_KEY env vars
        // Or set explicitly:
        // .awsAccessKeyId("AKIA...")
        // .awsSecretAccessKey("wJalr...")
        // .awsSessionToken("FwoG...")  // optional, for temporary credentials
        .build();
```

### Custom Models and Providers

Use `AIModel.custom()` with a built-in provider, or create a fully custom provider with `AIProvider.custom()`:

```java
// Custom model name with a built-in provider
AIClient client = AIClient.builder()
        .model(AIModel.custom("gpt-5.1-turbo-preview", AIModel.GPT_5_1.getProvider()))
        .build();

// Fully custom provider
AIProvider myProvider = AIProvider.custom("my-provider")
        .apiKeyVarName("MY_PROVIDER_API_KEY")
        .clientType(MyProviderClient.class)
        .headers(credential -> Map.of(
                "Authorization", "Bearer " + credential,
                "Content-Type", "application/json"
        ))
        .allMimeTypesSupported()
        .build();

AIClient client = AIClient.builder()
        .provider(myProvider)
        .explicitModelVersion("my-model-v2")
        .build();
```

### Client Configuration

```java
AIClient client = AIClient.builder()
        .model(AIModel.GPT_5_1)
        .instructions("You are a helpful assistant. Respond concisely.")
        .temperature(0.7)
        .maxOutputTokens(500)
        .reasoningEffort(ReasoningEffort.MEDIUM)  // enable extended thinking
        .retryPolicy(RetryPolicy.builder().maxRetries(3).build())
        .apiKey("sk-...")                        // explicit API key (optional)
        .logRequests(true)                       // log outgoing request JSON
        .logResponses(true)                      // log incoming response JSON
        .prettyPrint(true)                       // pretty-print logged JSON
        .truncateLongRequestInputs(true)         // truncate large inputs in logs
        .overridePipelineConfig(false)           // see Configuration Hierarchy
        .build();
```

---

## Requests

### Text Input

```java
AIResponse response = client.newRequest()
        .addInput("Summarize the following text:")
        .addInput("The quick brown fox jumps over the lazy dog.")
        .build()
        .execute();

System.out.println(response.text());
```

Multiple `addInput()` calls append content blocks to the same user message.

### File Input

Images and PDFs can be attached from files, classpath resources, URLs, or Base64 data URLs:

```java
// From a local file
AIResponse response = client.newRequest()
        .addInput("Describe this image:")
        .addInput(AIFileInput.fromFile(new File("/path/to/photo.jpg")))
        .build()
        .execute();

// From classpath
client.newRequest()
        .addInput("Summarize this PDF:")
        .addInput(AIFileInput.fromClasspath("/documents/report.pdf"))
        .build()
        .execute();

// From a file path string
client.newRequest()
        .addInput(AIFileInput.fromFilePath("/path/to/diagram.png"))
        .build()
        .execute();

// From a URL
client.newRequest()
        .addInput(AIFileInput.fromUrl("https://example.com/image.png"))
        .build()
        .execute();

// From a Base64 data URL
client.newRequest()
        .addInput(AIFileInput.fromDataUrl("data:image/png;base64,iVBORw0KGgo..."))
        .build()
        .execute();
```

Supported file types: PNG, JPG/JPEG, GIF, WebP, HEIC, HEIF, PDF.

### Request Configuration

Per-request settings override client-level settings:

```java
AIResponse response = client.newRequest()
        .addInput("Write a creative story.")
        .instructions("You are a novelist.")     // overrides client instructions
        .temperature(0.9)                         // overrides client temperature
        .maxOutputTokens(2000)                    // overrides client maxOutputTokens
        .reasoningEffort(ReasoningEffort.HIGH)    // overrides client reasoning effort
        .retryPolicy(RetryPolicy.builder().maxRetries(5).build())
        .build()
        .execute();
```

---

## Responses

`AIResponse` provides the generated text and metadata:

```java
AIResponse response = client.newRequest()
        .addInput("Hello!")
        .build()
        .execute();

String text              = response.text();
String reasoning         = response.getReasoningContent();  // chain-of-thought (or null)
String modelName         = response.getModelName();
long inputTokens         = response.getInputTokens();
long outputTokens        = response.getOutputTokens();
long totalTokens         = response.getTotalTokens();
boolean cached           = response.isCachedResponse();
String rawProviderJson   = response.getProviderResponse();
String responseId        = response.getResponseId();
String correlationId     = response.getCorrelationId();
```

---

## Structured Output

Deserialize LLM JSON responses directly into Java objects using `as()` and `asList()`:

### Single Object

```java
public class MovieReview {
    private String title;
    private int rating;
    private String summary;

    // getters and setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
}

AIResponse response = client.newRequest()
        .addInput("Review the movie Inception. Respond in JSON with fields: title, rating (1-10), summary.")
        .build()
        .execute();

MovieReview review = response.as(MovieReview.class);
System.out.println(review.getTitle() + ": " + review.getRating() + "/10");
```

### List of Objects

```java
List<MovieReview> reviews = client.newRequest()
        .addInput("Review these 3 movies as a JSON array: Inception, Interstellar, Tenet. "
                + "Each with fields: title, rating (1-10), summary.")
        .build()
        .execute()
        .asList(MovieReview.class);

reviews.forEach(r -> System.out.println(r.getTitle() + ": " + r.getRating()));
```

### Nested Objects and Collections

Nested objects, lists with generics, and maps are handled recursively:

```java
public class AnalysisReport {
    private String topic;
    private List<Finding> findings;
    private Map<String, Integer> scores;

    // getters and setters...
}

public class Finding {
    private String description;
    private String severity;

    // getters and setters...
}

AnalysisReport report = response.as(AnalysisReport.class);
```

### Custom JSON Field Names

Use `@ProtifyJsonProperty` on fields when the JSON key doesn't match the Java field name:

```java
import ai.protify.core.internal.util.json.ProtifyJsonProperty;

public class GitHubRepo {
    @ProtifyJsonProperty("full_name")
    private String fullName;

    @ProtifyJsonProperty("stargazers_count")
    private int starCount;

    private String description;

    // getters and setters...
}
```

### Supported Types

| Java Type | JSON Type |
|---|---|
| `String` | string |
| `int` / `Integer` | number |
| `long` / `Long` | number |
| `double` / `Double` | number |
| `float` / `Float` | number |
| `boolean` / `Boolean` | boolean |
| `short` / `Short`, `byte` / `Byte` | number |
| Enums | string (matched by name) |
| `List<T>` | array |
| `Map<String, T>` | object |
| Nested POJOs | object |

---

## Declarative AI Services

Define AI-powered interfaces with annotations and let the SDK generate implementations via JDK dynamic proxies. This eliminates boilerplate for common AI call patterns.

**Before (imperative):**

```java
AIResponse response = client.newRequest()
        .instructions("You are a sentiment analyzer")
        .addInput("Analyze the sentiment of: I love this product!")
        .build().execute();
Sentiment result = response.as(Sentiment.class);
```

**After (declarative):**

```java
@AIService
interface SentimentAnalyzer {
    @Instructions("You are a sentiment analyzer")
    @UserMessage("Analyze the sentiment of: {{text}}")
    Sentiment analyze(@V("text") String text);
}

SentimentAnalyzer analyzer = ProtifyAI.create(SentimentAnalyzer.class, client);
Sentiment result = analyzer.analyze("I love this product!");
```

### Basic Usage

1. Define an interface annotated with `@AIService`
2. Annotate methods with `@UserMessage` (the prompt template) and optionally `@Instructions` (the system prompt)
3. Annotate all parameters with `@V` to bind them to `{{placeholder}}` variables in the template
4. Call `ProtifyAI.create()` to get a proxy implementation

```java
@AIService
@Instructions("You are a helpful assistant.")
interface Assistant {
    @UserMessage("Summarize the following text in {{sentences}} sentences: {{text}}")
    String summarize(@V("text") String text, @V("sentences") int sentences);

    @UserMessage("Translate the following text to {{language}}: {{text}}")
    String translate(@V("text") String text, @V("language") String language);
}

AIClient client = AIClient.builder()
        .model(AIModel.CLAUDE_SONNET_4_6)
        .build();

Assistant assistant = ProtifyAI.create(Assistant.class, client);

String summary = assistant.summarize("Long article text here...", 3);
String translated = assistant.translate("Hello world", "French");
```

`@Instructions` on the interface applies to all methods as a default. A method-level `@Instructions` overrides the interface-level value for that method.

### Annotations

| Annotation | Target | Required | Purpose |
|---|---|---|---|
| `@AIService` | Interface | Yes | Marks the interface for proxy generation |
| `@Instructions` | Interface, Method | No | System prompt. Method-level overrides interface-level |
| `@UserMessage` | Method | Yes | Message template with `{{variable}}` placeholders |
| `@V` | Parameter | Yes | Binds a method parameter to a template variable |
| `@Temperature` | Method | No | Per-method temperature override |
| `@MaxTokens` | Method | No | Per-method max output tokens override |
| `@Reasoning` | Method | No | Per-method reasoning effort (`ReasoningEffort.LOW`, `MEDIUM`, `HIGH`) |

### Return Types

The proxy automatically maps the AI response to the method's return type:

| Return Type | Behavior |
|---|---|
| `String` | Returns `response.text()` directly |
| POJO | Deserializes JSON via `response.as(Class)`. Adds "Respond with valid JSON only" guidance to the system prompt |
| `List<T>` | Deserializes JSON array via `response.asList(Class)`. Adds JSON array guidance |
| Enum | Case-insensitive match on `response.text().trim()`. Adds "Respond with exactly one of: VALUE1, VALUE2" guidance |
| `int`, `boolean`, etc. | Parses `response.text().trim()`. Adds format guidance |
| `AIResponse` | Returns the raw response object |
| `AIStreamResponse` | Uses `executeStream()` instead of `execute()` |
| `CompletableFuture<T>` | Wraps execution in `CompletableFuture.supplyAsync()`, maps the inner type |
| `void` | Executes the request but discards the response |

```java
@AIService
interface Analyzer {
    @UserMessage("Classify the sentiment of: {{text}}")
    Sentiment classifySentiment(@V("text") String text);

    @UserMessage("Extract key entities from: {{text}}")
    List<Entity> extractEntities(@V("text") String text);

    @UserMessage("Rate the quality of this text from 1-10: {{text}}")
    int rateQuality(@V("text") String text);

    @UserMessage("Is this text written in English? {{text}}")
    boolean isEnglish(@V("text") String text);
}
```

### Streaming and Async

Return `AIStreamResponse` to stream tokens in real time, or `CompletableFuture<T>` for async execution:

```java
@AIService
interface Writer {
    @UserMessage("Write a story about: {{topic}}")
    AIStreamResponse writeStory(@V("topic") String topic);

    @UserMessage("Summarize: {{text}}")
    CompletableFuture<String> summarizeAsync(@V("text") String text);

    @UserMessage("Analyze: {{text}}")
    CompletableFuture<Analysis> analyzeAsync(@V("text") String text);
}

Writer writer = ProtifyAI.create(Writer.class, client);

// Streaming
AIStreamResponse stream = writer.writeStory("a dragon");
stream.onToken(token -> System.out.print(token));
AIResponse fullResponse = stream.toResponse();

// Async
CompletableFuture<String> future = writer.summarizeAsync("Long text...");
String summary = future.join();
```

### Configuration Overrides

Use `@Temperature` and `@MaxTokens` to override client-level settings on a per-method basis:

```java
@AIService
interface ContentGenerator {
    @Temperature(0.9)
    @MaxTokens(2000)
    @UserMessage("Write a creative story about: {{topic}}")
    String writeCreativeStory(@V("topic") String topic);

    @Temperature(0.1)
    @MaxTokens(100)
    @UserMessage("Extract the main topic from: {{text}}")
    String extractTopic(@V("text") String text);
}
```

### Validation

All annotation and template errors are caught eagerly at `ProtifyAI.create()` time, not at method invocation time:

- The target must be an interface
- The interface must be annotated with `@AIService`
- Every non-default method must have `@UserMessage`
- Every parameter must be annotated with `@V`

```java
// Throws IllegalArgumentException immediately with a descriptive message
ProtifyAI.create(NotAnInterface.class, client);
ProtifyAI.create(MissingAIServiceAnnotation.class, client);
ProtifyAI.create(MethodMissingUserMessage.class, client);
ProtifyAI.create(ParamMissingV.class, client);
```

---

## Streaming

Stream tokens in real time from any single request or pipeline:

```java
AIStreamResponse stream = client.newRequest()
        .addInput("Tell me a story about a dragon.")
        .build()
        .executeStream();

// Register a listener for real-time token output
stream.onToken(token -> System.out.print(token));

// Block until the stream completes and get the full response
AIResponse fullResponse = stream.toResponse();
System.out.println("\n\nTotal tokens: " + fullResponse.getTotalTokens());
```

`onToken()` fires for each text chunk as it arrives from the provider. `toResponse()` blocks until the stream finishes and returns the accumulated `AIResponse`.

---

## Async Execution

Both requests and pipelines support non-blocking execution via `executeAsync()`, which returns a `CompletableFuture<AIResponse>`:

### Async Requests

```java
CompletableFuture<AIResponse> future = client.newRequest()
        .addInput("What is the capital of France?")
        .build()
        .executeAsync();

// Do other work while the request runs...
AIResponse response = future.join();
System.out.println(response.text());
```

### Fire-and-Forget with Callbacks

```java
client.newRequest()
        .addInput("Summarize this article.")
        .build()
        .executeAsync()
        .thenAccept(response -> System.out.println("Done: " + response.text()))
        .exceptionally(ex -> { System.err.println("Failed: " + ex.getMessage()); return null; });
```

### Parallel Requests

Run multiple independent requests concurrently and collect results:

```java
CompletableFuture<AIResponse> english = client.newRequest()
        .addInput("Translate to English: Bonjour le monde")
        .build()
        .executeAsync();

CompletableFuture<AIResponse> spanish = client.newRequest()
        .addInput("Translate to Spanish: Bonjour le monde")
        .build()
        .executeAsync();

CompletableFuture<AIResponse> german = client.newRequest()
        .addInput("Translate to German: Bonjour le monde")
        .build()
        .executeAsync();

CompletableFuture.allOf(english, spanish, german).join();

System.out.println("English: " + english.join().text());
System.out.println("Spanish: " + spanish.join().text());
System.out.println("German: " + german.join().text());
```

### Async Pipelines

Pipelines also support `executeAsync()`:

```java
AIPipeline pipeline = AIPipeline.builder()
        .withInitialStep(() -> client.newRequest()
                .addInput("Write a short story.")
                .build())
        .addStep(ctx -> PipelineAIResponse.of(ctx.text().toUpperCase()))
        .build();

CompletableFuture<AIResponse> future = pipeline.executeAsync();

// Do other work...
AIResponse result = future.join();
```

`executeAsync()` runs the entire execution on a background thread from the common `ForkJoinPool`. For custom thread pools, wrap `execute()` in your own `CompletableFuture.supplyAsync(pipeline::execute, yourExecutor)`.

---

## Pipelines

Pipelines chain multiple steps together. Each step receives the previous step's response via `AIPipelineContext` and produces an `AIResponse`.

### Basic Pipeline

```java
AIPipeline pipeline = AIPipeline.builder()
        .withInitialStep(() -> client.newRequest()
                .addInput("Write a 200-word essay about space exploration.")
                .build())
        .addStep(ctx -> {
            // ctx.text() returns the previous step's output text
            String essay = ctx.text();
            return PipelineAIResponse.of(essay.toUpperCase());
        })
        .build();

AIResponse result = pipeline.execute();
System.out.println(result.text());
```

### Request Steps

`addRequestStep` is a convenience for steps that build and execute an AI request. The request automatically receives the previous step's output as input:

```java
AIPipeline pipeline = AIPipeline.builder()
        .withInitialStep(() -> openAIClient.newRequest()
                .addInput("Write a short story about a robot.")
                .build())
        .addRequestStep(ctx -> anthropicClient.newRequest()
                .addInput("Translate the following story to French:")
                .addInput(ctx.response())
                .build())
        .addRequestStep(ctx -> openAIClient.newRequest()
                .addInput("Summarize this French text in one sentence:")
                .addInput(ctx.response())
                .build())
        .build();

AIResponse result = pipeline.execute();
```

### Transformation Steps

Use lambda steps for non-LLM transformations:

```java
AIPipeline pipeline = AIPipeline.builder()
        .withInitialStep(() -> client.newRequest()
                .addInput("Generate a list of 10 random words.")
                .build())
        .addStep(ctx -> {
            // Pure Java transformation — no LLM call
            String sorted = Arrays.stream(ctx.text().split(","))
                    .map(String::trim)
                    .sorted()
                    .collect(Collectors.joining(", "));
            return PipelineAIResponse.of(sorted);
        })
        .build();
```

### Parallel Steps

Execute multiple steps concurrently and join results:

```java
AIPipeline pipeline = AIPipeline.builder()
        .withInitialStep(() -> client.newRequest()
                .addInput("Write a paragraph about climate change.")
                .build())
        .addParallelStep(List.of(
                AIPipelineContext::response,  // forward the original text
                ctx -> openAIClient.newRequest()
                        .addInput("Translate to French: " + ctx.text())
                        .build().execute(),
                ctx -> anthropicClient.newRequest()
                        .addInput("Translate to Spanish: " + ctx.text())
                        .build().execute(),
                ctx -> openAIClient.newRequest()
                        .addInput("Translate to German: " + ctx.text())
                        .build().execute()
        ))
        .build();

// Result text is all outputs joined with "\n---\n"
AIResponse result = pipeline.execute();
```

### Conditional Steps

Branch pipeline execution based on the output of previous steps:

```java
AIPipeline pipeline = AIPipeline.builder()
        .withInitialStep(() -> client.newRequest()
                .addInput("Analyze the sentiment of: 'I love this product!'")
                .build())
        .addConditionalStep(conditional -> conditional
                .when(ctx -> ctx.text().toLowerCase().contains("positive"),
                        ctx -> client.newRequest()
                                .addInput("Generate a thank-you response for: " + ctx.text())
                                .build().execute())
                .when(ctx -> ctx.text().toLowerCase().contains("negative"),
                        ctx -> client.newRequest()
                                .addInput("Generate an apology response for: " + ctx.text())
                                .build().execute())
                .otherwise(ctx -> client.newRequest()
                        .addInput("Generate a neutral acknowledgment for: " + ctx.text())
                        .build().execute())
        )
        .build();

AIResponse result = pipeline.execute();
```

Each branch can be a lambda step, a request step, or an entire pipeline (see below). Conditions are evaluated in order; the first match wins. If no condition matches and no `otherwise` is provided, the previous response passes through unchanged.

### Safe Steps (Error Handling)

Wrap any step with retry logic and an optional fallback using `addSafeStep`:

```java
AIPipeline pipeline = AIPipeline.builder()
        .withInitialStep(() -> client.newRequest()
                .addInput("Generate a report.")
                .build())
        .addSafeStep(safe -> safe
                .step(ctx -> client.newRequest()
                        .addInput("Refine: " + ctx.text())
                        .build().execute())
                .maxRetries(3)
                .onError((ctx, ex) -> PipelineAIResponse.of("Fallback response"))
        )
        .build();

AIResponse result = pipeline.execute();
```

- `step(PipelineStep)` — the step to execute (required)
- `maxRetries(int)` — number of retries before invoking the fallback (default `0`)
- `onError(BiFunction<AIPipelineContext, Exception, AIResponse>)` — fallback handler that receives the context and the last exception (optional; if not set, the exception propagates after retries are exhausted)

Retries with no fallback — the exception propagates after 2 attempts:

```java
.addSafeStep(safe -> safe
        .step(ctx -> unreliableService.call(ctx.text()))
        .maxRetries(2)
)
```

Fallback only, no retries — immediately falls back on any error:

```java
.addSafeStep(safe -> safe
        .step(ctx -> client.newRequest()
                .addInput(ctx.text())
                .build().execute())
        .onError((ctx, ex) -> PipelineAIResponse.of("Default answer"))
)
```

### Loop Steps

Repeat a step until a condition is met using `addLoopStep`:

```java
AIPipeline pipeline = AIPipeline.builder()
        .withInitialStep(() -> client.newRequest()
                .addInput("Describe a sunset.")
                .build())
        .addLoopStep(loop -> loop
                .step(ctx -> client.newRequest()
                        .addInput("Generate valid JSON for: " + ctx.text())
                        .build().execute())
                .until(ctx -> isValidJson(ctx.text()))
                .maxIterations(5)
                .onMaxIterations(ctx -> PipelineAIResponse.of(ctx.text()))
        )
        .build();

AIResponse result = pipeline.execute();
```

- `step(PipelineStep)` — the step to repeat (required)
- `until(Predicate<AIPipelineContext>)` — loop stops when this returns `true` (required)
- `maxIterations(int)` — hard cap to prevent infinite loops (default `5`)
- `onMaxIterations(PipelineStep)` — fallback when max iterations are reached without the condition passing (optional; defaults to returning the last iteration's result)

On each iteration the step's output is set as the context's previous response, so `ctx.text()` returns the latest iteration's output. This enables "refine until good enough" patterns where each iteration builds on the previous result.

When streaming (`executeStream()`), loop steps run all iterations in batch and return the final result as a completed stream, since the termination condition requires the full output to evaluate.

### Composable Pipelines (Pipeline as a Step)

Since `AIPipeline` implements `PipelineStep`, any pipeline can be used as a step inside another pipeline:

```java
// Reusable sub-pipeline: translate and back-translate for quality check
AIPipeline translateAndVerify = AIPipeline.builder()
        .inheritParentConfig()  // inherit temperature, retry policy, etc.
        .withInitialStep(() -> openAIClient.newRequest()
                .addInput("Translate the following to French")
                .build())
        .addRequestStep(ctx -> anthropicClient.newRequest()
                .addInput("Translate this French text back to English:")
                .addInput(ctx.response())
                .build())
        .build();

// Outer pipeline uses the sub-pipeline as a step
AIPipeline outer = AIPipeline.builder()
        .withInitialStep(() -> openAIClient.newRequest()
                .addInput("Write a paragraph about quantum computing.")
                .build())
        .addStep(translateAndVerify)  // pipeline as a step
        .addRequestStep(ctx -> anthropicClient.newRequest()
                .addInput("Compare the original and back-translated versions:")
                .addInput(ctx.response())
                .build())
        .build();

AIResponse result = outer.execute();
```

Sub-pipelines can also be used inside conditional branches:

```java
AIPipeline errorRecovery = AIPipeline.builder()
        .withInitialStep(() -> client.newRequest()
                .addInput("Rephrase and retry the failed request")
                .build())
        .addRequestStep(ctx -> client.newRequest()
                .addInput("Validate the rephrased output: " + ctx.text())
                .build())
        .build();

AIPipeline pipeline = AIPipeline.builder()
        .withInitialStep(() -> client.newRequest()
                .addInput("Perform a complex analysis.")
                .build())
        .addConditionalStep(conditional -> conditional
                .when(ctx -> ctx.text().contains("ERROR"), errorRecovery)
                .otherwise(ctx -> ctx.response())
        )
        .build();
```

### Pipeline Streaming

`executeStream()` runs all intermediate steps in batch and streams only the final step:

```java
AIPipeline pipeline = AIPipeline.builder()
        .withInitialStep(() -> openAIClient.newRequest()
                .addInput("Summarize this document")
                .build())
        .addStep(ctx -> PipelineAIResponse.of(ctx.text().replace("AI", "Artificial Intelligence")))
        .addRequestStep(ctx -> anthropicClient.newRequest()
                .addInput("Expand on: " + ctx.text())
                .build())
        .build();

AIStreamResponse stream = pipeline.executeStream();
stream.onToken(token -> System.out.print(token));
AIResponse finalResponse = stream.toResponse();
```

When a pipeline is used as a step and it's the final step, streaming flows through: the outer pipeline's `executeStream()` calls the inner pipeline's `executeStream()`, which streams its own final step.

### Pipeline Configuration

Pipeline-level settings apply as defaults to all requests within the pipeline:

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
        .onStepComplete(output -> System.out.println("Step done: " + output.substring(0, 50)))
        .withInitialStep(() -> client.newRequest()
                .addInput("Begin the analysis.")
                .build())
        .build();
```

---

## Configuration Hierarchy

Settings are resolved in this order (highest priority first):

1. **Request-level** - set on `AIRequestBuilder`
2. **Client-level** - set on `AIClientBuilder`
3. **Pipeline-level** - set on `AIPipelineBuilder` (only applies within a pipeline)
4. **Base configuration** - loaded at `ProtifyAI.initialize()`

A client or request can set `overridePipelineConfig(true)` to force its settings to take precedence over pipeline-level settings.

When using `inheritParentConfig()` on a sub-pipeline, the parent pipeline's properties are inherited as defaults, with the sub-pipeline's own properties taking precedence:

```java
AIPipeline parent = AIPipeline.builder()
        .temperature(0.5)
        .maxOutputTokens(1000)
        // ...
        .build();

AIPipeline child = AIPipeline.builder()
        .inheritParentConfig()      // inherits temperature=0.5, maxOutputTokens=1000
        .temperature(0.9)           // overrides temperature to 0.9
        // maxOutputTokens remains 1000 (inherited)
        // ...
        .build();
```

---

## Retry Policies

Configure automatic retry behavior for transient failures:

```java
RetryPolicy policy = RetryPolicy.builder()
        .maxRetries(3)
        .backoffStrategy(RetryBackoffStrategy.EXPONENTIAL)
        .delayMillis(500)
        .maxDelayMillis(10000)
        .jitterMillis(200)
        .maxElapsedTimeMillis(30000)
        .retryOnHttpStatusCodes(Set.of(429, 500, 502, 503, 504))
        .retryOnExceptions(Set.of(RuntimeException.class))
        .respectRetryAfter(true)
        .build();

AIClient client = AIClient.builder()
        .model(AIModel.GPT_5_1)
        .retryPolicy(policy)
        .build();
```

The default retry policy has `maxRetries(0)` (no retries). Retry policies can be set at the client, request, or pipeline level.

---

## API Key Resolution

API keys are resolved in this order:

1. **Environment variable** - each provider has a standard env var (see table below)
2. **Explicit configuration** - set via `AIClientBuilder.apiKey("sk-...")`
3. **Webhook resolution** - configure `providers.apiKeyUrl` and `providers.protifyApiKey` in base configuration for dynamic API key retrieval via a webhook endpoint

If both an environment variable and explicit key are set, the environment variable takes precedence (with a warning logged).

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
| Azure AI Foundry | `AZURE_AI_FOUNDRY_API_KEY` |
| Vertex AI | `VERTEX_AI_ACCESS_TOKEN` |
| AWS Bedrock | `AWS_ACCESS_KEY_ID` + `AWS_SECRET_ACCESS_KEY` (+ optional `AWS_SESSION_TOKEN`) + `AWS_REGION` |

---

## Full Example

A complete pipeline demonstrating multiple providers, transformations, parallel execution, conditional logic, and streaming:

```java

import ai.protify.core.internal.pipeline.PipelineAIResponse;

import java.util.List;

public class FullExample {
    public static void main(String[] args) {
        ProtifyAI.initialize();

        AIClient openAI = AIClient.builder()
                .model(AIModel.GPT_5_1)
                .instructions("Talk like a pirate.")
                .maxOutputTokens(500)
                .build();

        AIClient anthropic = AIClient.builder()
                .model(AIModel.CLAUDE_SONNET_4_6)
                .maxOutputTokens(500)
                .build();

        AIPipeline pipeline = AIPipeline.builder()
                // Step 1: Generate essay
                .withInitialStep(() -> openAI.newRequest()
                        .addInput("Write a 200-word essay about the history of the Chicago Bears.")
                        .build())
                // Step 2: Text transformation
                .addStep(ctx -> {
                    String essay = ctx.text().replace("Chicago Bears", "CHICAGO BEARS");
                    return PipelineAIResponse.of(essay);
                })
                // Step 3: Parallel translations
                .addParallelStep(List.of(
                        AIPipelineContext::response,
                        ctx -> openAI.newRequest()
                                .addInput("Translate to French: " + ctx.text())
                                .build().execute(),
                        ctx -> anthropic.newRequest()
                                .addInput("Translate to Spanish: " + ctx.text())
                                .build().execute(),
                        ctx -> openAI.newRequest()
                                .addInput("Translate to German: " + ctx.text())
                                .build().execute()
                ))
                // Step 4: Conditional — different analysis based on output size
                .addConditionalStep(conditional -> conditional
                        .when(ctx -> ctx.text().length() > 5000,
                                ctx -> anthropic.newRequest()
                                        .addInput("Provide a brief summary of each language section:")
                                        .addInput(ctx.response())
                                        .build().execute())
                        .otherwise(ctx -> anthropic.newRequest()
                                .addInput("Provide a word count table by language " +
                                          "(English, French, Spanish, German):")
                                .addInput(ctx.response())
                                .build().execute())
                )
                .onStepComplete(output -> System.out.println("--- Step complete ---"))
                .build();

        // Execute with streaming on the final step
        AIStreamResponse stream = pipeline.executeStream();
        stream.onToken(token -> System.out.print(token));
        AIResponse finalResponse = stream.toResponse();

        System.out.println("\n\nDone. Tokens used: " + finalResponse.getTotalTokens());
    }
}
```

---

## Tool Use

The SDK supports defining tools, detecting tool calls in AI responses, and executing tool loops — both manually and automatically.

### Defining Tools

```java
AITool weatherTool = AITool.builder("get_weather")
    .description("Get current weather for a location")
    .addRequiredParameter("location", AIToolParameter.string("City name"))
    .addParameter("units", AIToolParameter.stringEnum("Temperature units", List.of("celsius", "fahrenheit")))
    .build();
```

**Parameter types:** `AIToolParameter.string()`, `number()`, `integer()`, `bool()`, `stringEnum()`, `object()`, `array()`

### Manual Tool Loop

Use this when you need full control over tool execution — for example, to call external services, validate arguments, or apply business logic between rounds.

```java
AIResponse response = client.newRequest()
    .addInput("What's the weather in NYC?")
    .addTool(weatherTool)
    .build().execute();

while (response.hasToolCalls()) {
    AIRequestBuilder followUp = client.newRequest()
        .addInput("What's the weather in NYC?")
        .addTool(weatherTool)
        .previousResponse(response);

    for (AIToolCall call : response.getToolCalls()) {
        String result = myWeatherApi(call.getArguments());
        followUp.addToolResult(new AIToolResult(call.getId(), result));
    }
    response = followUp.build().execute();
}

System.out.println(response.text());
```

**`AIToolCall`** provides:
- `getId()` — unique call identifier (needed for tool results)
- `getName()` — the tool name
- `getArguments()` — parsed arguments as `Map<String, Object>`
- `getArgumentsJson()` — raw JSON string
- `getArgumentsAs(Class<T>)` — deserialize to a typed object

### Automatic Tool Loop

Register a handler with each tool and the SDK loops internally until the model produces a text response or the maximum number of rounds is reached.

```java
AIResponse response = client.newRequest()
    .addInput("What's the weather in NYC?")
    .addTool(weatherTool, args -> myWeatherApi(args))
    .maxToolRounds(5)
    .build().execute();

// SDK loops internally until text response or max rounds
System.out.println(response.text());
```

If a handler throws an exception, the SDK sends the error message back to the model as an error tool result, allowing the model to recover or report the issue.

---

## Conversations

Conversations maintain a running dialogue with a single LLM, where each turn sees the full message history. This is the third interaction pattern alongside one-shot `AIRequest` and multi-step `AIPipeline`.

### In-Memory Conversation

```java
AIConversation conversation = client.newConversation()
        .instructions("You are a helpful assistant")
        .build();

AIResponse r1 = conversation.send("What's the capital of France?");
System.out.println(r1.text());  // "The capital of France is Paris."

AIResponse r2 = conversation.send("What about Germany?");
System.out.println(r2.text());  // "The capital of Germany is Berlin."

// Access the full message history
List<AIMessage> history = conversation.getMessages();
```

Each `send()` call appends the user message and assistant response to the conversation history. The full history is sent with every request, so the model has complete context.

### Persistent Store

Implement `AIConversationStore` to persist conversation state across service instances:

```java
AIConversationStore store = new MyRedisStore(); // your implementation

// Start a new conversation with a store
AIConversation conversation = client.newConversation()
        .store(store)
        .build();

String id = conversation.getId();  // UUID — save this for later
conversation.send("Hello!");

// Resume on any service instance
AIConversation resumed = client.loadConversation(id, store);
AIResponse response = resumed.send("Continue from where we left off");
```

The `AIConversationStore` interface has three methods:

```java
public interface AIConversationStore {
    void save(AIConversationState state);
    AIConversationState load(String conversationId);
    void delete(String conversationId);
}
```

State is serialized as JSON via `AIConversationState.toJson()` / `fromJson()` — store implementations just need to persist and retrieve the JSON string.

### Conversations with Tools

Register tools with handlers and the conversation manages the tool loop internally within each `send()` call:

```java
AIConversation conversation = client.newConversation()
        .addTool(weatherTool, args -> getWeather(args))
        .maxToolRounds(5)
        .build();

AIResponse response = conversation.send("What's the weather in NYC?");
// Tool calls and results are automatically recorded in the message history
System.out.println(response.text());
```

The full tool exchange (assistant tool calls, user tool results) is captured in the conversation history, so subsequent turns have full context of what happened.

### File Inputs in Conversations

Send images, PDFs, and other file inputs alongside text. File inputs are sent to the model but **not persisted** to the store — the assistant's text response captures what matters:

```java
AIResponse response = conversation.send("Describe this image",
        AIFileInput.fromUrl("https://example.com/photo.jpg"));
```

### Conversation State

Access and manage conversation state:

```java
// Get current state (for manual serialization)
AIConversationState state = conversation.getState();
String json = state.toJson();

// Reconstruct state from JSON
AIConversationState restored = AIConversationState.fromJson(json);

// Reset the conversation
conversation.clear();
```

---

## Reasoning Models

Many modern LLMs support extended thinking — the model reasons through a problem before responding. Protify AI provides a unified `ReasoningEffort` API that works across providers, automatic parameter validation for reasoning models, and access to the model's reasoning output.

### Reasoning Effort

Use `reasoningEffort()` to enable extended thinking on any builder — client, request, conversation, or pipeline:

```java
import ai.protify.core.ReasoningEffort;

// Client-level default — all requests use extended thinking
AIClient client = AIClient.builder()
        .model(AIModel.CLAUDE_OPUS_4_6)
        .reasoningEffort(ReasoningEffort.HIGH)
        .maxOutputTokens(16000)
        .build();

// Per-request override
AIResponse response = client.newRequest()
        .addInput("Prove that the square root of 2 is irrational.")
        .reasoningEffort(ReasoningEffort.MEDIUM)
        .build()
        .execute();

// In a conversation
AIConversation conversation = client.newConversation()
        .instructions("You are a math tutor")
        .reasoningEffort(ReasoningEffort.HIGH)
        .build();

// In a pipeline
AIPipeline pipeline = AIPipeline.builder()
        .reasoningEffort(ReasoningEffort.LOW)
        .withInitialStep(/* ... */)
        .build();
```

`ReasoningEffort` has three levels: `LOW`, `MEDIUM`, and `HIGH`. Each level maps to provider-specific parameters:

| Provider | How it's sent |
|---|---|
| Anthropic | `thinking.type: "enabled"` with `budget_tokens` (low=1024, medium=5000, high=16000) |
| OpenAI | `reasoning.effort` set to `"low"`, `"medium"`, or `"high"` |
| Gemini | `generationConfig.thinkingConfig.thinkingBudget` (low=1024, medium=8192, high=24576) |
| AWS Bedrock | `additionalModelRequestFields.thinking` with `budget_tokens` |
| DeepSeek | Not needed — `deepseek-reasoner` always reasons |

### Reasoning Content in Responses

When a model uses extended thinking, the reasoning output is available via `getReasoningContent()`:

```java
AIClient client = AIClient.builder()
        .model(AIModel.CLAUDE_OPUS_4_6)
        .reasoningEffort(ReasoningEffort.HIGH)
        .maxOutputTokens(16000)
        .build();

AIResponse response = client.newRequest()
        .addInput("What is the probability of drawing a flush in poker?")
        .build()
        .execute();

String answer    = response.text();                // the final answer
String reasoning = response.getReasoningContent(); // the model's chain-of-thought (or null)

if (reasoning != null) {
    System.out.println("Reasoning: " + reasoning);
}
System.out.println("Answer: " + answer);
```

`getReasoningContent()` returns `null` when the model did not produce reasoning output (e.g., reasoning was not enabled, or the provider doesn't surface it).

The reasoning content is extracted from provider-specific response formats:

| Provider | Source |
|---|---|
| Anthropic | Content blocks with `type: "thinking"` |
| OpenAI | Output items with `type: "reasoning"` and `summary` |
| Gemini | Parts with `thought: true` |
| DeepSeek / xAI | `reasoning_content` field on the response message |
| AWS Bedrock | Content blocks with `reasoning.text` |

### Parameter Validation

Reasoning models typically don't support `temperature`, `topP`, or `topK`. Protify AI automatically detects these conflicts and excludes the unsupported parameters with a warning log — no API errors, no code changes needed:

```java
// This works without errors, even though DeepSeek Reasoner doesn't support temperature
AIClient client = AIClient.builder()
        .model(AIModel.DEEPSEEK_REASONER)
        .temperature(0.7)  // automatically excluded with a warning
        .build();
```

Parameters are automatically suppressed for:

| Model | Suppressed Parameters |
|---|---|
| `deepseek-reasoner` | `temperature`, `topP` |
| OpenAI GPT-5 family | `temperature` |
| xAI Grok reasoning models | `temperature`, `topP` |
| Any model with `reasoningEffort` set (Anthropic, Bedrock) | `temperature`, `topP`, `topK` |

### Reasoning in Declarative AI Services

Use the `@Reasoning` annotation to enable extended thinking on AI service methods:

```java
import ai.protify.core.service.*;
import ai.protify.core.ReasoningEffort;

@AIService
interface MathTutor {

    @Reasoning(ReasoningEffort.HIGH)
    @MaxTokens(16000)
    @UserMessage("Solve this step by step: {{problem}}")
    String solve(@V("problem") String problem);

    @Reasoning(ReasoningEffort.LOW)
    @UserMessage("What is {{a}} + {{b}}?")
    int add(@V("a") int a, @V("b") int b);
}

MathTutor tutor = ProtifyAI.create(MathTutor.class, client);
String proof = tutor.solve("Prove that there are infinitely many primes");
```

---

## MCP Client

Connect to [Model Context Protocol](https://modelcontextprotocol.io) servers to dynamically discover and use tools.

### Stdio Transport

```java
MCPClient mcp = MCPClient.stdio("npx", "-y", "@modelcontextprotocol/server-filesystem", "/tmp");
mcp.connect();

AIRequestBuilder builder = client.newRequest().addInput("List files in /tmp");
for (AITool tool : mcp.listTools()) {
    builder.addTool(tool, args -> mcp.callTool(tool.getName(), args));
}
AIResponse response = builder.maxToolRounds(10).build().execute();

System.out.println(response.text());
mcp.close();
```

### HTTP Transport

```java
MCPClient mcp = MCPClient.http("http://localhost:8080/mcp");
mcp.connect();
// ... same usage as above
mcp.close();
```

`MCPClient` implements `AutoCloseable`, so you can use it with try-with-resources:

```java
try (MCPClient mcp = MCPClient.stdio("npx", "-y", "@modelcontextprotocol/server-filesystem", "/tmp")) {
    mcp.connect();
    // ...
}
```

---

## Documentation

The [`docs/`](docs/) folder contains detailed guides:

| Guide | Description |
|---|---|
| [Spring Boot Starter](docs/spring-boot-starter.md) | Auto-configuration for Spring Boot 2.x/3.x/4.x — default and named multi-provider clients, `@AIService` bean scanning, `@Qualifier` injection, and full property reference |
| [Configuration Guide](docs/configuration-guide.md) | Base properties files, profiles, configuration hierarchy, retry policies, cloud provider setup, API key resolution, and a complete property reference table |
| [Custom Provider Guide](docs/custom-provider-guide.md) | Step-by-step walkthrough for adding your own AI provider — registration, request/client classes, with full code examples |
| [AI Agent Guide](docs/AI-AGENT-GUIDE.md) | Compact SDK reference optimized for AI coding agents — see [Using with AI Coding Agents](#using-with-ai-coding-agents) above |
