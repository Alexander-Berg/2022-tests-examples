package ru.yandex.payments.testing.pglocal;

import java.nio.file.Path;

import lombok.val;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;

class PgExecutor {
    static final String PG_CONFIG = "postgresql.conf";
    private static final String PG_SERVER_LOG = "postgresql.log";

    private final Logger log;
    private final String initDbBin;
    private final String pgCtlBin;
    private final String pgBaseBackupBin;
    private final Executor executor;

    PgExecutor(Path pgPath, Path dataDir, Logger log) {
        this.log = log;
        val pgBinPath = pgPath.resolve("bin");
        initDbBin = pgBinPath.resolve("initdb").toString();
        pgCtlBin = pgBinPath.resolve("pg_ctl").toString();
        pgBaseBackupBin = pgBinPath.resolve("pg_basebackup").toString();
        executor = new Executor(dataDir, log);
    }

    void initDb(Path data, String superuser) {
        log.info("starting initdb at '{}'", data);
        executor.run(
                initDbBin,
                "-U", superuser,
                "-E", "UTF-8",
                "--auth-local", "trust",
                "-D", data.toAbsolutePath().toString());
        log.info("initdb complete");
    }

    void start(Path data, int port, ServerType type) {
        log.info("Starting {} server at port {}", type, port);
        executor.run(
                // FIXME: process input stream reading hangs on windows
                SystemUtils.IS_OS_WINDOWS ? Executor.Mode.SILENT : Executor.Mode.VERBOSE,
                pgCtlBin,
                "-D", data.toAbsolutePath().toString(),
                "start", "-w",
                "-l", PG_SERVER_LOG,
                "-o", "--config-file=" + PG_CONFIG);
        log.info("{} server started", type);
    }

    void stop(Path data, ServerType type) {
        log.info("Stop {} server at '{}'", type, data);
        executor.run(
                pgCtlBin,
                "-D", data.toAbsolutePath().toString(),
                "stop", "-w");
        log.info("{} server has been stopped", type);
    }

    void makeBackup(int masterPort, Path destination) {
        log.info("Start backup master database on port '{}'", masterPort);
        executor.run(
                pgBaseBackupBin,
                "-h", "localhost",
                "-U", "replica",
                "-D", destination.toAbsolutePath().toString(),
                "-p", String.valueOf(masterPort),
                "-X", "stream",
                "-R"
        );
        log.info("Backup complete");
    }

    DatabaseStatus status(Path data) {
        return switch (executor.execute(pgCtlBin, "status", "-D", data.toString())) {
            case 3, 4 -> DatabaseStatus.STOPPED;
            default -> DatabaseStatus.STARTED;
        };
    }
}
