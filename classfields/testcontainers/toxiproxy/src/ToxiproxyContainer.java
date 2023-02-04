// forked from https://github.com/testcontainers/testcontainers-java/blob/master/modules/toxiproxy/src/main/java/org/testcontainers/containers/ToxiproxyContainer.java
package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import eu.rekawek.toxiproxy.model.ToxicList;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.DockerImageName;

public class ToxiproxyContainer extends GenericContainer<ToxiproxyContainer> {
    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("shopify/toxiproxy");
    private static final String DEFAULT_TAG = "2.1.0";
    private static final int TOXIPROXY_CONTROL_PORT = 8474;
    private static final int FIRST_PROXIED_PORT = 8666;
    private static final int LAST_PROXIED_PORT = 8697;
    private ToxiproxyClient client;
    private final Map<String, ToxiproxyContainer.ContainerProxy> proxies;
    private final AtomicInteger nextPort;

    /** @deprecated */
    @Deprecated
    public ToxiproxyContainer() {
        this(DEFAULT_IMAGE_NAME.withTag("2.1.0"));
    }

    public ToxiproxyContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public ToxiproxyContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        this.proxies = new HashMap();
        this.nextPort = new AtomicInteger(8666);
        dockerImageName.assertCompatibleWith(new DockerImageName[]{DEFAULT_IMAGE_NAME});
        this.addExposedPorts(new int[]{8474});
        this.setWaitStrategy((new HttpWaitStrategy()).forPath("/version").forPort(8474));

        for(int i = 8666; i <= 8697; ++i) {
            this.addExposedPort(i);
        }

    }

    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        this.client = new ToxiproxyClient(this.getHost(), this.getMappedPort(8474));
    }

    public int getControlPort() {
        return this.getMappedPort(8474);
    }

    public ToxiproxyContainer.ContainerProxy getProxy(GenericContainer<?> container, int port) {
        return this.getProxy((String)container.getNetworkAliases().get(0), port, Optional.empty());
    }

    public ToxiproxyContainer.ContainerProxy getProxy(String hostname, int port, Optional<String> proxyId) {
        String upstream = hostname + ":" + port;
        return (ToxiproxyContainer.ContainerProxy)this.proxies.computeIfAbsent(upstream, (__) -> {
            try {
                int toxiPort = this.nextPort.getAndIncrement();
                if (toxiPort > 8697) {
                    throw new IllegalStateException("Maximum number of proxies exceeded");
                } else {
                    Proxy proxy = this.client.createProxy(proxyId.orElse(upstream), "0.0.0.0:" + toxiPort, upstream);
                    int mappedPort = this.getMappedPort(toxiPort);
                    return new ToxiproxyContainer.ContainerProxy(proxy, this.getHost(), mappedPort, toxiPort);
                }
            } catch (IOException var6) {
                throw new RuntimeException("Proxy could not be created", var6);
            }
        });
    }

    public static class ContainerProxy {
        private static final String CUT_CONNECTION_DOWNSTREAM = "CUT_CONNECTION_DOWNSTREAM";
        private static final String CUT_CONNECTION_UPSTREAM = "CUT_CONNECTION_UPSTREAM";
        private final Proxy toxi;
        private final String containerIpAddress;
        private final int proxyPort;
        private final int originalProxyPort;
        private boolean isCurrentlyCut;

        public String getName() {
            return this.toxi.getName();
        }

        public ToxicList toxics() {
            return this.toxi.toxics();
        }

        public void setConnectionCut(boolean shouldCutConnection) {
            try {
                if (shouldCutConnection) {
                    this.toxics().bandwidth("CUT_CONNECTION_DOWNSTREAM", ToxicDirection.DOWNSTREAM, 0L);
                    this.toxics().bandwidth("CUT_CONNECTION_UPSTREAM", ToxicDirection.UPSTREAM, 0L);
                    this.isCurrentlyCut = true;
                } else if (this.isCurrentlyCut) {
                    this.toxics().get("CUT_CONNECTION_DOWNSTREAM").remove();
                    this.toxics().get("CUT_CONNECTION_UPSTREAM").remove();
                    this.isCurrentlyCut = false;
                }

            } catch (IOException var3) {
                throw new RuntimeException("Could not control proxy", var3);
            }
        }

        protected ContainerProxy(Proxy toxi, String containerIpAddress, int proxyPort, int originalProxyPort) {
            this.toxi = toxi;
            this.containerIpAddress = containerIpAddress;
            this.proxyPort = proxyPort;
            this.originalProxyPort = originalProxyPort;
        }

        public String getContainerIpAddress() {
            return this.containerIpAddress;
        }

        public int getProxyPort() {
            return this.proxyPort;
        }

        public int getOriginalProxyPort() {
            return this.originalProxyPort;
        }
    }
}
