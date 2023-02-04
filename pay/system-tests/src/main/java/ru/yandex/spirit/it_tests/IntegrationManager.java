package ru.yandex.spirit.it_tests;

import lombok.experimental.UtilityClass;
import lombok.val;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import ru.yandex.darkspirit.it_tests.DarkspiritClient;
import ru.yandex.darkspirit.it_tests.DatabaseClient;
import ru.yandex.whitespirit.it_tests.utils.Constants;
import ru.yandex.whitespirit.it_tests.whitespirit.client.WhiteSpiritClient;

import java.time.Duration;

import static ru.yandex.spirit.it_tests.Utils.getFile;

@UtilityClass
public class IntegrationManager {
    private static final String WS_SERVICE = "ws";
    private static final int WS_SERVICE_PORT = 8080;
    private static final String DS_SERVICE = "ds";
    private static final int DS_SERVICE_PORT = 5000;
    private static final String DB_SERVICE = "db";
    private static final int DB_PORT = 1521;
    private static final String WIREMOCK_SERVICE = "wiremock";
    private static final int WIREMOCK_PORT = 80;
    private static final ToStringConsumer logConsumer = new ToStringConsumer();
    private static DockerComposeContainer environment = new DockerComposeContainer(getFile("docker-compose.yml"))
            // .withLocalCompose(true)
            .withExposedService(DS_SERVICE, DS_SERVICE_PORT, Wait.forHealthcheck().withStartupTimeout(Duration.ofMinutes(2)))
            .withExposedService(WS_SERVICE, WS_SERVICE_PORT, Wait.forHealthcheck().withStartupTimeout(Duration.ofMinutes(2)))
            .withExposedService(DB_SERVICE, DB_PORT, Wait.defaultWaitStrategy().withStartupTimeout(Duration.ofMinutes(2)))
            .withExposedService(WIREMOCK_SERVICE, WIREMOCK_PORT, Wait.defaultWaitStrategy().withStartupTimeout(Duration.ofMinutes(2)))
            ;

    private static DarkspiritClient darkspiritClient;
    private static WhiteSpiritClient whiteSpiritClient;
    private static DatabaseClient databaseClient;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(environment::stop));
        environment.start();
        darkspiritClient = new DarkspiritClient(getUrl(DS_SERVICE, DS_SERVICE_PORT));
        whiteSpiritClient = new WhiteSpiritClient(getUrl(WS_SERVICE, WS_SERVICE_PORT));
        databaseClient = new DatabaseClient(getUrl(DB_SERVICE, DB_PORT, "jdbc:oracle:thin:ds/tiger@%s:%d/XEPDB1"));
        setGlobalInstances();
    }

    public static int getWiremockPort() {
        return environment.getServicePort(WIREMOCK_SERVICE, WIREMOCK_PORT);
    }

    public static String getWiremockHost() {
        return environment.getServiceHost(WIREMOCK_SERVICE, WIREMOCK_PORT);
    }

    private static String getUrl(String serviceName, int port) {
        return getUrl(serviceName, port, "http://%s:%d");
    }

    private static String getUrl(String serviceName, int port, String pattern) {
        val serviceHost = environment.getServiceHost(serviceName, port);
        val servicePort = environment.getServicePort(serviceName, port);
        return String.format(pattern, serviceHost, servicePort);
    }

    public static DarkspiritClient getDarkspiritClient() {
        return darkspiritClient;
    }

    public static WhiteSpiritClient getWhiteSpiritClient() {
        return whiteSpiritClient;
    }

    public static DatabaseClient getDatabaseClient() {
        return databaseClient;
    }

    public static void pauseService(String serviceName) {
        DockerClientFactory.instance().client()
                .pauseContainerCmd(
                        ((ContainerState)environment.getContainerByServiceName(
                                getContainerInstanceName(serviceName)
                        ).get()).getContainerId()
                ).exec();
    }

    public static void unpauseService(String serviceName) {
        DockerClientFactory.instance().client()
                .unpauseContainerCmd(
                        ((ContainerState)environment.getContainerByServiceName(
                                getContainerInstanceName(serviceName)
                        ).get()).getContainerId()
                ).exec();
    }

    public static void stopService(String serviceName) {
        DockerClientFactory.instance().client()
                .stopContainerCmd(
                        ((ContainerState)environment.getContainerByServiceName(
                                getContainerInstanceName(serviceName)
                        ).get()).getContainerId()
                ).exec();
    }

    public static void startService(String serviceName) {
        DockerClientFactory.instance().client()
                .startContainerCmd(
                        ((ContainerState)environment.getContainerByServiceName(
                                getContainerInstanceName(serviceName)
                        ).get()).getContainerId()
                ).exec();
    }

    private static String getContainerInstanceName(String serviceName) {
        return serviceName + "_1";
    }

    private static void setGlobalInstances() {
        darkspiritClient.putFirm(Constants.HORNS_AND_HOOVES).then().statusCode(200);
        databaseClient.setSigner(
                Constants.HORNS_AND_HOOVES.getInn(), "Ibragim", "Venskiy", "Ivanovich"
        );
        databaseClient.setOfd("7704358518", "localhost", 5555);
    }
}
