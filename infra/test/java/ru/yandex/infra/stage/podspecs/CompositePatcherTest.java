package ru.yandex.infra.stage.podspecs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.infra.stage.deployunit.DeployUnitContext;
import ru.yandex.infra.stage.dto.DeployUnitSpec;
import ru.yandex.infra.stage.podspecs.revision.RevisionsHolder;
import ru.yandex.infra.stage.podspecs.revision.RevisionsHolderImpl;
import ru.yandex.infra.stage.podspecs.revision.model.RevisionScheme;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.yp.client.api.TPodTemplateSpec;
import ru.yandex.yt.ytree.TAttribute;

import static ru.yandex.infra.stage.TestData.DEFAULT_UNIT_CONTEXT;
import static ru.yandex.infra.stage.podspecs.PodSpecCompositePatcher.DEPLOY_LABEL_KEY;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatContains;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatThrowsWithMessage;

class CompositePatcherTest {

    private static RevisionsHolder<TPodTemplateSpec.Builder> singleRevisionHolder(List<SpecPatcher<TPodTemplateSpec.Builder>> patchers) {
        return new RevisionsHolder<>() {
            @Override
            public Optional<List<SpecPatcher<TPodTemplateSpec.Builder>>> getPatchersFor(int revisionId) {
                return Optional.of(patchers);
            }

            @Override
            public boolean containsRevision(int revisionId) {
                return true;
            }
        };
    }

    private static class CompositePatcherTestBuilder {

        private final TPodTemplateSpec.Builder podTemplateSpecBuilder;
        private final DeployUnitContext deployUnitContext;
        private final YTreeBuilder labelsBuilder;
        private final PodSpecCompositePatcher compositePatcher;

        public CompositePatcherTestBuilder(List<SpecPatcher<TPodTemplateSpec.Builder>> patchers) {
            this(singleRevisionHolder(patchers));
        }

        public CompositePatcherTestBuilder(Map<Integer, List<SpecPatcher<TPodTemplateSpec.Builder>>> revisionIdToPatcher,
                                           int fallbackRevisionId) {
            this(new RevisionsHolderImpl<>(revisionIdToPatcher, fallbackRevisionId));
        }

        public CompositePatcherTestBuilder(RevisionsHolder<TPodTemplateSpec.Builder> revisionsHolder) {
            this(
                    TPodTemplateSpec.newBuilder(),
                    DEFAULT_UNIT_CONTEXT,
                    new YTreeBuilder(),
                    new PodSpecCompositePatcher(revisionsHolder)
            );
        }

        public CompositePatcherTestBuilder(
                TPodTemplateSpec.Builder podTemplateSpecBuilder,
                DeployUnitContext deployUnitContext,
                YTreeBuilder labelsBuilder,
                PodSpecCompositePatcher compositePatcher
        ) {
            this.podTemplateSpecBuilder = podTemplateSpecBuilder;
            this.deployUnitContext = deployUnitContext;
            this.labelsBuilder = labelsBuilder;
            this.compositePatcher = compositePatcher;
        }

        public CompositePatcherTestBuilder withPatchersRevision(int patchersRevision) {
            return new CompositePatcherTestBuilder(
                    podTemplateSpecBuilder,
                    deployUnitContext.withDeployUnitSpec(DeployUnitSpec::withPatchersRevision, patchersRevision),
                    labelsBuilder,
                    compositePatcher
            );
        }

        public DeployUnitContext getDeployUnitContext() {
            return deployUnitContext;
        }

        public static class PatchInfo {

            private final Message podTemplateSpec;
            private final DeployUnitContext context;
            private final PodSpecCompositePatcher compositePatcher;

            public PatchInfo(Message podTemplateSpec,
                             DeployUnitContext context,
                             PodSpecCompositePatcher compositePatcher) {
                this.podTemplateSpec = podTemplateSpec;
                this.context = context;
                this.compositePatcher = compositePatcher;
            }

            public Message getPodTemplateSpec() {
                return podTemplateSpec;
            }

            public DeployUnitContext getContext() {
                return context;
            }

            public PodSpecCompositePatcher getCompositePatcher() {
                return compositePatcher;
            }
        }

        public PatchInfo patch() {
            compositePatcher.patch(podTemplateSpecBuilder, deployUnitContext, labelsBuilder);

            return new PatchInfo(
                    podTemplateSpecBuilder.build(),
                    deployUnitContext,
                    compositePatcher
            );
        }
    }

    @Test
    void deployLabelsShouldBeBuiltOk() {
        final String key1 = "key1";
        final String value1 = "value1";

        final String key2 = "key2";
        final String value2 = "value2";

        SpecPatcher<TPodTemplateSpec.Builder> patcher1 = (TPodTemplateSpec.Builder builder, DeployUnitContext context,
                                   YTreeBuilder labelsBuilder) ->
                labelsBuilder.key(key1).value(value1);

        SpecPatcher<TPodTemplateSpec.Builder> patcher2 = (TPodTemplateSpec.Builder builder, DeployUnitContext context,
                                   YTreeBuilder labelsBuilder) ->
                labelsBuilder.key(key2).value(value2);

        var patchers = ImmutableList.of(patcher1, patcher2);

        var patchInfo = new CompositePatcherTestBuilder(patchers).patch();

        TPodTemplateSpec tPodTemplateSpec = (TPodTemplateSpec) patchInfo.getPodTemplateSpec();
        List<TAttribute> builtAttributes = tPodTemplateSpec.getLabels().getAttributesList();

        YTreeBuilder expectedLabelsBuilder = new YTreeBuilder().beginMap();
        expectedLabelsBuilder.key(key1).value(value1);
        expectedLabelsBuilder.key(key2).value(value2);

        TAttribute expectedAttribute = TAttribute.newBuilder()
                .setKey(DEPLOY_LABEL_KEY)
                .setValue(ByteString.copyFrom(expectedLabelsBuilder.endMap().build().toBinary()))
                .build();

        assertThatContains(builtAttributes, expectedAttribute);
    }

    private static class PatchingLoggerTestBuilder {

        private static class PatchingLogger {

            private final List<String> log;

            public PatchingLogger() {
                this.log = new ArrayList<>();
            }

            public List<String> getLog() {
                return log;
            }

            public SpecPatcher<TPodTemplateSpec.Builder> createPatcherWithMessage(String message) {
                return (builder, context, labelsBuilder) -> log.add(message);
            }
        }

        private final CompositePatcherTestBuilder testBuilder;
        private final PatchingLogger patchingLogger;

        public PatchingLoggerTestBuilder(
                CompositePatcherTestBuilder testBuilder,
                PatchingLogger patchingLogger
        ) {
            this.testBuilder = testBuilder;
            this.patchingLogger = patchingLogger;
        }

        public PatchingLoggerTestBuilder withPatchersRevision(int patchersRevision) {
            return new PatchingLoggerTestBuilder(
                    testBuilder.withPatchersRevision(patchersRevision),
                    patchingLogger
            );
        }

        public CompositePatcherTestBuilder getTestBuilder() {
            return testBuilder;
        }

        public List<String> calculatePatchingLog() {
            testBuilder.patch();
            return patchingLogger.getLog();
        }
    }

    public static final Map<Integer, String> REVISION_ID_TO_MESSAGE = ImmutableMap.of(
            1, "Revision 1",
            2, "Revision 2",
            3, "Revision 3"
    );

    public static PatchingLoggerTestBuilder createRevisionSelectionTestBuilderWithFallback(int fallbackRevisionId) {
        PatchingLoggerTestBuilder.PatchingLogger patchingLogger = new PatchingLoggerTestBuilder.PatchingLogger();

        Function<Map.Entry<Integer, String>, List<SpecPatcher<TPodTemplateSpec.Builder>>> createRevisionPatchersByIdToMessage =
                (Map.Entry<Integer, String> e) ->
                        ImmutableList.of(patchingLogger.createPatcherWithMessage(e.getValue()));

        Map<Integer, List<SpecPatcher<TPodTemplateSpec.Builder>>> revisionIdToPatchers = REVISION_ID_TO_MESSAGE.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        createRevisionPatchersByIdToMessage
                ));

        return new PatchingLoggerTestBuilder(
                new CompositePatcherTestBuilder(revisionIdToPatchers, fallbackRevisionId),
                patchingLogger
        );
    }

    private static Stream<Integer> provideRevisionIds() {
        return REVISION_ID_TO_MESSAGE.keySet().stream();
    }

    @ParameterizedTest
    @MethodSource("provideRevisionIds")
    public void okSelectedCorrectRevision(int selectedRevisionId) {
        var revisionSelectionTestBuilder = createRevisionSelectionTestBuilderWithFallback(0)
                .withPatchersRevision(selectedRevisionId);

        List<String> patchingLog = revisionSelectionTestBuilder.calculatePatchingLog();
        assertThatContains(patchingLog, REVISION_ID_TO_MESSAGE.get(selectedRevisionId));
    }

    @ParameterizedTest
    @MethodSource("provideRevisionIds")
    public void okSelectedFallbackRevision(int fallbackRevisionId) {
        var revisionSelectionTestBuilder = createRevisionSelectionTestBuilderWithFallback(fallbackRevisionId)
                .withPatchersRevision(RevisionScheme.DEFAULT_REVISION_ID);

        List<String> patchingLog = revisionSelectionTestBuilder.calculatePatchingLog();
        assertThatContains(patchingLog, REVISION_ID_TO_MESSAGE.get(fallbackRevisionId));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 4})
    public void failSelectedIncorrectRevision(int incorrectRevisionId) {
        int existingRevisionId = REVISION_ID_TO_MESSAGE.keySet().stream().findAny().orElseThrow();

        var revisionSelectionTestBuilder = createRevisionSelectionTestBuilderWithFallback(existingRevisionId)
                .withPatchersRevision(incorrectRevisionId);

        var deployUnitContext = revisionSelectionTestBuilder.getTestBuilder().getDeployUnitContext();

        assertThatThrowsWithMessage(RuntimeException.class,
                String.format(
                        "Project id %s; stage id %s; deploy unit id %s: unknown patchers revision %d",
                        deployUnitContext.getStageContext().getProjectId(),
                        deployUnitContext.getStageContext().getStageId(),
                        deployUnitContext.getDeployUnitId(),
                        incorrectRevisionId
                ),
                revisionSelectionTestBuilder::calculatePatchingLog
        );
    }

    public static PatchingLoggerTestBuilder createPatchingOrderTestBuilder(List<String> patcherMessages) {
        PatchingLoggerTestBuilder.PatchingLogger patchingLogger = new PatchingLoggerTestBuilder.PatchingLogger();

        List<SpecPatcher<TPodTemplateSpec.Builder>> patchers = patcherMessages.stream()
                .map(patchingLogger::createPatcherWithMessage)
                .collect(Collectors.toUnmodifiableList());

        return new PatchingLoggerTestBuilder(
                new CompositePatcherTestBuilder(patchers),
                patchingLogger
        );
    }

    @Test
    public void okPatchersAppliedInCorrectOrder() {
        List<String> patcherMessages = ImmutableList.of(
                "Patcher first in order", "Patcher second in order", "Patcher third in order"
        );

        var patchingOrderTestBuilder = createPatchingOrderTestBuilder(patcherMessages);

        List<String> patchingLog = patchingOrderTestBuilder.calculatePatchingLog();

        assertThatEquals(patchingLog, patcherMessages);
    }
}
