package ru.yandex.infra.stage.podspecs.revision;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ru.yandex.infra.stage.podspecs.SpecPatcher;
import ru.yandex.infra.stage.podspecs.patcher.DummyPatchersHolderFactory;
import ru.yandex.infra.stage.podspecs.patcher.PatchersHolder;
import ru.yandex.infra.stage.podspecs.patcher.dummy.first.FirstDummyPatcherV1;
import ru.yandex.infra.stage.podspecs.patcher.dummy.second.SecondDummyPatcherV1;
import ru.yandex.infra.stage.podspecs.patcher.dummy.second.SecondDummyPatcherV2;
import ru.yandex.infra.stage.podspecs.patcher.dummy.third.ThirdDummyPatcherV1;
import ru.yandex.infra.stage.podspecs.patcher.dummy.third.ThirdDummyPatcherV2;
import ru.yandex.infra.stage.podspecs.revision.model.DummyRevisionSchemeTestData;
import ru.yandex.infra.stage.podspecs.revision.model.Revision;
import ru.yandex.infra.stage.podspecs.revision.model.RevisionScheme;
import ru.yandex.yp.client.api.TPodTemplateSpec;

import static ru.yandex.infra.stage.podspecs.revision.model.DummyRevisionSchemeTestData.REVISION_1;
import static ru.yandex.infra.stage.podspecs.revision.model.DummyRevisionSchemeTestData.REVISION_2;
import static ru.yandex.infra.stage.podspecs.revision.model.DummyRevisionSchemeTestData.REVISION_3;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatSameInstance;

public class RevisionsHolderFactoryTest {

    private static PatchersHolder<TPodTemplateSpec.Builder> patchersHolder;
    private static RevisionScheme revisionScheme;
    private static Config revisionSchemeConfig;

    @BeforeAll
    public static void beforeTests() {
        patchersHolder = DummyPatchersHolderFactory.create();
        revisionScheme = DummyRevisionSchemeTestData.DUMMY_REVISION_SCHEME;

        var activeRevisionIds = Arrays.stream(revisionScheme.getRevisions())
                .mapToInt(Revision::getId)
                .boxed()
                .collect(Collectors.toUnmodifiableList());

        revisionSchemeConfig = ConfigFactory.parseMap(ImmutableMap.of(
                PatchersRevisionsHolderFactory.ACTIVE_REVISION_IDS_CONFIG_KEY, activeRevisionIds
        ));
    }

    @SafeVarargs
    private static List<SpecPatcher<TPodTemplateSpec.Builder>> getExpectedPatchers(Class<? extends SpecPatcher<TPodTemplateSpec.Builder>>... patcherClassesOrder) {
        return patchersHolder.getPatchersInExactOrder(
                Arrays.asList(patcherClassesOrder)
        );
    }

    private static RevisionsHolder<TPodTemplateSpec.Builder> createExpectedRevisionsHolder() {
        var expectedRevisionIdToPatchersBuilder = ImmutableMap.<Integer, List<SpecPatcher<TPodTemplateSpec.Builder>>>builder();

        expectedRevisionIdToPatchersBuilder.put(
                REVISION_1.getId(), getExpectedPatchers(FirstDummyPatcherV1.class, SecondDummyPatcherV1.class,
                        ThirdDummyPatcherV1.class)
        );

        expectedRevisionIdToPatchersBuilder.put(
                REVISION_2.getId(), getExpectedPatchers(SecondDummyPatcherV1.class, ThirdDummyPatcherV2.class,
                        FirstDummyPatcherV1.class)
        );

        expectedRevisionIdToPatchersBuilder.put(
                REVISION_3.getId(), getExpectedPatchers(ThirdDummyPatcherV2.class, FirstDummyPatcherV1.class,
                        SecondDummyPatcherV2.class)
        );

        return new RevisionsHolderImpl<>(
                expectedRevisionIdToPatchersBuilder.build(),
                revisionScheme.getFallbackRevisionId()
        );
    }

    @Test
    public void testCreateFrom() {
        var expectedRevisionsHolder = createExpectedRevisionsHolder();
        var revisionsHolder = new PatchersRevisionsHolderFactory<>(patchersHolder).from(
                revisionScheme, revisionSchemeConfig, PatcherType.POD_SPEC);

        assertThatEquals(revisionsHolder.getPatchersFor(RevisionScheme.DEFAULT_REVISION_ID),
                expectedRevisionsHolder.getPatchersFor(RevisionScheme.DEFAULT_REVISION_ID));

        for (Revision revision : DummyRevisionSchemeTestData.REVISIONS) {
            int revisionId = revision.getId();
            List<SpecPatcher<TPodTemplateSpec.Builder>> patchers = revisionsHolder.getPatchersFor(revisionId).orElseThrow();
            var expectedPatchers = expectedRevisionsHolder.getPatchersFor(revisionId).orElseThrow();

            assertThatEquals(patchers.size(), expectedPatchers.size());
            for (int index = 0; index < patchers.size(); ++index) {
                assertThatSameInstance(
                        patchers.get(index),
                        expectedPatchers.get(index)
                );
            }
        }
    }
}
