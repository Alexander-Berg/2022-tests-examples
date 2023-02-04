package ru.yandex.payments.testing.pglocal;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.text.StringSubstitutor;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static ru.yandex.payments.testing.pglocal.PgExecutor.PG_CONFIG;

@Slf4j
public class Manager {
    private static final String REPLICA_USER = "replica";
    private static final String DYNAMIC_SHARED_MEMORY_TYPE = SystemUtils.IS_OS_WINDOWS ? "windows" : "posix";
    private static final String UNIX_SOCKET_DIRECTORIES = SystemUtils.IS_OS_WINDOWS ? "''" : "'/tmp'";

    private final Path pgPath;
    private final PgExecutor executor;

    public Manager(BinarySource binarySource) {
        pgPath = binarySource.fetch();
        executor = new PgExecutor(pgPath, pgPath, log);
    }

    public static int randomPort() {
        try (val socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static record SlaveOptions(int port,
                                      String user,
                                      String applicationName,
                                      int masterPort) {
    }

    public static record MasterOptions(int port,
                                       String user,
                                       SynchronousCommit synchronousCommit,
                                       List<String> synchronousStandbyNames) {
    }

    public Server startNewServer(Path data, MasterOptions options) {
        cleanupData(data.toFile());
        executor.initDb(data, options.user());
        preparePgConfig(data, options);
        preparePgHbaConfig(data);

        val server = new Server(pgPath, data, options.port(), options.user(), ServerType.MASTER);
        server.start();
        server.attachDefaultDatabase().execute("CREATE USER " + REPLICA_USER + " REPLICATION LOGIN");
        return server;
    }

    public Server startServer(Path data, MasterOptions options) {
        preparePgConfig(data, options);
        preparePgHbaConfig(data);
        val server = new Server(pgPath, data, options.port(), options.user(), ServerType.MASTER);
        server.start();
        return server;
    }

    public Server startNewServer(Path data, SlaveOptions options) {
        cleanupData(data.toFile());
        executor.makeBackup(options.masterPort(), data);
        return startServer(data, options);
    }

    public Server startServer(Path data, SlaveOptions options) {
        preparePgConfig(data, options);
        val server = new Server(pgPath, data, options.port(), options.user(), ServerType.SLAVE);
        server.start();
        return server;
    }

    private static void preparePgConfig(Path data, MasterOptions options) {
        val config = Map.of(
                "port", String.valueOf(options.port()),
                "synchronous_commit", options.synchronousCommit().configValue(),
                "unix_socket_directories", UNIX_SOCKET_DIRECTORIES,
                "dynamic_shared_memory_type", DYNAMIC_SHARED_MEMORY_TYPE
        );

        copyTemplateFile("pg_master.conf", data.resolve(PG_CONFIG), config);
    }

    private static void preparePgConfig(Path data, SlaveOptions options) {
        val primaryConninfo = String.format("'host=localhost port=%d user=replica application_name=%s'",
                options.masterPort, options.applicationName);

        val config = Map.of(
                "port", String.valueOf(options.port()),
                "synchronous_commit", SynchronousCommit.OFF.configValue(),
                "unix_socket_directories", UNIX_SOCKET_DIRECTORIES,
                "dynamic_shared_memory_type", DYNAMIC_SHARED_MEMORY_TYPE,
                "primary_conninfo", primaryConninfo
        );

        copyTemplateFile("pg_slave.conf", data.resolve(PG_CONFIG), config);
    }

    private static void preparePgHbaConfig(Path data) {
        val hbaTemplate = SystemUtils.IS_OS_WINDOWS ? "pg_hba_win.conf" : "pg_hba_unix.conf";
        copyTemplateFile(hbaTemplate, data.resolve("pg_hba.conf"), Collections.emptyMap());
    }

    private static void copyTemplateFile(String name, Path destination, Map<String, String> values) {
        try {
            val resourceStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
            if (resourceStream == null) {
                log.error("Resource '{}' not found", name);
                throw new RuntimeException("Resource '" + name + "' not found");
            }

            val substitutor = new StringSubstitutor(values, "{", "}");

            val template = IOUtils.toString(resourceStream);
            val text = substitutor.replace(template);
            Files.write(destination, text.getBytes(), WRITE, TRUNCATE_EXISTING, CREATE);
        } catch (IOException e) {
            log.error("Error reading template file '{}'", name);
            throw new UncheckedIOException(e);
        }
    }

    private static void cleanupData(File dataDir) {
        if (dataDir.exists()) {
            try {
                FileUtils.forceDelete(dataDir);
            } catch (IOException e) {
                log.error("Data folder '" + dataDir + "' cleanup failed", e);
                throw new UncheckedIOException(e);
            }
        }
    }
}
