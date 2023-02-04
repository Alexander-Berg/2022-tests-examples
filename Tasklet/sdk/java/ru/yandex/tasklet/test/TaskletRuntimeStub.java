package ru.yandex.tasklet.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.tasklet.Result;
import ru.yandex.tasklet.TaskletUtils;
import ru.yandex.tasklet.api.v2.WellKnownStructures.UserError;

public class TaskletRuntimeStub<Input extends Message, Output extends Message> implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(TaskletRuntimeStub.class);

    private final Output defaultOutputMessage;
    private final TaskletContextStub taskletContextStub;

    public TaskletRuntimeStub(Class<Input> inputMessageType, Class<Output> outputMessageType) {
        this.defaultOutputMessage = TaskletUtils.getDefaultMessageInstance(outputMessageType);
        this.taskletContextStub = TaskletContextStub.networkStub(inputMessageType, outputMessageType);
    }

    public Result<Output> execute(Input input, Consumer<String[]> mainClass) {
        Path tmpDir;
        try {
            tmpDir = Files.createTempDirectory("tasklet-runtime-stub");
        } catch (IOException e) {
            throw new RuntimeException("Unable to create temp directory", e);
        }
        Path inputFile = tmpDir.resolve("input.proto");
        Path outputFile = tmpDir.resolve("output.proto");
        Path errorFile = tmpDir.resolve("error.proto");
        try {
            var inputFileReference = inputFile.toString();
            var outputFileReference = outputFile.toString();
            var errorFileReference = errorFile.toString();

            var args = new String[]{
                    "localhost:" + taskletContextStub.getServer().getPort(),
                    inputFileReference,
                    outputFileReference,
                    errorFileReference
            };
            TaskletUtils.writeProto(inputFileReference, input);
            Files.deleteIfExists(outputFile);
            Files.deleteIfExists(errorFile);

            mainClass.accept(args);

            if (Files.exists(outputFile)) {
                var output = TaskletUtils.readProto(outputFileReference, defaultOutputMessage);
                return Result.of(output);
            } else if (Files.exists(errorFile)) {
                var error = TaskletUtils.readProto(errorFileReference, UserError.getDefaultInstance());
                return Result.ofError(error);
            } else {
                throw new RuntimeException("Internal error, unable to read result");
            }
        } catch (IOException io) {
            throw new RuntimeException("Error when trying to work with files", io);
        } finally {
            try {
                Files.deleteIfExists(inputFile);
                Files.deleteIfExists(outputFile);
                Files.deleteIfExists(errorFile);
                Files.delete(tmpDir);
            } catch (IOException e) {
                log.error("Unable to clean temp files", e);
            }
        }
    }

    public TaskletContextStub getTaskletContextStub() {
        return taskletContextStub;
    }

    @Override
    public void close() {
        taskletContextStub.close();
    }

    public static <Input extends Message, Output extends Message> TaskletRuntimeStub<Input, Output> networkBasedStub(
            Class<Input> inputMessageType,
            Class<Output> outputMessageType
    ) {
        return new TaskletRuntimeStub<>(inputMessageType, outputMessageType);
    }
}
