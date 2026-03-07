# Custom Provider Guide

This guide walks through adding your own AI provider to Protify AI. You need two classes (a request and a client) and a one-line provider registration.

---

## Overview

A custom provider consists of three parts:

1. **Provider registration** -- tells Protify AI how to authenticate and which client class to use
2. **Request class** -- transforms the SDK's generic `AIRequest` into provider-specific JSON
3. **Client class** -- sends the request to the provider's API and parses the response

---

## Step 1: Register the Provider

Use `AIProvider.custom()` to define your provider:

```java
AIProvider myProvider = AIProvider.custom("my-provider")
        .apiKeyVarName("MY_PROVIDER_API_KEY")
        .clientType(MyProviderClient.class)
        .build();
```

That's the minimum. The SDK will use a default `Authorization: Bearer {key}` header. You can customize:

```java
AIProvider myProvider = AIProvider.custom("my-provider")
        .apiKeyVarName("MY_PROVIDER_API_KEY")
        .clientType(MyProviderClient.class)
        .headers(credential -> Map.of(
                "x-api-key", credential,
                "Content-Type", "application/json"
        ))
        .supportedMimeTypes(Set.of(MimeType.PNG, MimeType.JPG, MimeType.PDF))
        // or .allMimeTypesSupported()
        .build();
```

### Builder Methods

| Method | Required | Description |
|---|---|---|
| `apiKeyVarName(String)` | Yes | Environment variable name for the API key |
| `clientType(Class)` | Yes | Your client class (must extend `ProtifyAIProviderClient`) |
| `headers(Function<String, Map<String, String>>)` | No | Custom header function. Default: Bearer token + JSON content type |
| `supportedMimeTypes(Set<MimeType>)` | No | Which file types this provider accepts |
| `allMimeTypesSupported()` | No | Accept all MIME types |

---

## Step 2: Create the Request Class

Your request class transforms the SDK's internal representation into the JSON your provider expects. Extend `ProtifyAIProviderRequest`:

```java
package com.example.ai;

import ai.protify.core.internal.config.AIConfigProperty;
import ai.protify.core.internal.config.Configuration;
import ai.protify.core.internal.util.json.ProtifyJson;
import ai.protify.core.provider.ProtifyAIProviderRequest;
import ai.protify.core.request.AIInput;
import ai.protify.core.request.AIRequest;
import ai.protify.core.request.AITextInput;

import java.util.*;

public class MyProviderRequest extends ProtifyAIProviderRequest {

    private Map<String, Object> body;

    @Override
    public void initialize(AIRequest request, Configuration configuration) {
        super.initialize(request, configuration);

        // Build the request body your provider expects
        body = new LinkedHashMap<>();
        body.put("model", getModelName());

        // Build messages array
        List<Map<String, Object>> messages = new ArrayList<>();

        // System prompt
        String instructions = configuration.getProperty(AIConfigProperty.INSTRUCTIONS);
        if (instructions != null) {
            messages.add(Map.of("role", "system", "content", instructions));
        }

        // User message from inputs
        StringBuilder userContent = new StringBuilder();
        for (AIInput input : getInputs()) {
            if (input instanceof AITextInput) {
                userContent.append(((AITextInput) input).getText());
            }
        }
        messages.add(Map.of("role", "user", "content", userContent.toString()));

        body.put("messages", messages);

        // Optional parameters
        Double temperature = configuration.getProperty(AIConfigProperty.TEMPERATURE);
        if (temperature != null) {
            body.put("temperature", temperature);
        }

        Integer maxTokens = configuration.getProperty(AIConfigProperty.MAX_OUTPUT_TOKENS);
        if (maxTokens != null) {
            body.put("max_tokens", maxTokens);
        }
    }

    @Override
    public String toJson() {
        return ProtifyJson.toJson(body);
    }

    @Override
    public String toLoggableJson() {
        return toJson(); // same unless you want to redact sensitive fields
    }
}
```

### Key Points

- `super.initialize(request, configuration)` sets up `getModelName()`, `getInputs()`, `getConfiguration()`, `getTools()`, `getToolResults()`, `getPreviousAssistantResponse()`, and `getMessages()`.
- `ProtifyJson.toJson(object)` is the SDK's built-in JSON serializer -- it handles Maps, Lists, primitives, and annotated POJOs. Zero external dependencies.
- `configuration.getProperty(AIConfigProperty.X)` reads a typed config value. Returns `null` if not set.

### Declaring Unsupported Parameters

If your provider doesn't support certain parameters (e.g., `topK`), override these methods to suppress them with a logged warning instead of sending invalid requests:

```java
@Override
protected Set<AIConfigProperty> getUnsupportedParameters() {
    return Set.of(AIConfigProperty.TOP_K);
}

// For model-specific restrictions
@Override
protected Set<AIConfigProperty> getUnsupportedParametersForModel(String model) {
    if (model.contains("mini")) {
        return Set.of(AIConfigProperty.TOP_P);
    }
    return Collections.emptySet();
}
```

---

## Step 3: Create the Client Class

The client sends the request and parses the response. Extend `ProtifyAIProviderClient`:

```java
package com.example.ai;

import ai.protify.core.internal.config.CredentialHelper;
import ai.protify.core.internal.http.ProtifyHttpClient;
import ai.protify.core.internal.http.ProtifyHttpResponse;
import ai.protify.core.internal.pipeline.PipelineAIResponse;
import ai.protify.core.internal.response.ProtifyAIStreamResponse;
import ai.protify.core.internal.util.json.ProtifyJson;
import ai.protify.core.provider.ProtifyAIProviderClient;
import ai.protify.core.response.AIResponse;
import ai.protify.core.response.AIStreamResponse;

public class MyProviderClient extends ProtifyAIProviderClient<MyProviderRequest> {

    private static final String ENDPOINT = "https://api.myprovider.com/v1/chat/completions";

    @Override
    public AIResponse execute(MyProviderRequest request) {
        ProtifyHttpResponse response = ProtifyHttpClient.getInstance()
                .post(request, ENDPOINT);

        String rawJson = response.getResponseBody();

        // Parse the response -- adapt to your provider's response format
        // Option A: Use ProtifyJson to deserialize into a response POJO
        // MyResponseBody body = ProtifyJson.fromJson(rawJson, MyResponseBody.class);

        // Option B: Use the lightweight JSON string extractor for simple responses
        String text = CredentialHelper.extractJsonString(rawJson, "content");
        return PipelineAIResponse.of(text);
    }

    // Optional: implement streaming
    @Override
    public AIStreamResponse executeStream(MyProviderRequest request) {
        ProtifyAIStreamResponse streamResponse = new ProtifyAIStreamResponse();

        ProtifyHttpClient.getInstance().postStream(request, ENDPOINT,
                chunk -> {
                    // Parse each SSE chunk for the text token
                    String token = CredentialHelper.extractJsonString(chunk, "content");
                    if (token != null) {
                        streamResponse.pushToken(token);
                    }
                },
                () -> streamResponse.completeWithAccumulatedText()
        ).exceptionally(ex -> {
            streamResponse.completeExceptionally(ex);
            return null;
        });

        return streamResponse;
    }
}
```

### Key Points

- `ProtifyHttpClient.getInstance().post(request, url)` handles authentication headers, timeouts, retries, and response caching automatically. It reads headers from your provider registration.
- `ProtifyHttpClient.getInstance().postStream(request, url, onChunk, onComplete)` handles SSE streaming. The `onChunk` callback receives each `data:` line. Returns a `CompletableFuture<Void>`.
- `PipelineAIResponse.of(text)` creates a simple response with just text content. For richer responses (token counts, model name, etc.), create your own `AIResponse` implementation.
- `CredentialHelper.extractJsonString(json, key)` is a lightweight JSON value extractor -- useful for streaming chunks where you only need one field. It properly handles escaped quotes.

---

## Step 4: Use Your Provider

```java
ProtifyAI.initialize();

AIProvider myProvider = AIProvider.custom("my-provider")
        .apiKeyVarName("MY_PROVIDER_API_KEY")
        .clientType(MyProviderClient.class)
        .build();

AIClient client = AIClient.builder()
        .provider(myProvider)
        .explicitModelVersion("my-model-v2")
        .temperature(0.7)
        .build();

AIResponse response = client.newRequest()
        .addInput("Hello, world!")
        .build()
        .execute();

System.out.println(response.text());
```

Note: when using a custom provider, use `.provider()` + `.explicitModelVersion()` instead of `.model()`. The `AIModel` constants are only for built-in providers.

---

## Real-World Example: How DeepSeek is Implemented

DeepSeek follows the OpenAI-compatible chat completions format, so its implementation is minimal -- it reuses the shared `ChatCompletionsClient` base class:

**DeepSeekRequest.java:**
```java
public final class DeepSeekRequest extends ChatCompletionsRequest {
    // Inherits everything from ChatCompletionsRequest
}
```

**DeepSeekClient.java:**
```java
public class DeepSeekClient extends ChatCompletionsClient<DeepSeekRequest> {
    private static final String ENDPOINT_URL = "https://api.deepseek.com/chat/completions";

    @Override
    protected String getEndpointUrl() {
        return ENDPOINT_URL;
    }
}
```

If your provider uses the OpenAI-compatible chat completions format, you can do the same -- extend `ChatCompletionsClient` and `ChatCompletionsRequest` and just specify your endpoint URL. The base classes handle message formatting, tool calls, streaming, and response parsing.

---

## Supporting Conversations and Tool Calls

If your provider supports multi-turn conversations, handle the `getMessages()` list in your request class:

```java
@Override
public void initialize(AIRequest request, Configuration configuration) {
    super.initialize(request, configuration);

    List<Map<String, Object>> messages = new ArrayList<>();

    // Conversation history (populated when used via AIConversation)
    for (AIMessage msg : getMessages()) {
        messages.add(Map.of(
            "role", msg.getRole().name().toLowerCase(),
            "content", msg.getText()
        ));
    }

    // If no conversation history, build from inputs (single request)
    if (messages.isEmpty()) {
        // ... build from getInputs() as shown above
    }

    body.put("messages", messages);
}
```

For tool calls, check `getTools()` for tool definitions and `getToolResults()` / `getPreviousAssistantResponse()` for the tool loop follow-up pattern. See the built-in provider implementations for complete examples.

---

## Summary

| What | Base class | Your job |
|---|---|---|
| Provider registration | `AIProvider.custom()` | Name, env var, client class, optional headers |
| Request | Extend `ProtifyAIProviderRequest` | Override `initialize()`, `toJson()`, `toLoggableJson()` |
| Client | Extend `ProtifyAIProviderClient<YourRequest>` | Override `execute()`, optionally `executeStream()` |

The SDK handles authentication, retries, caching, configuration merging, and the request/response lifecycle. Your provider code only needs to handle JSON serialization and deserialization specific to your provider's API format.
