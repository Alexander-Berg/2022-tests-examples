package ru.yandex.partner.core.entity.queue;

import java.io.IOException;
import java.time.Duration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import ru.yandex.partner.core.entity.queue.exceptions.TaskExecutionException;
import ru.yandex.partner.core.queue.AbstractTask;
import ru.yandex.partner.core.queue.TaskData;
import ru.yandex.partner.core.queue.TaskPayload;

public class TestNonConcurrentTask extends AbstractTask<TestNonConcurrentTask.Payload, String> {

    public TestNonConcurrentTask(Payload payload, TaskData savedTaskData) {
        super(payload, savedTaskData);
    }

    @Override
    public String execute() {
        System.out.println("Executing non-concurrent task for someId = " + getPayload().someId);

        if (getPayload().someId == 1 && getSavedTaskData().getTries() < 2) {
            throw new TaskExecutionException("Failed one time for someId == 1") {
                @Override
                public Object getErrorData() {
                    return "Error data for someId = 1";
                }
            };
        } else if (getPayload().someId == 2) {
            throw new RuntimeException("Always fail for someId == 2");
        }

        return "Result of non-concurrent task execution";
    }

    @Override
    public TaskData getTaskData() {
        return getSavedTaskData();
    }

    @Override
    public Duration getEstimatedTime() {
        return Duration.ofSeconds(3);
    }

    @JsonDeserialize(using = Payload.Deserializer.class)
    public static class Payload implements TaskPayload {

        @JsonProperty
        private final Long someId;

        private final String previousError = null;

        public Payload(Long someId) {
            this.someId = someId;
        }

        public static Payload of(Long someId) {
            return new Payload(someId);
        }

        @Override
        public String serializeParams() {
            try {
                return new ObjectMapper().writeValueAsString(this);
            } catch (IOException exception) {
                throw new RuntimeException("Failed to serialize TestNonConcurrentTask.Payload", exception);
            }
        }

        @Override
        public int getTypeId() {
            return TestTaskType.TEST_NON_CONCURRENT_TASK.getTypeId();
        }

        @Override
        public Long getGroupId() {
            return someId;
        }

        public static class Deserializer extends StdDeserializer<TestNonConcurrentTask.Payload> {

            public Deserializer() {
                this(null);
            }

            public Deserializer(Class<?> vc) {
                super(vc);
            }

            @Override
            public TestNonConcurrentTask.Payload deserialize(JsonParser jp, DeserializationContext ctxt)
                    throws IOException {
                JsonNode node = jp.getCodec().readTree(jp);
                JsonNode someIdNode = node.get("someId");
                if (someIdNode != null && someIdNode.isNumber()) {
                    return new TestNonConcurrentTask.Payload(node.get("someId").longValue());
                } else {
                    throw new JsonParseException(jp, "Expected numeric someId");
                }
            }
        }

    }
}
