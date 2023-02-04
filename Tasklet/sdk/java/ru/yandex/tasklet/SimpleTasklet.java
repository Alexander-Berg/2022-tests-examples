package ru.yandex.tasklet;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.tasklet.api.v2.WellKnownStructures.UserError;
import ru.yandex.tasklet.sdk.v2.java.src.test.proto.Example;
import ru.yandex.tasklet.sdk.v2.java.src.test.proto.Example.TaskletInput;
import ru.yandex.tasklet.sdk.v2.java.src.test.proto.Example.TaskletOutput;


public class SimpleTasklet implements TaskletAction<TaskletInput, TaskletOutput> {
    private static final Logger log = LoggerFactory.getLogger(SimpleTasklet.class);

    @Override
    public Result<TaskletOutput> execute(TaskletInput taskletInput, TaskletContext context) {
        log.info("Processing tasklet: {}", taskletInput);

        var id = taskletInput.getConfig().getId();
        log.info("Report id: {}", id);

        var secret = context.getSecretValue(taskletInput.getCiToken());
        log.info("Exposed secret: {}", secret.getValue());

        if (id.equals("error")) {
            var userError = UserError.newBuilder()
                    .setIsTransient(true)
                    .setDescription("Something bad happens")
                    .setDetails(Struct.newBuilder()
                            .putFields("error", Value.newBuilder().setStringValue("major").build())
                            .build())
                    .build();
            log.info("User error: {}", userError);
            return Result.ofError(userError);
        } else if (id.equals("runtime")) {
            throw new RuntimeException("Internal error");
        } else {
            var output = TaskletOutput.newBuilder()
                    .setReport(Example.Report.newBuilder()
                            .setUrl("https://a.yandex-team.ru/arcadia/" + id)
                            .setDescription("Link for Arcadia")
                            .build())
                    .build();
            log.info("Output: {}", output);
            return Result.of(output);
        }
    }

    public static void main(String[] args) {
        var tasklet = new SimpleTasklet();

        TaskletRuntime.execute(
                TaskletInput.class,
                TaskletOutput.class,
                args,
                tasklet
        );
    }


}
