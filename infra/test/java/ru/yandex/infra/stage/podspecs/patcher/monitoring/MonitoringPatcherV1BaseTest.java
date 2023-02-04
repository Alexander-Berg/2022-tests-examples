package ru.yandex.infra.stage.podspecs.patcher.monitoring;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.infra.stage.podspecs.PatcherTestUtils;
import ru.yandex.infra.stage.podspecs.patcher.PatcherTestBase;
import ru.yandex.infra.stage.util.StringUtils;
import ru.yandex.yp.client.api.DataModel;
import ru.yandex.yp.client.api.TMonitoringInfo;
import ru.yandex.yp.client.api.TMonitoringUnistatEndpoint;
import ru.yandex.yp.client.api.TMonitoringWorkloadEndpoint;
import ru.yandex.yp.client.api.TPodAgentMonitoringSpec;
import ru.yandex.yp.client.api.TPodTemplateSpec;
import ru.yandex.yp.client.pods.TBox;
import ru.yandex.yp.client.pods.TWorkload;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.infra.stage.TestData.DEFAULT_UNIT_CONTEXT;
import static ru.yandex.infra.stage.podspecs.patcher.monitoring.MonitoringPatcherUtils.WORKLOAD_KEY;
import static ru.yandex.infra.stage.podspecs.patcher.monitoring.MonitoringPatcherV1BaseTest.LabelOwner.POD;
import static ru.yandex.infra.stage.podspecs.patcher.monitoring.MonitoringPatcherV1BaseTest.LabelOwner.UNISTAT;
import static ru.yandex.infra.stage.podspecs.patcher.monitoring.MonitoringPatcherV1BaseTest.LabelOwner.WORKLOAD;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatContains;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;

abstract class MonitoringPatcherV1BaseTest extends PatcherTestBase<MonitoringPatcherV1Context> {
    private static final MonitoringPatcherV1Context DEFAULT_PATCHER_CONTEXT = new MonitoringPatcherV1Context();
    private static final Map<String, String> USER_LABELS = ImmutableMap.of("key", "value");
    private static final Map<String, String> USER_POD_LABELS = ImmutableMap.of(
            MonitoringPatcherUtils.ITYPE_KEY, "some_user_itype",
            MonitoringPatcherUtils.PRJ_KEY, "prj",
            "user_label_key", "user_label_value");

    private static final Map<String, String> USER_POD_LABELS_WITHOUT_ITYPE_AND_PRJ = ImmutableMap.of(
            "user_label_key", "user_label_value");

    private static final String WORKLOAD_ID = "workload1";
    private static final String BOX_ID = "box1";
    private static final String LABEL_DELIMITER = "_";

    enum LabelOwner {
        POD, WORKLOAD, UNISTAT
    }

    protected abstract boolean shouldAddEnvVars();

    @Test
    void boxEnvVarsTest() {
        var specBuilder = createSpecBuilderWithWorkload();
        var podAgentSpec = specBuilder.getSpecBuilder().getPodAgentPayloadBuilder().getSpecBuilder();
        podAgentSpec
                .addBoxes(TBox.newBuilder().setId(BOX_ID));

        MonitoringPatcherTestBuilder.createInstance(specBuilder, this)
                .putLabels("deployUnitLabel1", "duValue1")
                .patch();

        boolean varsShouldPresent = shouldAddEnvVars();

        TBox box = PatcherTestUtils.groupById(podAgentSpec.getBoxesList(), TBox::getId).get(BOX_ID);
        var boxVars = PatcherTestUtils.getAllLiteralVars(box.getEnvList());

        assertThatEquals(!boxVars.isEmpty(), varsShouldPresent);

        if (varsShouldPresent) {
            var allVarsStartsWithPrefix =
                    boxVars.keySet().stream().allMatch(name -> name.startsWith(MonitoringPatcherUtils.ENV_VARS_PREFIX + "_"));
            assertThat(allVarsStartsWithPrefix, equalTo(true));
            assertThat(boxVars.get("YASM_DEPLOYUNITLABEL1"), equalTo("duValue1"));
        }
    }


    @Test
    void workloadEnvVarsTest() {
        var specBuilder = createSpecBuilderWithWorkload();
        var podAgentSpec = specBuilder.getSpecBuilder().getPodAgentPayloadBuilder().getSpecBuilder();

        var endpoint = TMonitoringUnistatEndpoint.newBuilder()
                .setWorkloadId(WORKLOAD_ID)
                .setPath("/unistat1")
                .setPort(8081)
                .putLabels("workloadLabel1", "workloadValue1")
                .build();

        MonitoringPatcherTestBuilder.createInstance(specBuilder, this)
                .addUnistats(endpoint)
                .patch();

        boolean varsShouldPresent = shouldAddEnvVars();

        TWorkload workload =
                PatcherTestUtils.groupById(podAgentSpec.getWorkloadsList(), TWorkload::getId).get(WORKLOAD_ID);
        var workloadVars = PatcherTestUtils.getAllLiteralVars(workload.getEnvList());
        assertThatEquals(!workloadVars.isEmpty(), varsShouldPresent);

        if (varsShouldPresent) {
            var allVarsStartsWithPrefix =
                    workloadVars.keySet().stream().allMatch(name -> name.startsWith(MonitoringPatcherUtils.ENV_VARS_PREFIX + "_"));
            assertThat(allVarsStartsWithPrefix, equalTo(true));
            assertThat(workloadVars.get("YASM_WORKLOADLABEL1"), equalTo("workloadValue1"));
        }
    }

    @Test
    void setPodDefaultLabelsTest() {
        String expectedPodPrj = DEFAULT_UNIT_CONTEXT.getFullDeployUnitId() + StringUtils.ID_SEPARATOR + WORKLOAD_ID;
        checkDefaultPodLabels(createTestWithWorkload(), expectedPodPrj);
    }

    @Test
    void setPodPrjUnknownTest() {
        String expectedPodPrj = MonitoringPatcherV1Base.PRJ_UNKNOWN;
        checkDefaultPodLabels(createTest(), expectedPodPrj);
    }

    private void checkDefaultPodLabels(MonitoringPatcherTestBuilder testBuilder, String expectedPodPrj) {
        var monitoringInfo = testBuilder.patch();
        assertThatPodLabelsAreCorrect(monitoringInfo, MonitoringPatcherUtils.ITYPE_DEPLOY, expectedPodPrj);
    }

    protected static Stream<Arguments> setWorkloadLabelInUnistatTestParametersGenerator(
            boolean mustContainWorkloadLabelIfWasAbsent
    ) {
        return Stream.of(
                Arguments.of(
                        TMonitoringUnistatEndpoint.newBuilder()
                                .putLabels(WORKLOAD_KEY, WORKLOAD_ID)
                                .setWorkloadId(WORKLOAD_ID)
                                .build(),
                        true
                ),
                Arguments.of(
                        TMonitoringUnistatEndpoint.newBuilder()
                                .setWorkloadId(WORKLOAD_ID)
                                .build(),
                        mustContainWorkloadLabelIfWasAbsent
                )
        );
    }

    protected void setWorkloadLabelInUnistatScenario(TMonitoringUnistatEndpoint unistatEndpoint,
                                       boolean expectedUnistatContainCorrectWorkloadLabel) {
        MonitoringPatcherTestBuilder testWithWorkload = createTestWithWorkload();
        testWithWorkload.addUnistats(unistatEndpoint);
        var monitoringInfo = testWithWorkload.patch();
        boolean actualUnistatContainCorrectWorkloadLabel = monitoringInfo.getUnistatsList()
                .stream()
                .filter(unistat -> unistatEndpoint.getWorkloadId().equals(unistat.getWorkloadId()))
                .allMatch(unistat -> unistat.containsLabels(WORKLOAD_KEY));
        assertThat(actualUnistatContainCorrectWorkloadLabel, equalTo(expectedUnistatContainCorrectWorkloadLabel));
    }

    @Test
    void keepUserPodLabelsTest() {
        String usedItype = "user_itype";
        String usedPrj = "user_prj";

        var monitoringInfo = createTestWithWorkload()
                .putLabels(MonitoringPatcherUtils.ITYPE_KEY, usedItype)
                .putLabels(MonitoringPatcherUtils.PRJ_KEY, usedPrj)
                .patch();

        assertThatPodLabelsAreCorrect(monitoringInfo, usedItype, usedPrj);
    }

    @Test
    void addUserWorkloadEndpointTest() {
        var endpoint = TMonitoringWorkloadEndpoint.newBuilder()
                .setWorkloadId(WORKLOAD_ID)
                .build();

        var expectedEndpoint = TMonitoringWorkloadEndpoint.newBuilder(endpoint)
                .build();

        var monitoringInfo = createTestWithWorkload()
                .addWorkload(endpoint)
                .patch();

        assertThatPodContainsWorkloadEndpoints(monitoringInfo, expectedEndpoint);
    }

    @Test
    void addUserUnistatEndpointPathTest() {
        String unistatPath = "/unistat1";
        var endpointBuilder = TMonitoringUnistatEndpoint.newBuilder()
                .setWorkloadId(WORKLOAD_ID)
                .setPath(unistatPath);
        addUserUnistatEndpointInfoScenario(endpointBuilder);
    }

    @Test
    void addUserUnistatEndpointPortTest() {
        int unistatPort = 8081;
        var endpointBuilder = TMonitoringUnistatEndpoint.newBuilder()
                .setWorkloadId(WORKLOAD_ID)
                .setPort(unistatPort);
        addUserUnistatEndpointInfoScenario(endpointBuilder);
    }

    private void addUserUnistatEndpointInfoScenario(TMonitoringUnistatEndpoint.Builder endpointBuilder) {
        var monitoringInfo = createTestWithWorkload()
                .addUnistats(endpointBuilder.build())
                .patch();

        TMonitoringUnistatEndpoint expectedEndpoint = mustContainWorkloadKey()
                ? endpointBuilder.putLabels(WORKLOAD_KEY, WORKLOAD_ID).build()
                : endpointBuilder.build();
        assertThatPodContainsUnistatEndpoints(monitoringInfo, expectedEndpoint, createDefaultUnistatEndpoint());
    }

    abstract boolean mustContainWorkloadKey();

    @Test
    void addPodAgentUnistatEndpointTest() {
        var monitoringInfo = createTest()
                .patch();

        assertThatPodContainsUnistatEndpoints(monitoringInfo, createDefaultUnistatEndpoint());
    }

    @Test
    void addPodAgentUnistatUserSignalsEndpointTest() {
        var monitoringInfo = createTest()
                .addPodAgentMonitoring(TPodAgentMonitoringSpec.newBuilder()
                        .setAddPodAgentUserSignals(true)
                        .putAllLabels(USER_POD_LABELS)
                        .build())
                .patch();

        assertThatPodContainsUnistatEndpoints(monitoringInfo, createDefaultUnistatEndpoint(),
                createUserUnistatEndpoint(USER_POD_LABELS));
    }

    @Test
    void addPodAgentUnistatUserSignalsEndpointWhenUserLabelsWithoutItypeAndPrjTest() {
        var monitoringInfo = createTest()
                .addPodAgentMonitoring(TPodAgentMonitoringSpec.newBuilder()
                        .setAddPodAgentUserSignals(true)
                        .putAllLabels(USER_POD_LABELS_WITHOUT_ITYPE_AND_PRJ)
                        .build())
                .patch();

        Map<String, String> expectedUserUnistatLabels = new HashMap<>(USER_POD_LABELS_WITHOUT_ITYPE_AND_PRJ);
        expectedUserUnistatLabels.put(MonitoringPatcherUtils.PRJ_KEY, DEFAULT_UNIT_CONTEXT.getFullDeployUnitId());
        expectedUserUnistatLabels.put(MonitoringPatcherUtils.ITYPE_KEY, MonitoringPatcherUtils.ITYPE_POD_AGENT);

        assertThatPodContainsUnistatEndpoints(monitoringInfo, createDefaultUnistatEndpoint(),
                createUserUnistatEndpoint(expectedUserUnistatLabels));
    }

    @Test
    void addPodAgentUnistatDisabledUserSignalsEndpointTest() {
        var monitoringInfo = createTest()
                .addPodAgentMonitoring(TPodAgentMonitoringSpec.newBuilder()
                        .setAddPodAgentUserSignals(false)
                        .putAllLabels(USER_POD_LABELS)
                        .build())
                .patch();

        assertThatPodContainsUnistatEndpoints(monitoringInfo, createDefaultUnistatEndpoint());
    }

    enum TagsInheritanceState {
        WITH_INHERITANCE, WITHOUT_INHERITANCE
    }

    private static Iterable<Arguments> provideParametersForCheckInheritance() {
        var argumentsCollectionBuilder = ImmutableList.<Arguments>builder();

        for (TagsInheritanceState workloadState : TagsInheritanceState.values()) {
            for (TagsInheritanceState unistatState : TagsInheritanceState.values()) {
                argumentsCollectionBuilder.add(
                        Arguments.of(workloadState, unistatState)
                );
            }
        }

        return argumentsCollectionBuilder.build();
    }

    @ParameterizedTest
    @MethodSource("provideParametersForCheckInheritance")
    public void checkInheritanceTest(TagsInheritanceState workloadState, TagsInheritanceState unistatState) {
        var initialWorkloadLabels = InheritanceTestUtils.generateLabels(WORKLOAD);
        var initialUnistatLabels = InheritanceTestUtils.generateLabels(UNISTAT);

        var testBuilder = createTestWithWorkload()
                .putAllLabels(InheritanceTestUtils.generateLabels(POD));

        var workloadEndpoint = TMonitoringWorkloadEndpoint.newBuilder()
                .putAllLabels(initialWorkloadLabels)
                .setInheritMissedLabels(TagsInheritanceState.WITH_INHERITANCE == workloadState)
                .setWorkloadId(WORKLOAD_ID)
                .build();

        testBuilder.addWorkload(workloadEndpoint);

        var unistatEndpoint = TMonitoringUnistatEndpoint.newBuilder()
                .putAllLabels(initialUnistatLabels)
                .setInheritMissedLabels(TagsInheritanceState.WITH_INHERITANCE == unistatState)
                .setWorkloadId(WORKLOAD_ID)
                .build();

        testBuilder.addUnistats(unistatEndpoint);

        var monitoringInfo = testBuilder.patch();

        var patchedPodLabels = monitoringInfo.getLabelsMap();
        var patchedWorkloadLabels = monitoringInfo.getWorkloadsList().stream()
                .findFirst()
                .map(TMonitoringWorkloadEndpoint::getLabelsMap)
                .orElse(Collections.emptyMap());

        var patchedUnistatLabels = monitoringInfo.getUnistatsList().stream()
                .findFirst()
                .map(TMonitoringUnistatEndpoint::getLabelsMap)
                .orElse(Collections.emptyMap());

        InheritanceTestUtils.assertInheritanceIsCorrect(
                TagsInheritanceState.WITH_INHERITANCE == workloadState,
                patchedPodLabels,
                patchedWorkloadLabels, initialWorkloadLabels);
        InheritanceTestUtils.assertInheritanceIsCorrect(
                TagsInheritanceState.WITH_INHERITANCE == unistatState,
                patchedPodLabels,
                patchedUnistatLabels, initialUnistatLabels);
    }

    private static class InheritanceTestUtils {
        private static final Map<Set<LabelOwner>, String> LABEL_KEYS;

        static {
            LABEL_KEYS = Stream.of(
                    EnumSet.of(UNISTAT), // 001
                    EnumSet.of(WORKLOAD), // 010
                    EnumSet.of(WORKLOAD, UNISTAT), // 011
                    EnumSet.of(POD), // 100
                    EnumSet.of(POD, UNISTAT), // 101
                    EnumSet.of(POD, WORKLOAD), // 110
                    EnumSet.of(POD, WORKLOAD, UNISTAT) // 111
            ).collect(
                    Collectors.toUnmodifiableMap(
                            Function.identity(),
                            InheritanceTestUtils::generateKey
                    )
            );
        }

        private static Map<String, String> generateLabels(LabelOwner labelOwner) {
            return LABEL_KEYS.entrySet().stream()
                    .filter(e -> e.getKey().contains(labelOwner))
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toUnmodifiableMap(
                            Function.identity(),
                            key -> generateValue(key, labelOwner)
                    ));
        }

        private static String generateKey(Set<LabelOwner> labelType) {
            return labelType.stream()
                    .map(Enum::name)
                    .collect(Collectors.joining(LABEL_DELIMITER));
        }

        private static String generateValue(String key, LabelOwner labelOwner) {
            return String.join(LABEL_DELIMITER, labelOwner.name().toLowerCase(), key);
        }

        private static void assertInheritanceIsCorrect(
                boolean withInheritance,
                Map<String, String> patchedParentLabels,
                Map<String, String> patchedChildLabels,
                Map<String, String> initialChildLabels) {
            var expectedChildLabels = new HashMap<>(initialChildLabels);

            if (withInheritance) {
                patchedParentLabels.forEach(expectedChildLabels::putIfAbsent);
            }

            boolean patchedContainsAllExpected = patchedChildLabels.entrySet()
                    .containsAll(expectedChildLabels.entrySet());
            assertThat(patchedContainsAllExpected, equalTo(true));
        }
    }

    private static class MonitoringPatcherTestBuilder {

        public static MonitoringPatcherTestBuilder createInstance(TPodTemplateSpec.Builder builder,
                                                                  MonitoringPatcherV1BaseTest patcherTests) {
            return new MonitoringPatcherTestBuilder(builder, patcherTests)
                    .putAllLabels(USER_LABELS);
        }

        private final TPodTemplateSpec.Builder specBuilder;
        private final TMonitoringInfo.Builder builder;
        private final MonitoringPatcherV1BaseTest patcherTests;

        private MonitoringPatcherTestBuilder(TPodTemplateSpec.Builder specBuilder,
                                             MonitoringPatcherV1BaseTest patcherTests) {
            this.specBuilder = specBuilder;
            this.builder = specBuilder.getSpecBuilder().getHostInfraBuilder().getMonitoringBuilder();
            this.patcherTests = patcherTests;
        }

        public MonitoringPatcherTestBuilder putLabels(String key, String value) {
            builder.putLabels(key, value);
            return this;
        }

        public MonitoringPatcherTestBuilder putAllLabels(Map<String, String> labelsMap) {
            builder.putAllLabels(labelsMap);
            return this;
        }

        public MonitoringPatcherTestBuilder addWorkload(TMonitoringWorkloadEndpoint workloadEndpoint) {
            builder.addWorkloads(workloadEndpoint);
            return this;
        }

        public MonitoringPatcherTestBuilder addUnistats(TMonitoringUnistatEndpoint unistatEndpoint) {
            builder.addUnistats(unistatEndpoint);
            return this;
        }

        public MonitoringPatcherTestBuilder addPodAgentMonitoring(TPodAgentMonitoringSpec podAgentMonitoringSpec) {
            builder.setPodAgent(podAgentMonitoringSpec);
            return this;
        }

        public TMonitoringInfo patch() {
            return patcherTests.patch(DEFAULT_PATCHER_CONTEXT, specBuilder, DEFAULT_UNIT_CONTEXT)
                    .getPodSpec().getHostInfra().getMonitoring();
        }
    }

    private MonitoringPatcherTestBuilder createTest() {
        return MonitoringPatcherTestBuilder.createInstance(TPodTemplateSpec.newBuilder(), this);
    }

    private MonitoringPatcherTestBuilder createTestWithWorkload() {
        var specBuilder = createSpecBuilderWithWorkload();
        return MonitoringPatcherTestBuilder.createInstance(specBuilder, this);
    }

    private static TPodTemplateSpec.Builder createSpecBuilderWithWorkload() {
        TPodTemplateSpec.Builder templateSpec = TPodTemplateSpec.newBuilder();
        DataModel.TPodSpec.Builder podSpec = templateSpec.getSpecBuilder();

        podSpec.getPodAgentPayloadBuilder().getSpecBuilder()
                .addWorkloads(TWorkload.newBuilder().setId(WORKLOAD_ID));
        return templateSpec;
    }

    private static Map<String, String> createPatchedPodDefaultLabelsMap(String itype, String prj) {
        return ImmutableMap.<String, String>builder()
                .putAll(USER_LABELS)
                .putAll(ImmutableMap.of(
                        MonitoringPatcherUtils.ITYPE_KEY, itype,
                        MonitoringPatcherUtils.PRJ_KEY, prj,
                        MonitoringPatcherUtils.STAGE_KEY, DEFAULT_UNIT_CONTEXT.getStageContext().getStageId(),
                        MonitoringPatcherUtils.DEPLOY_UNIT_KEY, DEFAULT_UNIT_CONTEXT.getDeployUnitId()
                ))
                .build();
    }

    private static void assertThatPodLabelsAreCorrect(TMonitoringInfo monitoringInfo, String itype, String prj) {
        assertThatPodLabelsAreCorrect(monitoringInfo, createPatchedPodDefaultLabelsMap(itype, prj));
    }

    private static void assertThatPodLabelsAreCorrect(TMonitoringInfo monitoringInfo,
                                                      Map<String, String> expectedLabelsMap) {
        assertThatEquals(monitoringInfo.getLabelsMap(), expectedLabelsMap);
    }

    private static TMonitoringUnistatEndpoint createDefaultUnistatEndpoint() {
        return TMonitoringUnistatEndpoint.newBuilder()
                .setPort(MonitoringPatcherV1Base.POD_AGENT_UNISTAT_PORT)
                .setPath(MonitoringPatcherV1Base.POD_AGENT_UNISTAT_PATH)
                .setOutputFormat(MonitoringPatcherV1Base.POD_AGENT_OUTPUT_FORMAT)
                .putLabels(MonitoringPatcherUtils.ITYPE_KEY, MonitoringPatcherUtils.ITYPE_POD_AGENT)
                .putLabels(MonitoringPatcherUtils.PRJ_KEY, DEFAULT_UNIT_CONTEXT.getFullDeployUnitId())
                .build();
    }

    private static TMonitoringUnistatEndpoint createUserUnistatEndpoint(Map<String, String> labels) {
        return TMonitoringUnistatEndpoint.newBuilder()
                .setPort(MonitoringPatcherV1Base.POD_AGENT_UNISTAT_PORT)
                .setPath(MonitoringPatcherV1Base.POD_AGENT_UNISTAT_USER_PATH)
                .setOutputFormat(MonitoringPatcherV1Base.POD_AGENT_OUTPUT_FORMAT)
                .putAllLabels(labels)
                .build();
    }

    private static void assertThatPodContainsUnistatEndpoints(TMonitoringInfo monitoringInfo,
                                                              TMonitoringUnistatEndpoint... endPoints) {
        assertThatContains(monitoringInfo.getUnistatsList(), endPoints);
    }

    private static void assertThatPodContainsWorkloadEndpoints(TMonitoringInfo monitoringInfo,
                                                               TMonitoringWorkloadEndpoint... endPoints) {
        assertThatContains(monitoringInfo.getWorkloadsList(), endPoints);
    }
}
