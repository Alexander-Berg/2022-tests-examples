package ru.yandex.payments.tvmlocal.testing;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.SystemUtils;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

@Slf4j
public class BinarySandboxSource implements BinarySource {
    private static final String TVMTOOL_WINDOWS_URL = "https://proxy.sandbox.yandex-team.ru/1550524123";
    private static final String TVMTOOL_LINUX_URL = "https://proxy.sandbox.yandex-team.ru/1550524011";
    private static final String TVMTOOL_MAC_URL = "https://proxy.sandbox.yandex-team.ru/1550524070";

    private static final String MARKER_FILE_NAME = "marker";
    private static final String LOCK_FILE_NAME = ".lock";

    private final Path filesPath;
    private final Path binaryPath;
    private final Path markerPath;
    private final Path lockPath;

    public BinarySandboxSource() {
        filesPath = Paths.get(System.getProperty("java.io.tmpdir"), "tvmlocal");
        binaryPath = filesPath.resolve(resolveBinaryName());
        markerPath = filesPath.resolve(MARKER_FILE_NAME);
        lockPath = filesPath.resolve(LOCK_FILE_NAME);
    }

    private static String resolveBinaryName() {
        return SystemUtils.IS_OS_WINDOWS ? "tvmtool.exe" : "tvmtool";
    }

    private static String resolveUrl() {
        if (SystemUtils.IS_OS_WINDOWS) {
            return TVMTOOL_WINDOWS_URL;
        } else if (SystemUtils.IS_OS_LINUX) {
            return TVMTOOL_LINUX_URL;
        } else if (SystemUtils.IS_OS_MAC_OSX) {
            return TVMTOOL_MAC_URL;
        } else {
            throw new RuntimeException("Unsupported OS used. Just install windows to fix the issue");
        }
    }

    private Optional<String> readMarker() {
        if (markerPath.toFile().exists()) {
            log.info("Marker file found");

            try {
                val url = Files.readString(markerPath);
                log.info("tvmtool has been downloaded from {}", url);
                return Optional.of(url);
            } catch (Throwable e) {
                log.error("Error reading marker file", e);
                return Optional.empty();
            }
        } else {
            log.info("Marker file not found, will download fresh binaries");
            return Optional.empty();
        }
    }

    private void writeMarker(String url) {
        try {
            Files.write(markerPath, List.of(url), WRITE, TRUNCATE_EXISTING, CREATE);
        } catch (IOException e) {
            log.error("Error creating marker file", e);
            throw new UncheckedIOException(e);
        }
    }

    @Override
    @SneakyThrows
    public Path fetch() {
        filesPath.toFile().mkdirs();

        try (val ignored = FsLock.open(lockPath)) {
            log.debug("Managed to get a lock {}", ignored);

            var existingUrl = "";
            if (binaryPath.toFile().exists()) {
                log.info("tvmtool binary found");
                existingUrl = readMarker().orElse("").trim();
            } else {
                log.info("tvmtool binary not found");
            }

            try {
                downloadBinary(existingUrl);
            } catch (Throwable e) {
                log.error("Error fetching tvmtool binary", e);
                throw e;
            }
        }

        return binaryPath;
    }

    private void downloadBinary(String existingUrl) {
        log.info("Trying download tvmtool binary...");

        val url = resolveUrl();
        if (url.equals(existingUrl)) {
            log.info("tvmtool binary is up to date");
            return;
        }

        try (val input = new BufferedInputStream(new URL(url).openStream());
             val output = new FileOutputStream(binaryPath.toFile())) {
            input.transferTo(output);
        } catch (IOException e) {
            log.error("Error loading tvmtool binary", e);
            throw new UncheckedIOException(e);
        }

        binaryPath.toFile().setExecutable(true);
        writeMarker(url);

        log.info("Download tvmtool binary...done");
    }
}
