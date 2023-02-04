package ru.yandex.tasklet.test;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ru.yandex.tasklet.SimpleTasklet;
import ru.yandex.tasklet.SimpleTaskletTest;
import ru.yandex.tasklet.api.v2.WellKnownStructures;
import ru.yandex.tasklet.api.v2.WellKnownStructures.SecretRef;
import ru.yandex.tasklet.sdk.v2.java.src.test.proto.Example;
import ru.yandex.tasklet.sdk.v2.java.src.test.proto.Example.TaskletInput;
import ru.yandex.tasklet.sdk.v2.java.src.test.proto.Example.TaskletOutput;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Internal example, you should not test your tasklets
public class SimpleTaskletMainTest {

    private static TaskletRuntimeStub<TaskletInput, TaskletOutput> runtimeStub;
    private static SecretRef ciSecretRef;

    @BeforeAll
    static void initAll() {
        runtimeStub = TaskletRuntimeStub.networkBasedStub(TaskletInput.class, TaskletOutput.class);

        ciSecretRef = SimpleTaskletTest.defaultSecretRef();
        runtimeStub.getTaskletContextStub().getExecutorServiceStub().getModifiableSecretsMap()
                .put(ciSecretRef, SimpleTaskletTest.defaultSecretValue());
    }

    @AfterAll
    static void closeAll() {
        runtimeStub.close();
    }

    @Test
    void testOutput() {
        var input = TaskletInput.newBuilder()
                .setConfig(Example.Configuration.newBuilder().setId("ya.make"))
                .setCiToken(ciSecretRef)
                .build();

        var result = runtimeStub.execute(input, SimpleTasklet::main);

        var expectOutput = TaskletOutput.newBuilder()
                .setReport(Example.Report.newBuilder()
                        .setUrl("https://a.yandex-team.ru/arcadia/ya.make")
                        .setDescription("Link for Arcadia"))
                .build();
        assertTrue(result.hasResult());
        assertFalse(result.hasError());
        assertEquals(expectOutput, result.getResult());
    }

    @Test
    void testError() {
        var input = TaskletInput.newBuilder()
                .setConfig(Example.Configuration.newBuilder().setId("error"))
                .setCiToken(ciSecretRef)
                .build();

        var result = runtimeStub.execute(input, SimpleTasklet::main);

        assertFalse(result.hasResult());
        assertTrue(result.hasError());
        assertEquals(
                WellKnownStructures.UserError.newBuilder()
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
                .setConfig(Example.Configuration.newBuilder().setId("runtime"))
                .setCiToken(ciSecretRef)
                .build();

        var exception = assertThrows(RuntimeException.class,
                () -> runtimeStub.execute(input, SimpleTasklet::main));
        assertEquals("Internal error", exception.getMessage());
    }
}
