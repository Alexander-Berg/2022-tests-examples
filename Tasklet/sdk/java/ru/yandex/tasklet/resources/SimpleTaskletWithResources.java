package ru.yandex.tasklet.resources;

import ru.yandex.tasklet.Result;
import ru.yandex.tasklet.TaskletAction;
import ru.yandex.tasklet.TaskletContext;
import ru.yandex.tasklet.TaskletRuntime;
import ru.yandex.tasklet.api.v2.WellKnownStructures.UserError;
import ru.yandex.tasklet.sdk.v2.java.src.test.proto.Example;
import ru.yandex.tasklet.sdk.v2.java.src.test.proto.Example.TaskletInput;
import ru.yandex.tasklet.sdk.v2.java.src.test.proto.Example.TaskletOutput;

public class SimpleTaskletWithResources implements TaskletAction<TaskletInput, TaskletOutput> {

    @Override
    public Result<TaskletOutput> execute(TaskletInput taskletInput, TaskletContext context) {

        var type = taskletInput.getConfig().getId();
        var response = context.getSandboxResourcesContext().getResources(req -> req.setType(type));
        if (response.isEmpty()) {
            return Result.ofError(UserError.newBuilder()
                    .setDescription("Resource type not found: " + type)
                    .build());
        } else {
            var res = response.get(0);
            return Result.of(TaskletOutput.newBuilder()
                    .setReport(Example.Report.newBuilder()
                            .setDescription(res.getType())
                            .setUrl("http://sandbox/" + res.getId())
                            .build())
                            .addAllResources(response)
                    .build());
        }
    }

    public static void main(String[] args) {
        TaskletRuntime.execute(
                TaskletInput.class,
                TaskletOutput.class,
                args,
                new SimpleTaskletWithResources()
        );
    }
}
