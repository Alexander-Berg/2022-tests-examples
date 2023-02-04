package ru.yandex.whitespirit.it_tests.whitespirit.providers;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import ru.yandex.whitespirit.it_tests.configuration.KKT;

import static ru.yandex.whitespirit.it_tests.utils.Utils.getResourceAsStream;
import static ru.yandex.whitespirit.it_tests.utils.Utils.inputStreamToFile;

@Slf4j
public class LocalServiceProvider implements ServiceProvider {
    private static final WaitStrategy KKT_WAIT_STRATEGY = new HttpWaitStrategy()
            .forPath("/fr/api/v2/NoOperation")
            .forStatusCode(200)
            .withReadTimeout(Duration.ofSeconds(30));
    private static final String WS_SERVICE_NAME = "ws";
    private static final String HUDSUCKER_SERVICE_NAME = "hudsucker";
    private static final int WS_PORT = 8080;
    private static final int KKT_PORT = 4444;
    private static final int HUDSUCKER_PORT = 6666;
    private static final int DEFAULT_DOCKER_COMPOSE_INSTANCE = 1;
    private static final String DOCKER_COMPOSE_FILENAME = "docker-compose.yaml";
    private static final String DOCKER_COMPOSE_NO_MGM_FILENAME = "docker-compose-no-mgm.yaml";


    @SuppressWarnings("rawtypes")
    private final DockerComposeContainer dockerComposeContainer;

    private static EntryStream<String, String> getIpEnv(Map<String, KKT> kkts, boolean virtualFn, int startAddr) {
        val startAddrAtomic = new AtomicInteger(startAddr);

        return EntryStream.of(kkts)
                .filterValues(kkt -> kkt.isUseVirtualFn() == virtualFn)
                .mapKeys(key -> String.format("%s_ip_addr", key))
                .mapValues(kkt -> String.format("171.42.42.%d", startAddrAtomic.getAndIncrement()));
    }

    private static Map<String, String> getIpEnv(Map<String, KKT> kkts) {
        val ipEnv = getIpEnv(kkts, true, 16)
                .append(getIpEnv(kkts, false, 128))
                .toImmutableMap();

        log.info("IP Env {}", ipEnv);
        return ipEnv;
    }

    public LocalServiceProvider(Predicate<String> wsReadyPredicate, Map<String, KKT> kkts, boolean useLocalCompose,
                                boolean disableMgmTests) {
        val wsWaitStrategy = new HttpWaitStrategy()
                .forPath("/v1/info")
                .forResponsePredicate(wsReadyPredicate)
                .withReadTimeout(Duration.ofSeconds(10));
        val hudsuckerWaitStrategy = new HttpWaitStrategy()
            .forPath("/ping")
            .forStatusCode(200)
            .withReadTimeout(Duration.ofSeconds(10));

        dockerComposeContainer = new DockerComposeContainer<>(
                inputStreamToFile(getResourceAsStream(disableMgmTests? DOCKER_COMPOSE_NO_MGM_FILENAME : DOCKER_COMPOSE_FILENAME), DOCKER_COMPOSE_FILENAME))
                .withLocalCompose(useLocalCompose)
                .withEnv(getIpEnv(kkts));

        dockerComposeContainer.withExposedService(WS_SERVICE_NAME, DEFAULT_DOCKER_COMPOSE_INSTANCE, WS_PORT, wsWaitStrategy);
        dockerComposeContainer.withExposedService(HUDSUCKER_SERVICE_NAME, DEFAULT_DOCKER_COMPOSE_INSTANCE, HUDSUCKER_PORT, hudsuckerWaitStrategy);
        kkts.keySet().forEach(kktServiceName -> {
            dockerComposeContainer.withExposedService(kktServiceName, DEFAULT_DOCKER_COMPOSE_INSTANCE, KKT_PORT, KKT_WAIT_STRATEGY);
        });

        Stream.of("kkt005").forEach(name -> dockerComposeContainer.withLogConsumer(name, new Slf4jLogConsumer(log).withPrefix(name)));
    }

    @Override
    public String getWhitespiritUrl() {
        return getUrl(WS_SERVICE_NAME, WS_PORT);
    }

    private String getUrl(String serviceName, int port) {
        val serviceHost = dockerComposeContainer.getServiceHost(serviceName, port);
        val servicePort = dockerComposeContainer.getServicePort(serviceName, port);
        return String.format("http://%s:%d", serviceHost, servicePort);
    }

    @Override
    public String getHudsuckerUrl() {
        return getUrl(HUDSUCKER_SERVICE_NAME, HUDSUCKER_PORT);
    }

    @Override
    public void onStart() {
        dockerComposeContainer.start();
    }

    @Override
    public void onShutdown() {
        dockerComposeContainer.stop();
    }
}
