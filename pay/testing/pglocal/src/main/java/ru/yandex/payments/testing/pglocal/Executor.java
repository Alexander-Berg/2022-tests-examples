package ru.yandex.payments.testing.pglocal;

import java.io.IOException;
import java.nio.file.Path;

import lombok.val;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;

class Executor {
    private static final String OUTPUT_ENCODING = SystemUtils.IS_OS_WINDOWS ? "cp1251" : "utf8";

    public enum Mode {
        SILENT,
        VERBOSE
    }

    private final Path workingPath;
    private final Logger log;

    Executor(Path workingPath, Logger log) {
        this.workingPath = workingPath;
        this.log = log;
    }

    void run(String... command) {
        run(Mode.VERBOSE, command);
    }

    void run(Mode mode, String... command) {
        val result = execute(mode, command);
        if (result != 0) {
            log.error("Non-zero ({}) exit code", result);
            throw new ExecutionException("Non-zero exit code " + result);
        }
    }

    int execute(String... command) {
        return execute(Mode.VERBOSE, command);
    }

    int execute(Mode mode, String... command) {
        log.info("Executing '{}'", String.join(", ", command));
        try {
            val builder = new ProcessBuilder(command)
                    .directory(workingPath.toFile())
                    .redirectErrorStream(true);

            val process = builder.start();

            if (mode == Mode.VERBOSE) {
                try (val stream = process.getInputStream()) {
                    val output = IOUtils.toString(stream, OUTPUT_ENCODING);
                    log.info(output);
                }
            }

            return process.waitFor();
        } catch (IOException | InterruptedException e) {
            log.error("Command execution failed", e);
            throw new ExecutionException("Execution failed", e);
        }
    }
}
