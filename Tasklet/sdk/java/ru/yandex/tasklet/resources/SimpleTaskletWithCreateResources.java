package ru.yandex.tasklet.resources;

import java.nio.file.Files;

import ru.yandex.tasklet.Result;
import ru.yandex.tasklet.TaskletAction;
import ru.yandex.tasklet.TaskletContext;
import ru.yandex.tasklet.TaskletRuntime;
import ru.yandex.tasklet.sdk.v2.java.src.test.proto.Example.TaskletInput;
import ru.yandex.tasklet.sdk.v2.java.src.test.proto.Example.TaskletOutput;

public class SimpleTaskletWithCreateResources implements TaskletAction<TaskletInput, TaskletOutput> {

    @Override
    public Result<TaskletOutput> execute(TaskletInput taskletInput, TaskletContext context) {

        var type = taskletInput.getConfig().getId();

        var resource = context.getSandboxResourcesContext().createResource(
                "example.txt",
                meta -> meta.setType(type)
                        .setDescription("Test example")
                        .putAttributes("key", "value"),
                path -> Files.writeString(path, "Example for " + type)
        );

        var output = TaskletOutput.newBuilder()
                .addResources(resource)
                .build();

        return Result.of(output);
    }

    public static void main(String[] args) {
        TaskletRuntime.execute(
                TaskletInput.class,
                TaskletOutput.class,
                args,
                new SimpleTaskletWithCreateResources()
        );
    }
}
