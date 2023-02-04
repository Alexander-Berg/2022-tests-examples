package ru.yandex.payments.tvmlocal.testing;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import ru.yandex.payments.tvmlocal.testing.exception.InvalidOptionsException;
import ru.yandex.payments.tvmlocal.testing.exception.TvmToolLaunchException;
import ru.yandex.payments.tvmlocal.testing.options.Mode;
import ru.yandex.payments.tvmlocal.testing.options.TvmToolOptions;

import static java.util.function.Predicate.not;

@Slf4j
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TvmTool {
    private static final Duration PING_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration START_CHECK_PERIOD = Duration.ofMillis(500);
    private static final int DAEMON_START_RETRIES = 20;
    private static final String DAEMON_NAME = "tvmtool";
    private static final String AUTH_TOKEN_ENV = "TVMTOOL_LOCAL_AUTHTOKEN";

    private final Daemon daemon;
    @Getter
    private final int port;
    private Optional<String> authToken;

    private static <T> boolean is2xx(HttpResponse<T> response) {
        return (response.statusCode() / 100) == 2;
    }

    @SneakyThrows
    public static TvmTool start(BinarySource source, TvmToolOptions options) {
        log.info("Starting tvmtool");

        val configPath = options.configLocation().resolvePath();
        if (!configPath.toFile().isFile()) {
            throw new InvalidOptionsException("Config path " + configPath + " needs to be a regular file");
        }

        val tvmtoolPath = source.fetch();
        val port = options.port().orElseGet(Utils::selectRandomPort);
        val configDir = configPath.toAbsolutePath().getParent();

        val cmd = new ArrayList<>(List.of(
                tvmtoolPath.toString(),
                "--port", String.valueOf(port),
                "-c", configPath.toString(),
                "-v"
        ));

        if (options.mode() == Mode.UNITTEST) {
            cmd.add("--unittest");
        }

        val authToken = Optional.of(options.connectionAuthToken())
                .filter(not(String::isBlank));

        val env = new HashMap<>(options.env());
        authToken.ifPresent(token -> env.put(AUTH_TOKEN_ENV, token));

        val daemon = Daemon.start(DAEMON_NAME, configDir, env, cmd.toArray(String[]::new));
        val tool = new TvmTool(daemon, port, authToken);

        int attempt = 1;
        log.info("Wait for tvmtool start");
        while (daemon.isAlive() && !tool.ping() && attempt <= DAEMON_START_RETRIES) {
            log.warn("[Attempt {}] tvmtool is not started yet", attempt++);
            Thread.sleep(START_CHECK_PERIOD.toMillis());
        }

        if (!daemon.isAlive()) {
            log.error("tvmtool launch failed: exit code: {}", daemon.exitCode());
            throw new TvmToolLaunchException();
        } else if (attempt > DAEMON_START_RETRIES) {
            log.error("tvmtool launch failed: start timeout exceeded");
            tool.stop();
            throw new TvmToolLaunchException();
        }

        log.info("tvmtool started at port {}", tool.getPort());
        return tool;
    }

    public void stop() {
        log.info("Stopping tvmtool");
        daemon.destroy();
        daemon.waitFor();
        log.info("tvmtool stopped");
    }

    @SneakyThrows
    public boolean ping() {
        val client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(PING_TIMEOUT)
                .build();

        val requestBuilder = HttpRequest.newBuilder()
                .GET()
                // NOTE: 127.0.0.1 required for freaking calendar test arguments java.net.preferIPv6Addresses=true
                .uri(new URI("http://127.0.0.1:" + port + "/tvm/ping"))
                .timeout(PING_TIMEOUT);

        authToken.ifPresent(token -> requestBuilder.header("Authorization", token));

        try {
            val response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.discarding());
            if (is2xx(response)) {
                log.info("Ping success");
                return true;
            } else {
                log.error("Ping failed, code = {}", response.statusCode());
                return false;
            }
        } catch (IOException e) {
            log.error("Ping failed", e);
            return false;
        }
    }
}
