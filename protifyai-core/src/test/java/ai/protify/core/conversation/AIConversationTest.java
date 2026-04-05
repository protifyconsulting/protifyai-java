package ai.protify.core.conversation;

import ai.protify.core.AIClient;
import ai.protify.core.AIModel;
import ai.protify.core.message.AIMessage;
import ai.protify.core.provider.mock.MockProvider;
import ai.protify.core.response.AIResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AIConversationTest {

    private static AIClient clientFor(MockProvider mock) {
        return AIClient.builder()
                .model(AIModel.custom("mock-model", mock))
                .apiKey("mock-key")
                .instructions("You are a helpful assistant.")
                .build();
    }

    // ---------------------------------------------------------------
    // 1. Basic conversation
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Basic conversation")
    class BasicConversation {

        @Test
        @DisplayName("Single turn returns response")
        void singleTurn() {
            MockProvider mock = MockProvider.withResponse("Hello there!");
            AIClient client = clientFor(mock);

            AIConversation conversation = client.newConversation().build();
            AIResponse response = conversation.send("Hi");

            assertEquals("Hello there!", response.text());
        }

        @Test
        @DisplayName("Multi-turn accumulates messages")
        void multiTurn() {
            MockProvider mock = MockProvider.withResponse("mock response");
            AIClient client = clientFor(mock);

            AIConversation conversation = client.newConversation().build();
            conversation.send("First message");
            conversation.send("Second message");
            conversation.send("Third message");

            List<AIMessage> messages = conversation.getMessages();
            // 3 user messages + 3 assistant messages = 6
            assertEquals(6, messages.size());
        }

        @Test
        @DisplayName("Messages alternate user and assistant")
        void messageRoles() {
            MockProvider mock = MockProvider.withResponse("response");
            AIClient client = clientFor(mock);

            AIConversation conversation = client.newConversation().build();
            conversation.send("hello");

            List<AIMessage> messages = conversation.getMessages();
            assertEquals(2, messages.size());
            assertEquals("user", messages.get(0).getRole());
            assertEquals("assistant", messages.get(1).getRole());
        }

        @Test
        @DisplayName("User message text is preserved")
        void userTextPreserved() {
            MockProvider mock = MockProvider.withResponse("response");
            AIClient client = clientFor(mock);

            AIConversation conversation = client.newConversation().build();
            conversation.send("What is Java?");

            List<AIMessage> messages = conversation.getMessages();
            assertEquals("What is Java?", messages.get(0).getText());
        }
    }

    // ---------------------------------------------------------------
    // 2. Conversation ID
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Conversation ID")
    class ConversationId {

        @Test
        @DisplayName("Auto-generates ID when not specified")
        void autoGeneratesId() {
            MockProvider mock = MockProvider.withResponse("ok");
            AIClient client = clientFor(mock);

            AIConversation conversation = client.newConversation().build();

            assertNotNull(conversation.getId());
            assertFalse(conversation.getId().isEmpty());
        }

        @Test
        @DisplayName("Uses provided ID")
        void usesProvidedId() {
            MockProvider mock = MockProvider.withResponse("ok");
            AIClient client = clientFor(mock);

            AIConversation conversation = client.newConversation()
                    .id("my-conversation-123")
                    .build();

            assertEquals("my-conversation-123", conversation.getId());
        }
    }

    // ---------------------------------------------------------------
    // 3. Clear
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Clear")
    class Clear {

        @Test
        @DisplayName("Clear removes all messages")
        void clearRemovesMessages() {
            MockProvider mock = MockProvider.withResponse("response");
            AIClient client = clientFor(mock);

            AIConversation conversation = client.newConversation().build();
            conversation.send("hello");
            conversation.send("world");

            assertEquals(4, conversation.getMessages().size());

            conversation.clear();

            assertEquals(0, conversation.getMessages().size());
        }

        @Test
        @DisplayName("Can send messages after clear")
        void canSendAfterClear() {
            MockProvider mock = MockProvider.withResponse("response");
            AIClient client = clientFor(mock);

            AIConversation conversation = client.newConversation().build();
            conversation.send("before clear");
            conversation.clear();
            conversation.send("after clear");

            assertEquals(2, conversation.getMessages().size());
        }
    }

    // ---------------------------------------------------------------
    // 4. Conversation state
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Conversation state")
    class State {

        @Test
        @DisplayName("getState returns current state")
        void getStateReturnsCurrent() {
            MockProvider mock = MockProvider.withResponse("response");
            AIClient client = clientFor(mock);

            AIConversation conversation = client.newConversation()
                    .id("test-id")
                    .build();
            conversation.send("hello");

            AIConversationState state = conversation.getState();

            assertEquals("test-id", state.getId());
            assertEquals(2, state.getMessages().size());
        }

        @Test
        @DisplayName("State serializes to JSON and back")
        void stateSerializesRoundTrip() {
            MockProvider mock = MockProvider.withResponse("I'm an AI");
            AIClient client = clientFor(mock);

            AIConversation conversation = client.newConversation()
                    .id("roundtrip-test")
                    .build();
            conversation.send("Who are you?");

            AIConversationState original = conversation.getState();
            String json = original.toJson();

            assertNotNull(json);
            assertFalse(json.isEmpty());

            AIConversationState restored = AIConversationState.fromJson(json);

            assertEquals("roundtrip-test", restored.getId());
            assertEquals(original.getMessages().size(), restored.getMessages().size());
        }
    }

    // ---------------------------------------------------------------
    // 5. Conversation store (persistence)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Conversation store")
    class StoreTest {

        @Test
        @DisplayName("Store receives save after each send")
        void storeSavesAfterSend() {
            MockProvider mock = MockProvider.withResponse("response");
            AIClient client = clientFor(mock);

            InMemoryStore store = new InMemoryStore();

            AIConversation conversation = client.newConversation()
                    .id("persist-test")
                    .store(store)
                    .build();

            conversation.send("first");
            assertEquals(1, store.saveCount);
            assertEquals(2, store.lastSaved.getMessages().size());

            conversation.send("second");
            assertEquals(2, store.saveCount);
            assertEquals(4, store.lastSaved.getMessages().size());
        }

        @Test
        @DisplayName("Store loads existing conversation on build")
        void storeLoadsOnBuild() {
            MockProvider mock = MockProvider.withResponse("response");
            AIClient client = clientFor(mock);

            // First conversation — populate the store
            InMemoryStore store = new InMemoryStore();
            AIConversation first = client.newConversation()
                    .id("reload-test")
                    .store(store)
                    .build();
            first.send("hello");

            // Second conversation — same ID, same store
            AIConversation second = client.newConversation()
                    .id("reload-test")
                    .store(store)
                    .build();

            // Should have loaded the 2 messages from the first conversation
            assertEquals(2, second.getMessages().size());
        }

        @Test
        @DisplayName("Store delete removes conversation")
        void storeDelete() {
            InMemoryStore store = new InMemoryStore();
            AIConversationState state = new AIConversationState("to-delete", List.of());
            store.save(state);

            assertNotNull(store.load("to-delete"));
            store.delete("to-delete");
            assertNull(store.load("to-delete"));
        }

        @Test
        @DisplayName("No store configured — no errors")
        void noStoreConfigured() {
            MockProvider mock = MockProvider.withResponse("response");
            AIClient client = clientFor(mock);

            AIConversation conversation = client.newConversation().build();
            AIResponse response = conversation.send("hello");

            assertEquals("response", response.text());
        }
    }

    // ---------------------------------------------------------------
    // 6. Configuration
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Builder configuration")
    class BuilderConfig {

        @Test
        @DisplayName("Instructions, temperature, and maxOutputTokens are accepted")
        void configMethodsChain() {
            MockProvider mock = MockProvider.withResponse("ok");
            AIClient client = clientFor(mock);

            AIConversation conversation = client.newConversation()
                    .instructions("Be concise")
                    .temperature(0.5)
                    .maxOutputTokens(100)
                    .topP(0.9)
                    .topK(40)
                    .build();

            AIResponse response = conversation.send("test");
            assertEquals("ok", response.text());
        }
    }

    // ---------------------------------------------------------------
    // 7. AIConversationState
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("AIConversationState")
    class ConversationStateTest {

        @Test
        @DisplayName("Empty state")
        void emptyState() {
            AIConversationState state = new AIConversationState();
            assertNull(state.getId());
            assertTrue(state.getMessages().isEmpty());
        }

        @Test
        @DisplayName("State with null messages defaults to empty list")
        void nullMessagesDefaultsToEmpty() {
            AIConversationState state = new AIConversationState("id", null);
            assertNotNull(state.getMessages());
            assertTrue(state.getMessages().isEmpty());
        }

        @Test
        @DisplayName("Messages list is unmodifiable")
        void messagesUnmodifiable() {
            AIConversationState state = new AIConversationState("id", List.of());
            assertThrows(UnsupportedOperationException.class, () ->
                    state.getMessages().add(null)
            );
        }
    }

    // ---------------------------------------------------------------
    // Simple in-memory store for testing
    // ---------------------------------------------------------------

    static class InMemoryStore implements AIConversationStore {
        final Map<String, AIConversationState> data = new HashMap<>();
        int saveCount = 0;
        AIConversationState lastSaved;

        @Override
        public void save(AIConversationState state) {
            data.put(state.getId(), state);
            lastSaved = state;
            saveCount++;
        }

        @Override
        public AIConversationState load(String conversationId) {
            return data.get(conversationId);
        }

        @Override
        public void delete(String conversationId) {
            data.remove(conversationId);
        }
    }
}
