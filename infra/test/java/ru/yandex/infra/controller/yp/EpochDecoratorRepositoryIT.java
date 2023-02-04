package ru.yandex.infra.controller.yp;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import com.google.common.net.HostAndPort;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.infra.controller.dto.SchemaMeta;
import ru.yandex.infra.controller.metrics.GaugeRegistry;
import ru.yandex.infra.controller.testutil.FutureUtils;
import ru.yandex.yp.YpInstance;
import ru.yandex.yp.YpRawClientBuilder;
import ru.yandex.yp.YpRawObjectService;
import ru.yandex.yp.client.api.Autogen.TSchemaMeta;
import ru.yandex.yp.client.api.TReplicaSetSpec;
import ru.yandex.yp.client.api.TReplicaSetStatus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.infra.controller.yp.TestYpRepositoryUtils.cleanup;
import static ru.yandex.infra.controller.yp.TestYpRepositoryUtils.getLabelsYson;
import static ru.yandex.infra.controller.yp.TestYpRepositoryUtils.getSpec;
import static ru.yandex.yp.model.YpObjectType.REPLICA_SET;

public class EpochDecoratorRepositoryIT {
    private static final TReplicaSetSpec SPEC = TReplicaSetSpec.newBuilder()
            .setAccountId("tmp")
            .build();

    private static final TReplicaSetSpec UPDATED_SPEC = TReplicaSetSpec.newBuilder()
            .setAccountId("tmp")
            .setReplicaCount(100500)
            .build();

    private static final long CURRENT_EPOCH = 5L;
    private static final String EPOCH_LABEL = "epoch";
    private static final Map<String, String> FILTER_LABELS = ImmutableMap.of("key", "value");
    private static final ObjectBuilderDescriptor<TSchemaMeta, SchemaMeta> DESCRIPTOR =
            new ObjectBuilderDescriptor<>(TReplicaSetSpec::newBuilder, TReplicaSetStatus::newBuilder,
                    SchemaMeta::fromProto, TSchemaMeta.getDefaultInstance());
    private static final int PAGE_SIZE = 2;
    private static final String ID = "id";

    private YpRawObjectService ypClient;
    private EpochDecoratorRepository<SchemaMeta, TReplicaSetSpec, TReplicaSetStatus> repository;
    static final GaugeRegistry gaugeRegistry = GaugeRegistry.EMPTY;

    @BeforeEach
    void before() throws Exception {
        HostAndPort masterHostPort = HostAndPort.fromString(System.getenv("YP_MASTER_GRPC_INSECURE_ADDR"));
        YpInstance masterInstance = new YpInstance(masterHostPort.getHost(), masterHostPort.getPort());
        ypClient = new YpRawClientBuilder(masterInstance, () -> "fake_token")
                .setUseMasterDiscovery(false)
                .setUsePlaintext(true)
                .build()
                .objectService();
        cleanup(ypClient, REPLICA_SET);

        repository = new EpochDecoratorRepository<>(new LabelBasedRepository<>(REPLICA_SET, FILTER_LABELS, Optional.empty(), ypClient, DESCRIPTOR, PAGE_SIZE, gaugeRegistry),
                REPLICA_SET, () -> CURRENT_EPOCH, EPOCH_LABEL
        );
    }

    @Test
    void updateObjectWithEmptyEpoch() {
        successfulUpdateTestTemplate(Collections.emptyMap());
        assertThat(getEpoch(ID), equalTo(CURRENT_EPOCH));
    }

    @Test
    void updateObjectWithLowerEpoch() {
        successfulUpdateTestTemplate(ImmutableMap.of(EPOCH_LABEL, CURRENT_EPOCH - 1));
        assertThat(getEpoch(ID), equalTo(CURRENT_EPOCH));
    }

    @Test
    void updateObjectWithSameEpoch() {
        successfulUpdateTestTemplate(ImmutableMap.of(EPOCH_LABEL, CURRENT_EPOCH));
    }

    @Test
    void notUpdateObjectWithGreaterEpoch() {
        TestYpRepositoryUtils.createReplicaSet(ypClient, ID, SPEC, ImmutableMap.of(EPOCH_LABEL, CURRENT_EPOCH + 1));
        Assertions.assertThrows(Exception.class,
                () -> FutureUtils.get5s(repository.updateObjectSpec(ID, UPDATED_SPEC)));
        assertThat(getSpec(ypClient, ID, DESCRIPTOR.createSpecBuilder(), REPLICA_SET), equalTo(SPEC));
    }

    @Test
    void notRemoveObjectWithGreaterEpoch() {
        TestYpRepositoryUtils.createReplicaSet(ypClient, ID, SPEC, ImmutableMap.of(EPOCH_LABEL, CURRENT_EPOCH + 1));
        Assertions.assertThrows(Exception.class, () -> FutureUtils.get5s(repository.removeObject(ID)));
        assertThat(getSpec(ypClient, ID, DESCRIPTOR.createSpecBuilder(), REPLICA_SET), equalTo(SPEC));
    }

    @Test
    void setEpochForCreatedObject() {
        FutureUtils.get5s(repository.createObject(ID, new CreateObjectRequest.Builder<>(SPEC).build()));
        assertThat(getEpoch(ID), equalTo(CURRENT_EPOCH));
    }

    private void successfulUpdateTestTemplate(Map<String, ?> objectLabels) {
        TestYpRepositoryUtils.createReplicaSet(ypClient, ID, SPEC, objectLabels);
        FutureUtils.get5s(repository.updateObjectSpec(ID, UPDATED_SPEC));
        assertThat(getSpec(ypClient, ID, DESCRIPTOR.createSpecBuilder(), REPLICA_SET), equalTo(UPDATED_SPEC));
    }

    private long getEpoch(String id) {
        return getLabelsYson(ypClient, id, REPLICA_SET).getLong(EPOCH_LABEL);
    }

}
