package ru.yandex.payments.tvmlocal.testing;


import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Scanner;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.payments.tvmlocal.testing.exception.ExecutionException;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
class Daemon {
    private final Process process;

    private static void redirectIO(InputStream stream, Logger log) {
        new Thread(() -> {
            val scanner = new Scanner(stream);
            while (scanner.hasNextLine()) {
                log.info(scanner.nextLine());
            }
        }).start();
    }

    private static void registerStopHook(Process process) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }));
    }

    public static Daemon start(String name, Path workingPath, Map<String, String> environment, String... command) {
        val log = LoggerFactory.getLogger(name);
        log.info("Starting daemon '{}'", String.join(", ", command));

        try {
            val builder = new ProcessBuilder(command)
                    .directory(workingPath.toFile())
                    .redirectErrorStream(true);

            builder.environment()
                    .putAll(environment);

            val process = builder.start();
            redirectIO(process.getInputStream(), log);
            registerStopHook(process);
            log.info("Started");

            return new Daemon(process);
        } catch (IOException e) {
            log.error("Command execution failed", e);
            throw new ExecutionException("Execution failed", e);
        }
    }

    public boolean isAlive() {
        return process.isAlive();
    }

    public int exitCode() {
        return process.exitValue();
    }

    @SneakyThrows
    public int waitFor() {
        return process.waitFor();
    }

    public void destroy() {
        process.destroy();
    }
}
