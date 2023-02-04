package ru.yandex.infra.controller.yp;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.net.HostAndPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.bolts.collection.Try;
import ru.yandex.infra.controller.dto.Acl;
import ru.yandex.infra.controller.dto.SchemaMeta;
import ru.yandex.infra.controller.metrics.GaugeRegistry;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.library.svnversion.VcsVersion;
import ru.yandex.yp.YpInstance;
import ru.yandex.yp.YpRawClientBuilder;
import ru.yandex.yp.YpRawObjectService;
import ru.yandex.yp.client.api.AccessControl;
import ru.yandex.yp.client.api.Autogen.TSchemaMeta;
import ru.yandex.yp.client.api.TProjectSpec;
import ru.yandex.yp.client.api.TReplicaSetSpec;
import ru.yandex.yp.client.api.TReplicaSetStatus;
import ru.yandex.yp.client.api.TStageSpec;
import ru.yandex.yp.client.api.TStageStatus;
import ru.yandex.yp.model.YpAttributeTimestampPrerequisite;
import ru.yandex.yp.model.YpEventType;
import ru.yandex.yp.model.YpObjectType;
import ru.yandex.yp.model.YpPayloadFormat;
import ru.yandex.yp.model.YpSelectStatement;
import ru.yandex.yp.model.YpTransaction;

import static com.spotify.hamcrest.optional.OptionalMatchers.emptyOptional;
import static com.spotify.hamcrest.optional.OptionalMatchers.optionalWithValue;
import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.infra.controller.testutil.FutureUtils.get1s;
import static ru.yandex.infra.controller.testutil.FutureUtils.get5s;
import static ru.yandex.infra.controller.yp.TestYpRepositoryUtils.cleanup;
import static ru.yandex.infra.controller.yp.TestYpRepositoryUtils.create;
import static ru.yandex.infra.controller.yp.TestYpRepositoryUtils.getAnnotationsYson;
import static ru.yandex.infra.controller.yp.TestYpRepositoryUtils.getLabelsYson;
import static ru.yandex.yp.model.YpObjectType.REPLICA_SET;
import static ru.yandex.yp.model.YpObjectType.STAGE;

class LabelBasedRepositoryIT {
    private static final TReplicaSetSpec SPEC = createSimpleReplicaSetSpec(0);
    private static final TReplicaSetSpec UPDATED_SPEC = createSimpleReplicaSetSpec(100500);

    private static final Map<String, String> FILTER_LABELS = ImmutableMap.of("key", "value");
    private static final ObjectBuilderDescriptor<TSchemaMeta, SchemaMeta> DESCRIPTOR =
            new ObjectBuilderDescriptor<>(TReplicaSetSpec::newBuilder, TReplicaSetStatus::newBuilder,
                    SchemaMeta::fromProto, TSchemaMeta.getDefaultInstance());
    private static final Selector SPEC_META_STATUS_SELECTOR =
            new Selector.Builder().withMeta().withStatus().withSpecAndTimestamps().build();
    private static final Selector SPEC_SELECTOR = new Selector.Builder().withSpecAndTimestamps().build();

    private static final int PAGE_SIZE = 2;
    private static final String ID = "id";
    private static final String VCS_LABEL = "vcs";
    private static final Duration WATCH_TIME_LIMIT = Duration.ofSeconds(1);

    private YpRawObjectService ypClient;
    private LabelBasedRepository<SchemaMeta, TReplicaSetSpec, TReplicaSetStatus> repository;
    static final GaugeRegistry gaugeRegistry = GaugeRegistry.EMPTY;

    @BeforeEach
    void before() throws Exception {
        ypClient = createClient(builder -> {});
        cleanup(ypClient, REPLICA_SET);
        cleanup(ypClient, STAGE);
        repository = new LabelBasedRepository<>(REPLICA_SET, FILTER_LABELS, Optional.empty(), ypClient, DESCRIPTOR, PAGE_SIZE, gaugeRegistry);
    }

    @Test
    void selectObjectsWithLabels() {
        TestYpRepositoryUtils.createReplicaSet(ypClient, "id1", SPEC, FILTER_LABELS);
        TestYpRepositoryUtils.createReplicaSet(ypClient, "id2", SPEC, emptyMap());
        Map<String, Try<YpObject<SchemaMeta, TReplicaSetSpec, TReplicaSetStatus>>> result =
                get5s(repository.selectObjects(SPEC_META_STATUS_SELECTOR, emptyMap())).getObjects();
        assertThat(result.keySet(), contains("id1"));
    }

    @Test
    void selectObjectsWithExtraLabels() {
        Map<String, String> labels = new java.util.HashMap<>(FILTER_LABELS);
        labels.put("extra-key", "extra-value");
        labels.put("dummy-key", "dummy-value");

        TestYpRepositoryUtils.createReplicaSet(ypClient, "id1", SPEC, FILTER_LABELS);
        TestYpRepositoryUtils.createReplicaSet(ypClient, "id2", SPEC, labels);
        Map<String, Try<YpObject<SchemaMeta, TReplicaSetSpec, TReplicaSetStatus>>> result =
                get5s(repository.selectObjects(SPEC_META_STATUS_SELECTOR, Map.of("extra-key", "extra-value")))
                        .getObjects();
        assertThat(result.keySet(), contains("id2"));
    }

    @Test
    void listAllIds() {
        TestYpRepositoryUtils.createReplicaSet(ypClient, "id1", SPEC, FILTER_LABELS);
        TestYpRepositoryUtils.createReplicaSet(ypClient, "id2", SPEC, emptyMap());
        Set<String> result = get5s(repository.listAllIds());
        assertThat(result, containsInAnyOrder("id1", "id2"));
    }

    @Test
    void listAllIdsWithFixedTimestamp() {
        TestYpRepositoryUtils.createReplicaSet(ypClient, "id1", SPEC, FILTER_LABELS);
        TestYpRepositoryUtils.createReplicaSet(ypClient, "id2", SPEC, emptyMap());
        Long timestamp = get5s(repository.generateTimestamp());
        TestYpRepositoryUtils.createReplicaSet(ypClient, "id3", SPEC, emptyMap());
        TestYpRepositoryUtils.createReplicaSet(ypClient, "id4", SPEC, emptyMap());

        Set<String> result = get5s(repository.listAllIds(timestamp));
        assertThat(result, containsInAnyOrder("id1", "id2"));
    }

    @Test
    void selectEmpty() {
        assertThat(get5s(repository.selectObjects(SPEC_META_STATUS_SELECTOR, emptyMap())).getObjects().keySet(), empty());
    }

    @Test
    void selectExactlyOnePage() {
        selectMultipleObjectsTestTemplate(PAGE_SIZE);
    }

    @Test
    void selectMultiplePages() {
        selectMultipleObjectsTestTemplate(2 * PAGE_SIZE + 1);
    }

    @Test
    void watchEmpty() throws ExecutionException, InterruptedException {
        Long startTimestamp = get1s(ypClient.generateTimestamp());
        assertThat(repository.watchObjects(startTimestamp, WATCH_TIME_LIMIT).get().getEvents().keySet(), empty());
    }

    @Test
    void watchExactlyOnePage() {
        watchMultipleObjectsTestTemplate(PAGE_SIZE);
    }

    @Test
    void watchMultiplePages() {
        watchMultipleObjectsTestTemplate(2 * PAGE_SIZE + 1);
    }

    @Test
    void watchObjectsFewTimes() throws ExecutionException, InterruptedException {
        int count  = 2 * PAGE_SIZE + 1;
        Long newStartTimestamp = watchMultipleObjectsTestTemplate(count);

        for (int i = 0; i < count; ++i) {
            get5s(repository.updateObjectSpec("id" + i, UPDATED_SPEC));
        }
        get5s(repository.removeObject("id" + 0));

        WatchedObjects watchedObjects = repository.watchObjects(newStartTimestamp, WATCH_TIME_LIMIT).get();
        assertThat(watchedObjects.getEvents().keySet(), hasSize(count));
        watchedObjects.getEvents().values().stream()
                .filter(events -> events.size() == 1)
                .forEach(events -> {
                    assertThat(events.get(0).getType(), equalTo(YpEventType.UPDATED));
                });
        watchedObjects.getEvents().entrySet().stream()
                .filter(entry -> entry.getValue().size() == 2)
                .forEach(entry -> {
                    assertThat(entry.getValue().get(0).getType(), equalTo(YpEventType.UPDATED));
                    assertThat(entry.getValue().get(1).getType(), equalTo(YpEventType.REMOVED));
                    assertThat(entry.getKey(), equalTo("id" + 0));
                });
    }

    @Test
    void emptyOptionalForNonExistentObject() {
        assertThat(get5s(repository.getObject(ID, SPEC_META_STATUS_SELECTOR)), emptyOptional());
    }

    @Test
    void keepLabelsWhenUpdatingObject() {
        String key = "some_label";
        String value = "some_value";
        TestYpRepositoryUtils.createReplicaSet(ypClient, ID, SPEC, ImmutableMap.of(key, value));
        get5s(repository.updateObjectSpec(ID, UPDATED_SPEC));
        assertThat(getLabelsYson(ypClient, ID, REPLICA_SET).getString(key), equalTo(value));
    }

    @Test
    void saveVcsLabelsOnCreate() {
        repository = vcsInfoRepository();
        get5s(repository.createObject(ID, new CreateObjectRequest.Builder<>(SPEC).build()));
        checkVcsLabels();
    }

    @Test
    void saveVcsLabelsOnUpdate() {
        get5s(repository.createObject(ID, new CreateObjectRequest.Builder<>(SPEC).build()));
        repository = vcsInfoRepository();
        get5s(repository.updateObject(ID, new UpdateYpObjectRequest.Builder<TReplicaSetSpec, TReplicaSetStatus>().setSpec(SPEC).build()));
        checkVcsLabels();
    }

    @Test
    void getAnnotations() {
        String annotationKey = "annotation-key";
        String annotationValue = "annotation-value";
        Map<String, ?> annotations = Map.of(annotationKey, annotationValue);

        LabelBasedRepository<SchemaMeta, TReplicaSetSpec, TReplicaSetStatus> annotationsRepository =
                new LabelBasedRepository<>(REPLICA_SET, FILTER_LABELS, Optional.empty(), ypClient, DESCRIPTOR, PAGE_SIZE, gaugeRegistry);

        Selector projectSelector = new Selector.Builder().withAnnotations(ImmutableSet.of(annotationKey)).build();
        create(ypClient, REPLICA_SET, ID, SPEC, FILTER_LABELS, annotations, Collections.emptyMap());
        Optional<YpObject<SchemaMeta, TReplicaSetSpec, TReplicaSetStatus>> object =
                get5s(annotationsRepository.getObject(ID, projectSelector));
        assertThat(object, optionalWithValue());
        assertThat(object.get().getAnnotations(), hasEntry(annotationKey, annotationValue));
    }

    @Test
    void getEmptyAnnotations() {
        LabelBasedRepository<SchemaMeta, TReplicaSetSpec, TReplicaSetStatus> annotationsRepository =
                new LabelBasedRepository<>(REPLICA_SET, FILTER_LABELS, Optional.empty(), ypClient, DESCRIPTOR, PAGE_SIZE, gaugeRegistry);

        String notExistAnnotation = "not-exist-annotation";
        Selector projectSelector = new Selector.Builder().withAnnotations(ImmutableSet.of(notExistAnnotation)).build();
        TestYpRepositoryUtils.createReplicaSet(ypClient, ID, SPEC, FILTER_LABELS);
        Optional<YpObject<SchemaMeta, TReplicaSetSpec, TReplicaSetStatus>> object =
                get5s(annotationsRepository.getObject(ID, projectSelector));
        assertThat(object, optionalWithValue());
        assertThat(object.get().getAnnotations(), not(hasKey(notExistAnnotation)));
    }

    @Test
    void getObjectsTest() throws ExecutionException, InterruptedException {
        LabelBasedRepository<SchemaMeta, TReplicaSetSpec, TReplicaSetStatus> repository =
                new LabelBasedRepository<>(REPLICA_SET, FILTER_LABELS, Optional.empty(), ypClient, DESCRIPTOR, PAGE_SIZE, gaugeRegistry);

        long count = 3;
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < count; ++i) {
            ids.add("id" + i);
            TestYpRepositoryUtils.createReplicaSet(ypClient, "id" + i, SPEC, FILTER_LABELS);
        }
        List<Optional<YpObject<SchemaMeta, TReplicaSetSpec, TReplicaSetStatus>>> objects =
                repository.getObjects(ids, SPEC_SELECTOR).get();
        assertThat(objects.stream()
                .filter(Optional::isPresent)
                .filter(object -> object.get().getSpec().equals(SPEC))
                .count(), equalTo(count));
    }

    @Test
    void getObjectsWhenIdDoesNotExistTest() throws ExecutionException, InterruptedException {
        LabelBasedRepository<SchemaMeta, TReplicaSetSpec, TReplicaSetStatus> repository =
                new LabelBasedRepository<>(REPLICA_SET, FILTER_LABELS, Optional.empty(), ypClient, DESCRIPTOR, PAGE_SIZE, gaugeRegistry);

        long count = 3;
        List<String> ids = new ArrayList<>();
        for (long i = 0; i < count; ++i) {
            ids.add("id" + i);
            TestYpRepositoryUtils.createReplicaSet(ypClient, "id" + i, SPEC, FILTER_LABELS);
        }
        ids.add("nonexistent");
        for (long i = count; i < count*2; ++i) {
            ids.add("id" + i);
            TestYpRepositoryUtils.createReplicaSet(ypClient, "id" + i, SPEC, FILTER_LABELS);
        }
        ids.add("nonexistent2");

        List<Optional<YpObject<SchemaMeta, TReplicaSetSpec, TReplicaSetStatus>>> objects =
                repository.getObjects(ids, SPEC_SELECTOR).get();

        assertThat(objects.stream()
                .filter(Optional::isPresent)
                .filter(object -> object.get().getSpec().equals(SPEC))
                .count(), equalTo(count * 2));
        assertThat(objects.stream()
                .filter(Optional::isEmpty)
                .count(), equalTo(2L));
    }

    @Test
    void transactionalConflictOperationsTest() throws ExecutionException, InterruptedException {
        TestYpRepositoryUtils.createReplicaSet(ypClient, ID, SPEC, FILTER_LABELS);

        YpTransactionClient transactionClient = new YpTransactionClientImpl(ypClient);
        YpTransaction firstTransaction = transactionClient.startTransaction().get();
        YpTransaction secondTransaction = transactionClient.startTransaction().get();

        get5s(repository.updateObject(ID, firstTransaction,
                new UpdateYpObjectRequest.Builder<TReplicaSetSpec, TReplicaSetStatus>()
                        .setSpec(createSimpleReplicaSetSpec(1))
                        .build()));

        get5s(repository.updateObject(ID, secondTransaction,
                new UpdateYpObjectRequest.Builder<TReplicaSetSpec, TReplicaSetStatus>()
                        .setSpec(createSimpleReplicaSetSpec(2))
                        .build()));

        get5s(transactionClient.commitTransaction(firstTransaction));
        assertThrows(Exception.class, () -> get5s(transactionClient.commitTransaction(secondTransaction)));

        assertThat(get5s(repository.getObject(ID, SPEC_SELECTOR)).get().getSpec(),
                equalTo(createSimpleReplicaSetSpec(1)));
    }

    @Test
    void transactionalOperationsTest() throws ExecutionException, InterruptedException {
        TestYpRepositoryUtils.createReplicaSet(ypClient, "id1", SPEC, FILTER_LABELS);
        TestYpRepositoryUtils.createReplicaSet(ypClient, "id2", SPEC, FILTER_LABELS);

        YpTransactionClient transactionClient = new YpTransactionClientImpl(ypClient);
        YpTransaction transaction = transactionClient.startTransaction().get();

        TReplicaSetSpec replicaSetSpec = createSimpleReplicaSetSpec(10);

        get5s(repository.updateObject("id1", transaction,
                new UpdateYpObjectRequest.Builder<TReplicaSetSpec, TReplicaSetStatus>()
                        .setSpec(replicaSetSpec)
                        .build()));

        get5s(repository.updateObject("id2", transaction,
                new UpdateYpObjectRequest.Builder<TReplicaSetSpec, TReplicaSetStatus>()
                        .setSpec(replicaSetSpec)
                        .build()));

        get5s(transactionClient.commitTransaction(transaction));

        YpObject<SchemaMeta, TReplicaSetSpec, TReplicaSetStatus> firstObject =
                get5s(repository.getObject("id1", SPEC_SELECTOR)).get();
        YpObject<SchemaMeta, TReplicaSetSpec, TReplicaSetStatus> secondObject =
                get5s(repository.getObject("id2", SPEC_SELECTOR)).get();

        assertThat(firstObject.getSpecTimestamp(), equalTo(secondObject.getSpecTimestamp()));
        assertThat(firstObject.getSpec(), equalTo(replicaSetSpec));
        assertThat(secondObject.getSpec(), equalTo(replicaSetSpec));
    }

    @Test
    void timestampPrerequisitesTest() {
        TestYpRepositoryUtils.createReplicaSet(ypClient, "id1", SPEC, FILTER_LABELS);
        YpObject<SchemaMeta, TReplicaSetSpec, TReplicaSetStatus> object =
                get5s(repository.getObject("id1", SPEC_SELECTOR)).get();
        long timestamp = object.getSpecTimestamp();

        get5s(repository.updateObject("id1",
                new UpdateYpObjectRequest.Builder<TReplicaSetSpec, TReplicaSetStatus>()
                        .setSpec(createSimpleReplicaSetSpec(10))
                        .addPrerequisite(new YpAttributeTimestampPrerequisite("/spec", timestamp))
                        .build()));

        assertThrows(Exception.class, () -> get5s(repository.updateObject("id1",
                new UpdateYpObjectRequest.Builder<TReplicaSetSpec, TReplicaSetStatus>()
                        .setSpec(createSimpleReplicaSetSpec(11))
                        .addPrerequisite(new YpAttributeTimestampPrerequisite("/spec", timestamp))
                        .build())));

        object = get5s(repository.getObject("id1", SPEC_SELECTOR)).get();
        assertThat(object.getSpec(), equalTo(createSimpleReplicaSetSpec(10)));
    }

    @Test
    void selectAndWatchObjectsNotFittingPage() {
        int messageSizeLimit = 1000000;
        int pageSize = 5;
        ypClient = createClient(builder -> builder.setMaxInboundMessageSize(messageSizeLimit));
        repository = new LabelBasedRepository<>(REPLICA_SET, emptyMap(), Optional.empty(), ypClient, DESCRIPTOR, pageSize, gaugeRegistry);

        Long startTimestamp = get1s(ypClient.generateTimestamp());

        for (int i = 0; i < pageSize; ++i) {
            TReplicaSetSpec spec = createSimpleReplicaSetSpec(i).toBuilder()
                    .setRevisionId(Strings.repeat("a", messageSizeLimit / 2))
                    .build();
            get5s(repository.createObject(String.valueOf(i), new CreateObjectRequest.Builder<>(spec).build()));
        }

        assertThrows(Exception.class,
                () -> get5s(ypClient.selectObjects(YpSelectStatement.builder(REPLICA_SET, YpPayloadFormat.YSON)
                        .addSelector(Paths.SPEC)
                        .build(), value -> value)));
        Map<String, Try<YpObject<SchemaMeta, TReplicaSetSpec, TReplicaSetStatus>>> objects =
                get5s(repository.selectObjects(new Selector.Builder().build(), emptyMap())).getObjects();
        assertThat(objects.keySet(), hasSize(pageSize));

        WatchedObjects watchedObjects = get5s(repository.watchObjects(startTimestamp, WATCH_TIME_LIMIT));
        assertThat(watchedObjects.getEvents().keySet(), hasSize(pageSize));
    }

    @Test
    void getMetaAndSpecTimestamp() {
        TestYpRepositoryUtils.createReplicaSet(ypClient, ID, SPEC, FILTER_LABELS);

        TReplicaSetSpec replicaSetSpec = createSimpleReplicaSetSpec(10);
        get5s(repository.updateObject(ID,
                new UpdateYpObjectRequest.Builder<TReplicaSetSpec, TReplicaSetStatus>()
                        .setSpec(replicaSetSpec)
                        .build()));
        get5s(repository.updateObject(ID,
                new UpdateYpObjectRequest.Builder<TReplicaSetSpec, TReplicaSetStatus>()
                        .setAcl(new Acl(List.of(AccessControl.TAccessControlEntry.newBuilder()
                                .addPermissions(AccessControl.EAccessControlPermission.ACA_WRITE)
                                .addSubjects("root")
                                .setAction(AccessControl.EAccessControlAction.ACA_ALLOW)
                                .build())))
                        .build()));

        YpObject<SchemaMeta, TReplicaSetSpec, TReplicaSetStatus> object =
                get5s(repository.getObject(ID, SPEC_META_STATUS_SELECTOR)).get();

        assertThat(object.getSpecTimestamp(), lessThan(object.getMetaTimestamp()));

        TReplicaSetSpec replicaSetSpec2 = createSimpleReplicaSetSpec(100);
        get5s(repository.updateObject(ID,
                new UpdateYpObjectRequest.Builder<TReplicaSetSpec, TReplicaSetStatus>()
                        .setSpec(replicaSetSpec2)
                        .setAcl(new Acl(List.of(AccessControl.TAccessControlEntry.newBuilder()
                                .addPermissions(AccessControl.EAccessControlPermission.ACP_READ)
                                .addSubjects("root")
                                .setAction(AccessControl.EAccessControlAction.ACA_ALLOW)
                                .build())))
                        .build()));

        object = get5s(repository.getObject(ID, SPEC_META_STATUS_SELECTOR)).get();

        assertThat(object.getSpecTimestamp(), equalTo(object.getMetaTimestamp()));
    }

    @Test
    void stageUpdateShouldtAddDeployEngineLabel() {
        TStageSpec stageSpec = TStageSpec.newBuilder()
                .setAccountId("tmp")
                .build();
        Map<String, String> meta = new java.util.HashMap<>();
        meta.put("project_id", "test_proj");
        createProject("test_proj");
        TestYpRepositoryUtils.create(ypClient, STAGE, ID, stageSpec, Collections.emptyMap(), Collections.emptyMap(), meta);

        LabelBasedRepository<SchemaMeta, TStageSpec, TStageStatus> stageRepository = new LabelBasedRepository<>
                (STAGE, FILTER_LABELS, Optional.empty(), ypClient, DESCRIPTOR, PAGE_SIZE, gaugeRegistry);

        TStageSpec updatedStageSpec = TStageSpec.newBuilder()
                .setAccountId("tmp")
                .setRevision(2)
                .build();

        Map<String, YTreeNode> requestLabels = new java.util.HashMap<>();
        String epochValue = "1111";
        requestLabels.put("epoch", YTree.builder().value(epochValue).build());

        get5s(stageRepository.updateObject(ID, new UpdateYpObjectRequest.Builder<TStageSpec,TStageStatus>()
                                                                        .setSpec(updatedStageSpec)
                                                                        .setLabels(requestLabels)
                                                                        .build()));
        YTreeMapNode labels = getLabelsYson(ypClient, ID, STAGE);
        assertThat(labels.keys().size(), equalTo(1));
        assertThat(labels.getString("deploy_engine"), equalTo("env_controller"));

        YTreeMapNode annotations = getAnnotationsYson(ypClient, ID, STAGE);
        assertThat(annotations.getString("epoch"), equalTo(epochValue));
    }

    private void createProject(String id) {
        TProjectSpec spec = TProjectSpec.newBuilder()
                .setAccountId("tmp")
                .build();

        TestYpRepositoryUtils.create(ypClient, YpObjectType.PROJECT, id, spec, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
    }

    private static YpRawObjectService createClient(Consumer<YpRawClientBuilder> customizer) {
        HostAndPort masterHostPort = HostAndPort.fromString(System.getenv("YP_MASTER_GRPC_INSECURE_ADDR"));
        YpInstance masterInstance = new YpInstance(masterHostPort.getHost(), masterHostPort.getPort());
        YpRawClientBuilder builder = new YpRawClientBuilder(masterInstance, () -> "fake_token")
                .setUseMasterDiscovery(false)
                .setUsePlaintext(true);
        customizer.accept(builder);
        return builder.build().objectService();
    }

    private static TReplicaSetSpec createSimpleReplicaSetSpec(int replicaCount) {
        return TReplicaSetSpec.newBuilder().setAccountId("tmp").setReplicaCount(replicaCount).build();
    }

    private LabelBasedRepository<SchemaMeta, TReplicaSetSpec, TReplicaSetStatus> vcsInfoRepository() {
        return new LabelBasedRepository<>(REPLICA_SET, emptyMap(), Optional.of(VCS_LABEL), ypClient, DESCRIPTOR, PAGE_SIZE, gaugeRegistry);
    }

    private void checkVcsLabels() {
        VcsVersion version = new VcsVersion(repository.getClass());
        YTreeMapNode vcsLabels = getAnnotationsYson(ypClient, ID, REPLICA_SET).getMap(VCS_LABEL);
        assertThat(vcsLabels.getInt("svn_revision"), equalTo(version.getProgramSvnRevision()));
        assertThat(vcsLabels.getString("svn_branch"), equalTo(version.getBranch()));
        assertThat(vcsLabels.getString("svn_tag"), equalTo(version.getTag()));
    }

    private void selectMultipleObjectsTestTemplate(int count) {
        for (int i = 0; i < count; ++i) {
            TestYpRepositoryUtils.createReplicaSet(ypClient, "id" + i, SPEC, FILTER_LABELS);
        }
        assertThat(get5s(repository.selectObjects(SPEC_META_STATUS_SELECTOR, emptyMap())).getObjects().keySet(),
                hasSize(count));
    }

    private Long watchMultipleObjectsTestTemplate(int count) {
        Long startTimestamp = get1s(ypClient.generateTimestamp());
        for (int i = 0; i < count; ++i) {
            TestYpRepositoryUtils.createReplicaSet(ypClient, "id" + i, SPEC, FILTER_LABELS);
        }
        WatchedObjects watchedObjects = get5s(repository.watchObjects(startTimestamp, WATCH_TIME_LIMIT));
        assertThat(watchedObjects.getEvents().keySet(), hasSize(count));
        watchedObjects.getEvents().values()
                .forEach(events -> {
                    assertThat(events, hasSize(1));
                    assertThat(events.get(0).getType(), equalTo(YpEventType.CREATED));
                });

        return watchedObjects.getTimestamp();
    }
}
