package ru.yandex.tasklet;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.tasklet.api.v2.ExecutorServiceOuterClass.SecretValue;
import ru.yandex.tasklet.api.v2.WellKnownStructures.SecretRef;
import ru.yandex.tasklet.api.v2.WellKnownStructures.UserError;
import ru.yandex.tasklet.sdk.v2.java.src.test.proto.Example.Configuration;
import ru.yandex.tasklet.sdk.v2.java.src.test.proto.Example.Report;
import ru.yandex.tasklet.sdk.v2.java.src.test.proto.Example.TaskletInput;
import ru.yandex.tasklet.sdk.v2.java.src.test.proto.Example.TaskletOutput;
import ru.yandex.tasklet.test.TaskletContextStub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SimpleTaskletTest {

    private SecretRef ciSecretRef;
    private SimpleTasklet tasklet;
    private TaskletContextStub stub;

    @BeforeEach
    void beforeEach() {
        ciSecretRef = defaultSecretRef();
        tasklet = new SimpleTasklet();

        stub = TaskletContextStub.stub(TaskletInput.class, TaskletOutput.class);
        stub.getExecutorServiceStub().getModifiableSecretsMap()
                .put(ciSecretRef, defaultSecretValue());
    }

    @AfterEach
    void afterEach() {
        stub.close();
    }

    @Test
    void testValid() {
        testValid("ya.make");
    }

    @Test
    void testValid2() {
        testValid("ci");
    }

    @Test
    void testInvalid() {
        var someSecret = SecretRef.newBuilder()
                .setId("unknown-id")
                .build();

        var input = TaskletInput.newBuilder()
                .setConfig(Configuration.newBuilder().setId("ya.make"))
                .setCiToken(someSecret)
                .build();

        var exception = assertThrows(StatusRuntimeException.class,
                () -> tasklet.execute(input, stub.getContext()));
        assertEquals("NOT_FOUND: Secret is not found: " + someSecret, exception.getMessage());
    }

    @Test
    void testError() {
        var input = TaskletInput.newBuilder()
                .setConfig(Configuration.newBuilder().setId("error"))
                .setCiToken(ciSecretRef)
                .build();

        var result = tasklet.execute(input, stub.getContext());
        assertFalse(result.hasResult());
        assertTrue(result.hasError());
        assertEquals(
                UserError.newBuilder()
                        .setIsTransient(true)
                        .setDescription("Something bad happens")
                        .setDetails(Struct.newBuilder()
                                .putFields("error", Value.newBuilder().setStringValue("major").build())
                                .build())
                        .build(),
                result.getError()
        );
    }

    @Test
    void testRuntime() {
        var input = TaskletInput.newBuilder()
                .setConfig(Configuration.newBuilder().setId("runtime"))
                .setCiToken(ciSecretRef)
                .build();

        var exception = assertThrows(RuntimeException.class,
                () -> tasklet.execute(input, stub.getContext()));
        assertEquals("Internal error", exception.getMessage());
    }

    private void testValid(String id) {
        var input = TaskletInput.newBuilder()
                .setConfig(Configuration.newBuilder().setId(id))
                .setCiToken(ciSecretRef)
                .build();

        var result = tasklet.execute(input, stub.getContext());

        var expectOutput = TaskletOutput.newBuilder()
                .setReport(Report.newBuilder()
                        .setUrl("https://a.yandex-team.ru/arcadia/" + id)
                        .setDescription("Link for Arcadia"))
                .build();
        assertTrue(result.hasResult());
        assertFalse(result.hasError());
        assertEquals(expectOutput, result.getResult());
    }

    public static SecretRef defaultSecretRef() {
        return SecretRef.newBuilder()
                .setId("id")
                .setKey("key")
                .build();
    }

    public static SecretValue defaultSecretValue() {
        return SecretValue.newBuilder()
                .setValue("value")
                .build();
    }
}
