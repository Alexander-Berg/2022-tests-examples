package ru.yandex.solomon.alert;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.discovery.DiscoveryService;
import ru.yandex.discovery.DiscoveryServices;
import ru.yandex.discovery.cluster.ClusterMapper;
import ru.yandex.discovery.cluster.ClusterMapperImpl;
import ru.yandex.grpc.conf.ClientOptionsFactory;
import ru.yandex.misc.lang.Validate;
import ru.yandex.monlib.metrics.registry.MetricRegistry;
import ru.yandex.solomon.alert.api.converters.AlertConverter;
import ru.yandex.solomon.alert.client.AlertingClient;
import ru.yandex.solomon.alert.client.AlertingClients;
import ru.yandex.solomon.alert.protobuf.ERequestStatusCode;
import ru.yandex.solomon.alert.protobuf.TReadAlertRequest;
import ru.yandex.solomon.alert.rule.AlertRuleFactory;
import ru.yandex.solomon.alert.rule.AlertRuleFactoryImpl;
import ru.yandex.solomon.alert.rule.AlertRuleFairDeadlines;
import ru.yandex.solomon.alert.rule.ExplainResult;
import ru.yandex.solomon.alert.rule.usage.ProjectAlertRuleMetrics;
import ru.yandex.solomon.alert.statuses.AlertingStatusesSelectorImpl;
import ru.yandex.solomon.alert.template.MustacheTemplateFactory;
import ru.yandex.solomon.config.SolomonConfigs;
import ru.yandex.solomon.config.protobuf.alert.TAlertingConfig;
import ru.yandex.solomon.config.thread.StubThreadPoolProvider;
import ru.yandex.solomon.config.thread.ThreadPoolProvider;
import ru.yandex.solomon.flags.FeatureFlagHolderStub;
import ru.yandex.solomon.metrics.client.MetricsClient;
import ru.yandex.solomon.metrics.client.MetricsClientContext;
import ru.yandex.solomon.metrics.client.cache.CachingMetricsClientImpl;
import ru.yandex.solomon.metrics.client.cache.FindCacheOptions;
import ru.yandex.solomon.metrics.client.cache.MetabaseFindCache;
import ru.yandex.solomon.metrics.client.cache.MetabaseFindCacheImpl;

/**
 * @author Vladimir Gordiychuk
 */
@ParametersAreNonnullByDefault
public class DebugEvaluation {
    public static void main(String[] args) {
        {
            long actual = LocalDateTime.of(2014, 12, 14, 17, 40).atZone(ZoneId.of("Europe/Moscow")).toEpochSecond();
            long expected = 1418568000;
            Validate.equals(expected, actual);
        }

        // TODO: wtf?
        final String config = "/home/gordiychuk/project/ya/arcadia/solomon/configs/prestable/alerting.conf";
        final String alertAddress = "conductor_group://solomon_pre_alerting:8799";

        TAlertingConfig cfg = SolomonConfigs.parseConfig(config, TAlertingConfig.getDefaultInstance());
        try (Dependency dependency = new Dependency(cfg, alertAddress)) {
            System.out.println("NOW " + Instant.now());
            evaluate(dependency, "solomon", "alerting-deadline");
        }
    }

    private static void evaluate(Dependency dependency, String projectId, String alertId) {
        ExplainResult result = dependency.alertingClient.readAlert(TReadAlertRequest.newBuilder()
                .setAlertId(alertId)
                .setProjectId(projectId)
                .build())
                .thenApply(response -> {
                    if (response.getRequestStatus() != ERequestStatusCode.OK) {
                        throw new IllegalStateException(response.toString());
                    }

                    return AlertConverter.protoToAlert(response.getAlert());
                })
                .thenApply(dependency.ruleFactory::createAlertRule)
                .thenCompose(rule -> {
                    System.out.println("NOW2 " + Instant.now());
                    return rule.explain(Instant.now(), AlertRuleFairDeadlines.ignoreLag(Instant.now(), 3, TimeUnit.MINUTES));
                })
                .join();

        System.out.println(result.getStatus());
        System.out.println(result.getSeries());
    }

    private static class Dependency implements AutoCloseable {
        private MetricRegistry registry = new MetricRegistry();
        private ThreadPoolProvider threadpool;
        private MetricsClient metricsClient;
        private AlertRuleFactory ruleFactory;
        private AlertingClient alertingClient;

        public Dependency(TAlertingConfig config, String alertingAddress) {
            threadpool = new StubThreadPoolProvider();
            ClusterMapper clusterMapper = new ClusterMapperImpl(config.getClustersConfigList(), DiscoveryService.async(), threadpool.getExecutorService("", ""), threadpool.getSchedulerExecutorService());
            ClientOptionsFactory clientOptionsFactory = new ClientOptionsFactory(Optional.empty(), Optional.empty(), threadpool);
            var metricsClientCtx = new MetricsClientContext(threadpool, registry, clusterMapper,
                    Optional.empty(), Optional.empty(), clientOptionsFactory);
            metricsClient = metricsClientCtx.metricsClient(
                    config.getClientId(),
                    config.getMetabaseClientConfig(),
                    config.getStockpileClientConfig(),
                    Optional.empty(),
                    Optional.empty());
            FindCacheOptions cacheOptions = FindCacheOptions.newBuilder()
                    .setMaxSize(1000)
                    .build();
            MetabaseFindCache metabaseFindCache = new MetabaseFindCacheImpl(metricsClient, cacheOptions);
            MetricsClient cachingMetricsClient = new CachingMetricsClientImpl(metricsClient, metabaseFindCache);
            var featureFlags = new FeatureFlagHolderStub();
            ruleFactory = new AlertRuleFactoryImpl(cachingMetricsClient,
                    new ProjectAlertRuleMetrics(),
                    new MustacheTemplateFactory(),
                    new AlertingStatusesSelectorImpl(null),
                    featureFlags);
            alertingClient = AlertingClients.create(DiscoveryServices.resolve(Collections.singletonList(alertingAddress)));
        }

        @Override
        public void close() {
            if (alertingClient != null) {
                alertingClient.close();
            }

            if (metricsClient != null) {
                metricsClient.close();
            }

            if (threadpool != null) {
                threadpool.close();
            }
        }
    }
}
