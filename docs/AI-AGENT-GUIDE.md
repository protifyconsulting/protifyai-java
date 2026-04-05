# Protify AI - Java SDK Reference for AI Agents

This document is designed for AI coding agents (Claude, Copilot, Cursor, etc.) to quickly understand and use the Protify AI Java SDK. It covers the full API surface with copy-paste examples.

## What This Is

Protify AI is a zero-dependency, provider-agnostic Java SDK for AI. One API works across 13 providers: OpenAI, Anthropic, Google Gemini, Mistral, Groq, DeepSeek, Together, Fireworks, xAI, Azure OpenAI, Azure AI Foundry, Google Vertex AI, and AWS Bedrock.

**Maven coordinates (core library):**
```xml
<dependency>
    <groupId>ai.protify</groupId>
    <artifactId>protifyai</artifactId>
    <version>0.1.8</version>
</dependency>
```

**Gradle (core library):**
```groovy
implementation 'ai.protify:protifyai:0.1.8'
```

**Spring Boot Starter** (includes core automatically):
```groovy
implementation 'ai.protify:protifyai-spring-boot-starter:0.1.8'
```
```xml
<dependency>
    <groupId>ai.protify</groupId>
    <artifactId>protifyai-spring-boot-starter</artifactId>
    <version>0.1.8</version>
</dependency>
```

**Core: Java 11+, zero external dependencies. Spring Boot Starter: Java 17+, Spring Boot 2.x/3.x/4.x.**

---

## Package Structure

All public API classes live under `ai.protify.core`:

| Package | Key Classes |
|---|---|
| `ai.protify.core` | `ProtifyAI`, `AIClient`, `AIClientBuilder`, `AIModel`, `ReasoningEffort` |
| `ai.protify.core.request` | `AIRequest`, `AIRequestBuilder`, `AIInput`, `AITextInput`, `AIFileInput` |
| `ai.protify.core.response` | `AIResponse`, `AIStreamResponse` |
| `ai.protify.core.conversation` | `AIConversation`, `AIConversationBuilder`, `AIConversationStore`, `AIConversationState` |
| `ai.protify.core.message` | `AIMessage` |
| `ai.protify.core.pipeline` | `AIPipeline`, `AIPipelineBuilder`, `AIPipelineContext`, `PipelineStep` |
| `ai.protify.core.tool` | `AITool`, `AIToolBuilder`, `AIToolParameter`, `AIToolCall`, `AIToolResult`, `AIToolHandler` |
| `ai.protify.core.service` | `@AIService`, `@Instructions`, `@UserMessage`, `@V`, `@Temperature`, `@MaxTokens`, `@Reasoning` |
| `ai.protify.core.resiliency` | `RetryPolicy`, `RetryPolicyBuilder`, `RetryBackoffStrategy` |
| `ai.protify.core.provider` | `AIProvider`, `AIProviderClient` |
| `ai.protify.core.mcp` | `MCPClient` |
| `ai.protify.core.internal.pipeline` | `PipelineAIResponse` (used in pipeline steps and simple response creation) |
| `ai.protify.spring` | `ProtifyAIAutoConfiguration`, `ProtifyAIProperties`, `AIClientRegistry`, `EnableProtifyAI` (Spring Boot starter only) |

---

## Quick Patterns

### Initialize (required once at startup)

```java
import ai.protify.core.ProtifyAI;
ProtifyAI.initialize();
```

### Simple Request

```java
import ai.protify.core.AIClient;
import ai.protify.core.AIModel;
import ai.protify.core.response.AIResponse;

AIClient client = AIClient.builder()
        .model(AIModel.CLAUDE_SONNET_4_6)
        .build();

AIResponse response = client.newRequest()
        .addInput("What is the capital of France?")
        .build()
        .execute();

String answer = response.text();
```

### With System Instructions

```java
AIClient client = AIClient.builder()
        .model(AIModel.GPT_5_4)
        .instructions("You are a helpful assistant. Be concise.")
        .build();
```

### With Configuration

```java
AIClient client = AIClient.builder()
        .model(AIModel.GEMINI_2_5_PRO)
        .instructions("You are a helpful assistant.")
        .temperature(0.7)
        .maxOutputTokens(1000)
        .topP(0.9)
        .reasoningEffort(ReasoningEffort.HIGH)
        .retryPolicy(RetryPolicy.builder().maxRetries(3).build())
        .build();
```

### Streaming

```java
import ai.protify.core.response.AIStreamResponse;

AIStreamResponse stream = client.newRequest()
        .addInput("Tell me a story.")
        .build()
        .executeStream();

stream.onToken(token -> System.out.print(token));
AIResponse fullResponse = stream.toResponse(); // blocks until complete
```

### Async

```java
CompletableFuture<AIResponse> future = client.newRequest()
        .addInput("Summarize this text.")
        .build()
        .executeAsync();

AIResponse response = future.join();
```

### Structured Output (JSON to POJO)

```java
// Single object
MovieReview review = client.newRequest()
        .addInput("Review Inception. Respond in JSON: title, rating (1-10), summary.")
        .build().execute()
        .as(MovieReview.class);

// List
List<MovieReview> reviews = client.newRequest()
        .addInput("Review 3 movies as a JSON array.")
        .build().execute()
        .asList(MovieReview.class);
```

### File Input (Images, PDFs)

```java
import ai.protify.core.request.AIFileInput;

AIResponse response = client.newRequest()
        .addInput("Describe this image:")
        .addInput(AIFileInput.fromFile(new File("photo.jpg")))
        .build().execute();

// Also: fromClasspath(), fromFilePath(), fromUrl(), fromDataUrl()
```

### Conversations (Multi-Turn)

```java
import ai.protify.core.conversation.AIConversation;

AIConversation conversation = client.newConversation()
        .instructions("You are a helpful assistant.")
        .build();

AIResponse r1 = conversation.send("What's the capital of France?");
AIResponse r2 = conversation.send("What about Germany?"); // has full context

// Streaming in conversations
AIStreamResponse stream = conversation.sendStream("Tell me more about Berlin.");
stream.onToken(token -> System.out.print(token));
stream.toResponse();
```

### Conversations with Persistence

```java
import ai.protify.core.conversation.AIConversationStore;

AIConversation conversation = client.newConversation()
        .store(myStore) // your AIConversationStore implementation
        .build();

String id = conversation.getId(); // save this
conversation.send("Hello!");

// Resume later, even on a different server
AIConversation resumed = client.loadConversation(id, myStore);
resumed.send("Continue from where we left off.");
```

### Tool Use (Automatic Loop)

```java
import ai.protify.core.tool.AITool;
import ai.protify.core.tool.AIToolParameter;

AITool weatherTool = AITool.builder("get_weather")
        .description("Get current weather for a location")
        .addRequiredParameter("location", AIToolParameter.string("City name"))
        .addParameter("units", AIToolParameter.stringEnum("Units", List.of("celsius", "fahrenheit")))
        .build();

AIResponse response = client.newRequest()
        .addInput("What's the weather in NYC?")
        .addTool(weatherTool, args -> callWeatherApi(args))
        .maxToolRounds(5)
        .build().execute();
// SDK loops automatically until the model produces a text response
```

### Tool Use (Manual Loop)

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
        String result = callWeatherApi(call.getArguments());
        followUp.addToolResult(new AIToolResult(call.getId(), result));
    }
    response = followUp.build().execute();
}
```

### Tool Parameter Types

```java
AIToolParameter.string("description")
AIToolParameter.number("description")
AIToolParameter.integer("description")
AIToolParameter.bool("description")
AIToolParameter.stringEnum("description", List.of("option1", "option2"))
AIToolParameter.object("description", Map.of("field", AIToolParameter.string("...")))
AIToolParameter.array("description", AIToolParameter.string("item description"))
```

### Declarative AI Services

```java
import ai.protify.core.service.*;

@AIService
@Instructions("You are a sentiment analyzer.")
interface SentimentAnalyzer {
    @UserMessage("Classify the sentiment of: {{text}}")
    Sentiment classify(@V("text") String text);

    @UserMessage("Rate quality 1-10: {{text}}")
    int rate(@V("text") String text);

    @Temperature(0.9)
    @MaxTokens(2000)
    @UserMessage("Write a story about: {{topic}}")
    AIStreamResponse writeStory(@V("topic") String topic);

    @Reasoning(ReasoningEffort.HIGH)
    @UserMessage("Solve: {{problem}}")
    String solve(@V("problem") String problem);

    @UserMessage("Summarize: {{text}}")
    CompletableFuture<String> summarizeAsync(@V("text") String text);
}

// Without Spring Boot:
SentimentAnalyzer analyzer = ProtifyAI.create(SentimentAnalyzer.class, client);
Sentiment result = analyzer.classify("I love this product!");

// With Spring Boot: just @Autowired (no ProtifyAI.create() needed)
// @AIService also accepts client = "name" for named clients:
// @AIService(client = "gpt")
```

**Supported return types:** `String`, POJOs (JSON deserialization), `List<T>`, enums, primitives (`int`, `boolean`), `AIResponse`, `AIStreamResponse`, `CompletableFuture<T>`, `void`.

### Pipelines

```java
import ai.protify.core.pipeline.AIPipeline;
import ai.protify.core.internal.pipeline.PipelineAIResponse;

AIPipeline pipeline = AIPipeline.builder()
        .withInitialStep(() -> client.newRequest()
                .addInput("Write a short essay.")
                .build())
        .addStep(ctx -> PipelineAIResponse.of(ctx.text().toUpperCase()))
        .addRequestStep(ctx -> anotherClient.newRequest()
                .addInput("Translate: " + ctx.text())
                .build())
        .build();

AIResponse result = pipeline.execute();
// Also: pipeline.executeAsync(), pipeline.executeStream()
```

**Pipeline step types:**
- `addStep(PipelineStep)` -- lambda transformation or LLM call
- `addRequestStep(Function<AIPipelineContext, AIRequest>)` -- auto-executes the returned request
- `addParallelStep(List<PipelineStep>)` -- concurrent execution, results joined with `\n---\n`
- `addConditionalStep(...)` -- branching with `.when()` / `.otherwise()`
- `addSafeStep(...)` -- retry + fallback wrapper
- `addLoopStep(...)` -- repeat until condition met

### MCP (Model Context Protocol)

```java
import ai.protify.core.mcp.MCPClient;

// Stdio transport
MCPClient mcp = MCPClient.stdio("npx", "-y", "@modelcontextprotocol/server-filesystem", "/tmp");
mcp.connect();

AIRequestBuilder builder = client.newRequest().addInput("List files in /tmp");
for (AITool tool : mcp.listTools()) {
    builder.addTool(tool, args -> mcp.callTool(tool.getName(), args));
}
AIResponse response = builder.maxToolRounds(10).build().execute();

mcp.close();

// HTTP transport
MCPClient mcp = MCPClient.http("http://localhost:8080/mcp");
```

---

## Spring Boot Starter

The starter auto-configures `AIClient` beans and `@AIService` proxies. See [`docs/spring-boot-starter.md`](spring-boot-starter.md) for full documentation.

### Dependency

```groovy
implementation 'ai.protify:protifyai-spring-boot-starter:0.1.8'
```

### Minimal application.yml

```yaml
protify:
  ai:
    defaults:
      provider: anthropic
      model: claude-sonnet-4-6
```

### Multi-Provider Setup

```yaml
protify:
  ai:
    defaults:
      provider: anthropic
      api-key: ${ANTHROPIC_API_KEY}
      model: claude-sonnet-4-6
    clients:
      gpt:
        provider: openai
        api-key: ${OPENAI_API_KEY}
        model: gpt-4.1
      gemini:
        provider: google
        api-key: ${GEMINI_API_KEY}
        model: gemini-2.5-flash
```

### @AIService with Spring (auto-registered as beans)

```java
import ai.protify.core.service.*;

@AIService  // uses default client
@Instructions("You are a summarizer.")
public interface SummaryService {
    @UserMessage("Summarize: {{text}}")
    String summarize(@V("text") String text);
}

@AIService(client = "gpt")  // uses named "gpt" client
public interface TranslationService {
    @UserMessage("Translate to {{lang}}: {{text}}")
    String translate(@V("text") String text, @V("lang") String lang);
}
```

### Injecting services and clients

```java
@RestController
public class MyController {
    @Autowired private SummaryService summaryService;          // @AIService proxy
    @Autowired private TranslationService translationService;  // @AIService proxy

    @Autowired private AIClient defaultClient;                 // default client
    @Autowired @Qualifier("gpt") private AIClient gptClient;   // named client
    @Autowired private AIClientRegistry registry;              // all clients
}
```

### Key Spring Boot Properties

| Property | Description |
|---|---|
| `protify.ai.enabled` | Enable/disable auto-config (default: `true`) |
| `protify.ai.service-scan-package` | Base package for `@AIService` scanning (default: auto-detected) |
| `protify.ai.defaults.*` | Default client config (`provider`, `model`, `api-key`, `temperature`, etc.) |
| `protify.ai.clients.<name>.*` | Named client config (same properties as defaults) |
| `protify.ai.defaults.retry.*` | Retry policy (`max-retries`, `backoff-strategy`, `delay-millis`, etc.) |

### Spring Boot Mistakes to Avoid

1. **Forgetting `provider` and `model`** -- both are required under `protify.ai.defaults` (or each named client).
2. **Wrong `client` name in `@AIService(client = "...")`** -- must exactly match a key in `protify.ai.clients`.
3. **Using `ProtifyAI.create()` manually** -- not needed with the starter; `@AIService` interfaces are auto-registered.
4. **Calling `ProtifyAI.initialize()` manually** -- the starter does this automatically.

---

## Available Models

The SDK includes convenience constants for popular models. For any model not listed, use `explicitModelVersion`:

```java
AIClient client = AIClient.builder()
        .provider(ProtifyAIProvider.OPEN_AI)
        .explicitModelVersion("gpt-5.4-nano")
        .build();
```

### Anthropic
`AIModel.CLAUDE_OPUS_4_6`, `CLAUDE_SONNET_4_6`, `CLAUDE_HAIKU_4_5`

### OpenAI
`AIModel.GPT_5_4`, `GPT_5_4_MINI`, `O3`, `O4_MINI`

### Google Gemini
`AIModel.GEMINI_3_1_PRO_PREVIEW`, `GEMINI_2_5_PRO`, `GEMINI_2_5_FLASH`

### Mistral
`AIModel.MISTRAL_LARGE`, `MISTRAL_SMALL`, `CODESTRAL`

### Groq
`AIModel.LLAMA_4_SCOUT`, `LLAMA_3_3_70B`

### DeepSeek
`AIModel.DEEPSEEK_CHAT`, `DEEPSEEK_REASONER`

### Fireworks
`AIModel.LLAMA_3_3_70B_FIREWORKS`

### xAI
`AIModel.GROK_4_20`, `GROK_4`

### Azure OpenAI
`AIModel.GPT_5_4_AZURE`, `O4_MINI_AZURE`

### Azure AI Foundry
`AIModel.GPT_5_4_FOUNDRY`, `CLAUDE_SONNET_4_6_FOUNDRY`, `LLAMA_4_SCOUT_FOUNDRY`

### Google Vertex AI
`AIModel.GEMINI_2_5_PRO_VERTEX`, `GEMINI_2_5_FLASH_VERTEX`

### AWS Bedrock
`AIModel.CLAUDE_SONNET_4_6_BEDROCK`, `AMAZON_NOVA_PREMIER_BEDROCK`, `LLAMA_4_MAVERICK_BEDROCK`

### Custom Model
```java
AIModel.custom("my-model-name", existingModel.getProvider())
```

---

## AIResponse API

```java
String text              = response.text();              // main response text
String modelName         = response.getModelName();
long inputTokens         = response.getInputTokens();
long outputTokens        = response.getOutputTokens();
long totalTokens         = response.getTotalTokens();
long processingMs        = response.getProcessingTimeMillis();
boolean cached           = response.isCachedResponse();
String rawJson           = response.getProviderResponse();
String responseId        = response.getResponseId();
String correlationId     = response.getCorrelationId();
String pipelineId        = response.getPipelineId();
boolean hasTools         = response.hasToolCalls();
List<AIToolCall> calls   = response.getToolCalls();
String reasoning         = response.getReasoningContent(); // reasoning model output
String stopReason        = response.getStopReason();

// Structured output
MyPojo obj       = response.as(MyPojo.class);
List<MyPojo> list = response.asList(MyPojo.class);
```

---

## Cloud Provider Configuration

### Azure OpenAI

```java
AIClient client = AIClient.builder()
        .model(AIModel.GPT_5_4_AZURE)
        .apiKey("my-azure-key")           // or AZURE_OPENAI_API_KEY env var
        .resourceName("my-resource")
        .deploymentName("my-deployment")
        .apiVersion("2024-12-01-preview") // optional
        .build();
```

### Azure AI Foundry

```java
AIClient client = AIClient.builder()
        .model(AIModel.CLAUDE_SONNET_4_6_FOUNDRY)
        .apiKey("my-foundry-key")         // or AZURE_AI_FOUNDRY_API_KEY env var
        .resourceName("my-resource")
        .build();
```

### Google Vertex AI

```java
AIClient client = AIClient.builder()
        .model(AIModel.GEMINI_2_5_PRO_VERTEX)
        .apiKey("my-access-token")        // or VERTEX_AI_ACCESS_TOKEN env var
        .region("us-central1")
        .projectId("my-gcp-project")
        .build();
```

### AWS Bedrock

```java
AIClient client = AIClient.builder()
        .model(AIModel.CLAUDE_SONNET_4_6_BEDROCK)
        .region("us-east-1")
        .awsAccessKeyId("AKIA...")        // or AWS_ACCESS_KEY_ID env var
        .awsSecretAccessKey("secret")     // or AWS_SECRET_ACCESS_KEY env var
        .awsSessionToken("token")         // optional, or AWS_SESSION_TOKEN env var
        .build();
```

---

## API Key Configuration

Each provider reads from an environment variable by default:

| Provider | Env Var |
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
| AWS Bedrock | `AWS_ACCESS_KEY_ID` + `AWS_SECRET_ACCESS_KEY` |

Or set explicitly: `.apiKey("sk-...")` on the client builder.

---

## Common Mistakes to Avoid

1. **Forgetting `ProtifyAI.initialize()`** -- must be called once before any other API usage. (Not needed with the Spring Boot starter -- it does this automatically.)
2. **Forgetting `.build()` before `.execute()`** -- the builder chain is: `client.newRequest().addInput(...).build().execute()`.
3. **Using `.model()` with custom providers** -- use `.provider(myProvider).explicitModelVersion("model-name")` instead.
4. **Not calling `.toResponse()` on streams** -- `onToken()` registers a listener but doesn't block. Call `toResponse()` to wait for completion.
5. **Mixing `AIModel` providers** -- each `AIModel` constant is bound to a specific provider. Don't pass an OpenAI model to an Anthropic client.
6. **Using `ProtifyAI.create()` in Spring Boot** -- not needed; `@AIService` interfaces are auto-registered as beans.
7. **Wrong `@AIService(client = "...")` name** -- must match a key under `protify.ai.clients` in application.yml.
