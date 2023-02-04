package ru.yandex.infra.auth.yp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.bolts.collection.Try;
import ru.yandex.infra.auth.Metrics;
import ru.yandex.infra.auth.TreeNode;
import ru.yandex.infra.controller.dto.Acl;
import ru.yandex.infra.controller.dto.NannyServiceMeta;
import ru.yandex.infra.controller.dto.ProjectMeta;
import ru.yandex.infra.controller.dto.SchemaMeta;
import ru.yandex.infra.controller.dto.StageMeta;
import ru.yandex.infra.controller.metrics.GaugeRegistry;
import ru.yandex.infra.controller.yp.DummyYpObjectTransactionalRepository;
import ru.yandex.infra.controller.yp.SelectedObjects;
import ru.yandex.infra.controller.yp.YpObject;
import ru.yandex.infra.controller.yp.YpObjectSettings;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.misc.time.TimeUtils;
import ru.yandex.yp.client.api.AccessControl;
import ru.yandex.yp.client.api.DataModel;
import ru.yandex.yp.client.api.TNannyServiceSpec;
import ru.yandex.yp.client.api.TNannyServiceStatus;
import ru.yandex.yp.client.api.TProjectSpec;
import ru.yandex.yp.client.api.TProjectStatus;
import ru.yandex.yp.client.api.TStageSpec;
import ru.yandex.yp.client.api.TStageStatus;

import static java.util.Collections.emptyMap;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class YpObjectTreeGetterTest {
    public static final AccessControl.TAccessControlEntry ACL_ENTRY = AccessControl.TAccessControlEntry.newBuilder()
            .addSubjects("user")
            .addPermissions(AccessControl.EAccessControlPermission.ACA_CREATE)
            .setAction(AccessControl.EAccessControlAction.ACA_ALLOW)
            .build();
    public static final Acl ACL = new Acl(ImmutableList.of(ACL_ENTRY));
    private static final String STAGE_ID = "stage_id";
    private static final String STAGE_UUID = "e41ae482-87f4ed32-cdb633f0-aba7dcb9";
    private static final String PROJECT_KEY = "project";
    private static final String PROJECT_ID = "myProject";
    private static final String PROJECT_UUID = "537defc8-7349b8cb-38d68549-bd222a54";
    private static final String FIRST_SPECIFIC_BOX = "first-box";
    private static final String SECOND_SPECIFIC_BOX = "second-box";
    private static final List<String> DEFAULT_SPECIFIC_BOXES = List.of(FIRST_SPECIFIC_BOX, SECOND_SPECIFIC_BOX);
    private static final long TIMESTAMP = 0L;
    private static final long NEXT_TIMESTAMP = 1L;

    private static final String USE_UUID_SINCE = "2021-01-01 00:00:00";
    private static final long TIMESTAMP_BEFORE_UUID_ERA = dateToYpTimestamp("2020-01-01 00:00:00");
    private static final long TIMESTAMP_AFTER_UUID_ERA = dateToYpTimestamp("2022-01-01 00:00:00");

    private static final Map<String, YTreeNode> FILTER_LABELS_NODES = Map.of("deploy_engine",
            YTree.stringNode("env_controller"));
    private static final Map<String, String> FILTER_LABELS = Map.of("deploy_engine", "env_controller");

    private static final YpObject<StageMeta, TStageSpec, TStageStatus> STAGE_WITH_META_SPEC_ANNOTATIONS =
            createStageObject(STAGE_ID, PROJECT_ID, FILTER_LABELS_NODES);
    private static final YpObject<ProjectMeta, TProjectSpec, TProjectStatus> PROJECT_WITH_META_SPEC =
            createProjectObject(PROJECT_ID, FILTER_LABELS_NODES);
    private DummyYpObjectTransactionalRepository<StageMeta, TStageSpec, TStageStatus> ypStageRepository;
    private DummyYpObjectTransactionalRepository<NannyServiceMeta, TNannyServiceSpec, TNannyServiceStatus> ypNannyServiceRepository;
    private DummyYpObjectTransactionalRepository<ProjectMeta, TProjectSpec, TProjectStatus> ypProjectRepository;
    private DummyYpObjectTransactionalRepository<SchemaMeta, DataModel.TGroupSpec, DataModel.TGroupStatus> ypGroupRepository;

    private YpObjectsTreeGetterImpl treeGetter;

    private static long dateToYpTimestamp(String date) {
        final DateTime dt = new TimeUtils.DateTimeUtils().parse(date, DateTimeZone.getDefault());
        return dt.getMillis() * 1000;
    }

    private static YpObject<ProjectMeta, TProjectSpec, TProjectStatus> createProjectObject(String id,
                                                                                           Map<String, YTreeNode> labels) {
        return createProjectObject(id, labels, TIMESTAMP_BEFORE_UUID_ERA);
    }

    private static YpObject<ProjectMeta, TProjectSpec, TProjectStatus> createProjectObject(String id,
            Map<String, YTreeNode> labels, long timestamp) {
        return new YpObject.Builder<ProjectMeta, TProjectSpec, TProjectStatus>()
                .setSpecAndTimestamp(TProjectSpec.newBuilder()
                        .setAccountId("abc:service:1")
                        .addAllUserSpecificBoxTypes(DEFAULT_SPECIFIC_BOXES)
                        .build(), 0)
                .setMeta(new ProjectMeta(id, ACL, "", PROJECT_UUID, timestamp, "owner"))
                .setLabels(labels)
                .build();
    }

    private static YpObject<StageMeta, TStageSpec, TStageStatus> createStageObject(String id, String project_id,
            Map<String, YTreeNode> labels, String accountId, long timestamp) {
        return new YpObject.Builder<StageMeta, TStageSpec, TStageStatus>()
                .setSpecAndTimestamp(TStageSpec.newBuilder().setAccountId(accountId).build(), 0)
                .setMeta(new StageMeta(id, ACL, "", STAGE_UUID, timestamp, project_id))
                .setAnnotations(ImmutableMap.of(PROJECT_KEY, PROJECT_ID))
                .setLabels(labels)
                .build();
    }

    private static YpObject<StageMeta, TStageSpec, TStageStatus> createStageObject(String id, String project_id,
                                                                                   Map<String, YTreeNode> labels,
                                                                                   String accountId) {
        return createStageObject(id, project_id, labels, accountId, TIMESTAMP_BEFORE_UUID_ERA);
    }

    private static YpObject<StageMeta, TStageSpec, TStageStatus> createStageObject(String id, String project_id,
            Map<String, YTreeNode> labels) {
        return createStageObject(id, project_id, labels, "abc:service:1");
    }

    @BeforeEach
    void before() {
        ypStageRepository = new DummyYpObjectTransactionalRepository<>();
        ypStageRepository.generateTimestampResponse = completedFuture(TIMESTAMP);
        ypNannyServiceRepository = new DummyYpObjectTransactionalRepository<>();
        ypNannyServiceRepository.generateTimestampResponse = completedFuture(TIMESTAMP);
        ypNannyServiceRepository.selectResponse = completedFuture(new SelectedObjects<>(emptyMap(), TIMESTAMP));
        ypProjectRepository = new DummyYpObjectTransactionalRepository<>();
        ypProjectRepository.generateTimestampResponse = completedFuture(TIMESTAMP);
        ypGroupRepository = new DummyYpObjectTransactionalRepository<>();
        ypGroupRepository.generateTimestampResponse = completedFuture(TIMESTAMP);
        ypGroupRepository.selectResponse = completedFuture(new SelectedObjects<>(Map.of(), TIMESTAMP));
        var ypCacheSettings = Map.of(YpObjectSettings.TYPE_USED_AS_KEY_WITH_DEFAULT_SETTINGS,
                new YpObjectSettings.Builder().setWatches(false).build());

        treeGetter = new YpObjectsTreeGetterImpl(ypStageRepository, ypNannyServiceRepository, ypProjectRepository, ypGroupRepository,
                new Metrics(new MetricRegistry(), 1), USE_UUID_SINCE, ypCacheSettings, GaugeRegistry.EMPTY, false);
    }

    @Test
    void emptyResponse() {
        ypStageRepository.generateTimestampResponse = completedFuture(TIMESTAMP);
        ypStageRepository.selectResponse = completedFuture(new SelectedObjects<>(emptyMap(), TIMESTAMP));
        ypProjectRepository.selectResponse = completedFuture(new SelectedObjects<>(emptyMap(), TIMESTAMP));

        assertDoesNotThrow(() -> {
            TreeNode root = treeGetter.getObjectsTree(false).getTreeNode();
            assertThat(root.getChildren(), empty());
        });
    }

    @Test
    void fullNotEmptyResponse() {
        ypStageRepository.generateTimestampResponse = completedFuture(TIMESTAMP);
        ypStageRepository.selectResponse = completedFuture(
                new SelectedObjects<>(ImmutableMap.of(STAGE_ID, Try.success(STAGE_WITH_META_SPEC_ANNOTATIONS)),
                        TIMESTAMP)
        );
        String projectWithoutStages = "alone-project";
        ypProjectRepository.selectResponse = completedFuture(new SelectedObjects<>(ImmutableMap.of(
                PROJECT_ID, Try.success(PROJECT_WITH_META_SPEC),
                projectWithoutStages, Try.success(createProjectObject(projectWithoutStages, FILTER_LABELS_NODES))),
                TIMESTAMP));

        assertDoesNotThrow(() -> {
            TreeNode root = treeGetter.getObjectsTree(false).getTreeNode();

            assertThat(root.getChildren(), hasSize(2));
            assertThat(root.isConvertibleToRole(), equalTo(true));

            List<TreeNode> projects = root.getChildren();
            assertThat(projects.get(0).getName(), equalTo(PROJECT_ID));
            assertThat(projects.get(0).isConvertibleToRole(), equalTo(true));

            assertThat(projects.get(1).getName(), equalTo(projectWithoutStages));
            assertThat(projects.get(1).isConvertibleToRole(), equalTo(true));
            assertThat(projects.get(1).getChildren(), empty());

            List<TreeNode> stages = projects.get(0).getChildren();
            assertThat(stages.get(0).getName(), equalTo(STAGE_ID));
            assertThat(stages.get(0).isConvertibleToRole(), equalTo(true));

            List<TreeNode> boxes = stages.get(0).getChildren();
            assertThat(boxes, hasSize(2));
            assertThat(boxes.get(0).getName(), equalTo(FIRST_SPECIFIC_BOX));
            assertThat(boxes.get(0).isConvertibleToRole(), equalTo(true));
            assertThat(boxes.get(1).getName(), equalTo(SECOND_SPECIFIC_BOX));
            assertThat(boxes.get(1).isConvertibleToRole(), equalTo(true));
        });
    }

    @Test
    void useUuidSince() {
        ypStageRepository.generateTimestampResponse = completedFuture(TIMESTAMP);
        String stageWithUuid = "newStage";
        ypStageRepository.selectResponse = completedFuture(new SelectedObjects<>(ImmutableMap.of(
                STAGE_ID, Try.success(createStageObject(STAGE_ID, PROJECT_ID, FILTER_LABELS_NODES, "accountId", TIMESTAMP_BEFORE_UUID_ERA)),
                stageWithUuid, Try.success(createStageObject(stageWithUuid, PROJECT_ID, FILTER_LABELS_NODES, "accountId", TIMESTAMP_AFTER_UUID_ERA))),
                TIMESTAMP));

        String projectWithUuid = "newProject";
        ypProjectRepository.selectResponse = completedFuture(new SelectedObjects<>(ImmutableMap.of(
                PROJECT_ID, Try.success(createProjectObject(PROJECT_ID, FILTER_LABELS_NODES, TIMESTAMP_BEFORE_UUID_ERA)),
                projectWithUuid, Try.success(createProjectObject(projectWithUuid, FILTER_LABELS_NODES, TIMESTAMP_AFTER_UUID_ERA))),
                TIMESTAMP));

        assertDoesNotThrow(() -> {
            TreeNode root = treeGetter.getObjectsTree(false).getTreeNode();

            assertThat(root.getChildren(), hasSize(2));
            assertThat(root.isConvertibleToRole(), equalTo(true));

            List<TreeNode> projects = root.getChildren();
            projects.sort(Comparator.comparing(TreeNode::getName));
            assertThat(projects.get(0).getName(), equalTo(PROJECT_ID));
            assertThat(projects.get(0).isConvertibleToRole(), equalTo(true));
            assertThat(projects.get(0).getUniqueId(), equalTo(""));

            assertThat(projects.get(1).getName(), equalTo(projectWithUuid));
            assertThat(projects.get(1).isConvertibleToRole(), equalTo(true));
            assertThat(projects.get(1).getUniqueId(), equalTo(PROJECT_UUID));

            List<TreeNode> stages = projects.get(0).getChildren();
            stages.sort(Comparator.comparing(TreeNode::getName));
            assertThat(stages.get(1).getName(), equalTo(STAGE_ID));
            assertThat(stages.get(1).isConvertibleToRole(), equalTo(true));
            assertThat(stages.get(1).getUniqueId(), equalTo(""));

            assertThat(stages.get(0).getName(), equalTo(stageWithUuid));
            assertThat(stages.get(0).isConvertibleToRole(), equalTo(true));
            assertThat(stages.get(0).getUniqueId(), equalTo(STAGE_UUID));
        });
    }

    @Test
    void getRolesFilteringObjectsByIdTest() {
        String correctSymbols = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
        List<String> correctIds = new ArrayList<>();

        Map<String, Try<YpObject<StageMeta, TStageSpec, TStageStatus>>> stages = new TreeMap<>();
        Map<String, Try<YpObject<ProjectMeta, TProjectSpec, TProjectStatus>>> projects = new TreeMap<>();
        for (char id = 0; id < 128; ++id) {
            String idStr = String.valueOf(id);
            stages.put(idStr, Try.success(createStageObject(idStr, idStr, FILTER_LABELS_NODES)));
            projects.put(idStr, Try.success(createProjectObject(idStr, FILTER_LABELS_NODES)));

            if (correctSymbols.contains(idStr)) {
                correctIds.add(idStr);
            }
        }

        ypStageRepository.generateTimestampResponse = completedFuture(NEXT_TIMESTAMP);
        ypStageRepository.selectResponse = completedFuture(new SelectedObjects<>(stages, TIMESTAMP));
        ypProjectRepository.selectResponse = completedFuture(new SelectedObjects<>(projects, TIMESTAMP));

        assertDoesNotThrow(() -> {
            TreeNode root = treeGetter.getObjectsTree(false).getTreeNode();

            assertThat(root.getChildren(), hasSize(correctIds.size()));
            assertThat(root.isConvertibleToRole(), equalTo(true));

            root.getChildren().forEach(projectNode -> {
                assertThat(correctIds, hasItem(projectNode.getName()));
                assertThat(projectNode.isConvertibleToRole(), equalTo(true));
                assertThat(projectNode.getChildren(), hasSize(1));

                TreeNode stageNode = projectNode.getChildren().get(0);
                assertThat(correctIds, hasItem(projectNode.getName()));

                assertThat(stageNode.isConvertibleToRole(), equalTo(true));

                List<TreeNode> boxes = stageNode.getChildren();
                assertThat(boxes, hasSize(2));
                assertThat(boxes.get(0).getName(), equalTo(FIRST_SPECIFIC_BOX));
                assertThat(boxes.get(0).isConvertibleToRole(), equalTo(true));
                assertThat(boxes.get(1).getName(), equalTo(SECOND_SPECIFIC_BOX));
                assertThat(boxes.get(1).isConvertibleToRole(), equalTo(true));
            });

        });
    }
}
