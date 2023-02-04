package ru.yandex.tasklet.resources;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ru.yandex.ci.tasklet.SandboxResource;
import ru.yandex.sandbox.tasklet.sidecars.resource_manager.proto.ResourceManagerApi;
import ru.yandex.tasklet.api.v2.WellKnownStructures.UserError;
import ru.yandex.tasklet.sdk.v2.java.src.test.proto.Example;
import ru.yandex.tasklet.sdk.v2.java.src.test.proto.Example.TaskletInput;
import ru.yandex.tasklet.sdk.v2.java.src.test.proto.Example.TaskletOutput;
import ru.yandex.tasklet.test.TaskletContextStub;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleTaskletWithResourcesTest {
    @TempDir
    static Path tempDir;

    private TaskletContextStub stub;
    private SimpleTaskletWithResources tasklet;

    @BeforeEach
    void beforeEach() {
        stub = TaskletContextStub.stub(TaskletInput.class, TaskletOutput.class, tempDir);
        tasklet = new SimpleTaskletWithResources();
    }

    @AfterEach
    void afterEach() {
        stub.close();
    }

    @Test
    void testResourceNotFound() {
        var input = TaskletInput.newBuilder()
                .setConfig(Example.Configuration.newBuilder().setId("some-type"))
                .build();
        var response = tasklet.execute(input, stub.getContext());
        assertEquals(
                UserError.newBuilder()
                        .setDescription("Resource type not found: some-type")
                        .build(),
                response.getError()
        );
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testResourceDifferentType() {
        stub.getContext().getSandboxResourceManager()
                .createResource(
                        ResourceManagerApi.CreateResourceRequest.newBuilder()
                                .setDescription("new resource type")
                                .setType("some-type2")
                                .setPath("//dev/null")
                                .build())
                .getResource();

        var input = TaskletInput.newBuilder()
                .setConfig(Example.Configuration.newBuilder().setId("some-type"))
                .build();
        var response = tasklet.execute(input, stub.getContext());
        assertEquals(
                UserError.newBuilder()
                        .setDescription("Resource type not found: some-type")
                        .build(),
                response.getError()
        );
    }

    @Test
    void testResourceFound() {
        var res1 = stub.getContext().getSandboxResourceManager()
                .createResource(
                        ResourceManagerApi.CreateResourceRequest.newBuilder()
                                .setDescription("new resource type")
                                .setType("some-type")
                                .setPath("//dev/null")
                                .putAttributes("a1", "v1")
                                .putAttributes("a2", "v2")
                                .build())
                .getResource();

        var input = TaskletInput.newBuilder()
                .setConfig(Example.Configuration.newBuilder().setId("some-type"))
                .build();
        var response = tasklet.execute(input, stub.getContext());
        assertEquals(
                TaskletOutput.newBuilder()
                        .setReport(Example.Report.newBuilder()
                                .setDescription("some-type")
                                .setUrl("http://sandbox/" + res1.getId())
                                .build())
                        .addResources(SandboxResource.newBuilder()
                                .setId(res1.getId())
                                .setType("some-type")
                                .setTaskId(0)
                                .putAttributes("a1", "v1")
                                .putAttributes("a2", "v2"))
                        .build(),
                response.getResult()
        );
    }

    @Test
    void testMultipleResourceFound() {
        var res1 = stub.getContext().getSandboxResourceManager()
                .createResource(
                        ResourceManagerApi.CreateResourceRequest.newBuilder()
                                .setDescription("new resource type 1")
                                .setType("some-type")
                                .setPath("//dev/null")
                                .build())
                .getResource();

        var res2 = stub.getContext().getSandboxResourceManager()
                .createResource(
                        ResourceManagerApi.CreateResourceRequest.newBuilder()
                                .setDescription("new resource type 2")
                                .setType("some-type")
                                .setPath("//dev/null")
                                .build())
                .getResource();

        var input = TaskletInput.newBuilder()
                .setConfig(Example.Configuration.newBuilder().setId("some-type"))
                .build();
        var response = tasklet.execute(input, stub.getContext());
        assertEquals(
                TaskletOutput.newBuilder()
                        .setReport(Example.Report.newBuilder()
                                .setDescription("some-type")
                                .setUrl("http://sandbox/" + res1.getId())
                                .build())
                        .addResources(SandboxResource.newBuilder()
                                .setId(res1.getId())
                                .setType("some-type")
                                .setTaskId(0))
                        .addResources(SandboxResource.newBuilder()
                                .setId(res2.getId())
                                .setType("some-type")
                                .setTaskId(0))
                        .build(),
                response.getResult()
        );
    }

}
