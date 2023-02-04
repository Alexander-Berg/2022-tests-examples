package ru.yandex.payments.testing.pglocal;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.Collections.emptyList;

@Slf4j
public class BinarySandboxSource implements BinarySource {
    @Value
    private static class Urls {
        String linux;
        String macOs;
        String windows;
    }

    private static final Map<Version, Urls> PG_URL = Map.of(
            Version.V12, new Urls(
                    "https://proxy.sandbox.yandex-team.ru/1650221996",
                    "https://proxy.sandbox.yandex-team.ru/1650222731",
                    "https://proxy.sandbox.yandex-team.ru/1650223868"
            )
    );
    private static final String MARKER_FILE_NAME = "marker";
    private static final String ARCHIVE_FILE_NAME = "postgres.tar.gz";

    private final Version version;
    private final Path binariesDir;
    private final Path pgArchivePath;
    private final Path markerFilePath;

    public BinarySandboxSource(Version version) {
        this.version = version;
        binariesDir = Paths.get(System.getProperty("java.io.tmpdir"), "pg_sandbox_" + version);
        markerFilePath = binariesDir.resolve(MARKER_FILE_NAME);
        pgArchivePath = binariesDir.resolve(ARCHIVE_FILE_NAME);
    }

    @Override
    public Path fetch() {
        if (markerFilePath.toFile().exists()) {
            log.info("Postgres binaries up to date");
        } else {
            log.info("Postgres binaries not found");
            prepareBinaries();
        }

        return binariesDir.resolve("mail-postgresql");
    }

    private static String resolveUrl(Version version) {
        val versionUrls = PG_URL.get(version);
        if (versionUrls == null) {
            throw new RuntimeException("Unsupported PostgreSQL version");
        }

        if (SystemUtils.IS_OS_WINDOWS) {
            return versionUrls.windows;
        } else if (SystemUtils.IS_OS_LINUX) {
            return versionUrls.linux;
        } else if (SystemUtils.IS_OS_MAC_OSX) {
            return versionUrls.macOs;
        } else {
            throw new RuntimeException("Unsupported OS used");
        }
    }

    private void prepareBinaries() {
        if (binariesDir.toFile().exists()) {
            try {
                FileUtils.forceDelete(binariesDir.toFile());
            } catch (IOException e) {
                log.error("Error preparing binary folder", e);
                throw new UncheckedIOException(e);
            }
        }

        if (!binariesDir.toFile().mkdirs()) {
            log.error("Unable create '{}' binaries folder", binariesDir);
            throw new RuntimeException("Unable create '" + binariesDir + "' binaries folder");
        }

        downloadBinaries();
        extractBinaries();
        createMarkerFile();
    }

    private void downloadBinaries() {
        log.info("Download postgres binaries...");

        val url = resolveUrl(version);

        try (val input = new BufferedInputStream(new URL(url).openStream());
             val output = new FileOutputStream(pgArchivePath.toFile())) {
            IOUtils.copy(input, output);
        } catch (IOException e) {
            log.error("Error loading postgres binaries", e);
            throw new UncheckedIOException(e);
        }

        log.info("Download postgres binaries...done");
    }

    private void extractBinaries() {
        log.info("Extract postgres binaries...");

        try (val inputStream = new FileInputStream(pgArchivePath.toFile());
             val gzipInput = new GzipCompressorInputStream(inputStream);
             val tarInput = new TarArchiveInputStream(gzipInput)) {
            TarArchiveEntry entry;
            while ((entry = (TarArchiveEntry) tarInput.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                val entryName = entry.getName();
                val filePath = binariesDir.resolve(entryName);
                val parent = filePath.getParent().toFile();

                if (!parent.exists()) {
                    if (!parent.mkdirs()) {
                        val path = parent.getAbsolutePath();
                        log.error("Unable to create directory '{}', during postgres archive extraction", path);
                        throw new RuntimeException("Unable to create directory " + path);
                    }
                }

                if (entry.isSymbolicLink() || entry.isLink()) {
                    Files.createSymbolicLink(filePath, Paths.get(entry.getLinkName()));
                } else {
                    try (val output = new FileOutputStream(filePath.toFile(), false)) {
                        IOUtils.copy(tarInput, output);
                    }

                    if (entryName.contains("/bin/")) {
                        if (!filePath.toFile().setExecutable(true)) {
                            throw new RuntimeException("Can't set executable flag for " + entryName);
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error extracting postgres archive", e);
            throw new UncheckedIOException(e);
        }

        log.info("Extract postgres binaries...done");
    }

    private void createMarkerFile() {
        try {
            Files.write(markerFilePath, emptyList(), WRITE, TRUNCATE_EXISTING, CREATE);
        } catch (IOException e) {
            log.error("Error creating marker file", e);
            throw new UncheckedIOException(e);
        }
    }
}
