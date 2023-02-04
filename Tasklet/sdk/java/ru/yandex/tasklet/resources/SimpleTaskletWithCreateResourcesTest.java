package ru.yandex.tasklet.resources;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ru.yandex.ci.tasklet.SandboxResource;
import ru.yandex.tasklet.sdk.v2.java.src.test.proto.Example;
import ru.yandex.tasklet.sdk.v2.java.src.test.proto.Example.TaskletInput;
import ru.yandex.tasklet.sdk.v2.java.src.test.proto.Example.TaskletOutput;
import ru.yandex.tasklet.test.TaskletContextStub;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleTaskletWithCreateResourcesTest {
    @TempDir
    static Path tempDir;

    private TaskletContextStub stub;
    private SimpleTaskletWithCreateResources tasklet;

    @BeforeEach
    void beforeEach() {
        stub = TaskletContextStub.stub(TaskletInput.class, TaskletOutput.class, tempDir);
        tasklet = new SimpleTaskletWithCreateResources();
    }

    @AfterEach
    void afterEach() {
        stub.close();
    }

    @Test
    void testResource() throws IOException {
        var input = TaskletInput.newBuilder()
                .setConfig(Example.Configuration.newBuilder().setId("some-type"))
                .build();
        var response = tasklet.execute(input, stub.getContext());
        assertEquals(
                TaskletOutput.newBuilder()
                        .addResources(SandboxResource.newBuilder()
                                .setId(1)
                                .setType("some-type")
                                .setTaskId(0)
                                .putAttributes("key", "value"))
                        .build(),
                response.getResult()
        );

        var path = stub.getContext().getSandboxResourcesContext().downloadResource(1);
        assertEquals("Example for some-type", Files.readString(path));

    }

}
