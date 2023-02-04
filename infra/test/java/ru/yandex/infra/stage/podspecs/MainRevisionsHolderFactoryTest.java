package ru.yandex.infra.stage.podspecs;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.infra.stage.Main;
import ru.yandex.infra.stage.podspecs.patcher.PatchersHolder;
import ru.yandex.infra.stage.podspecs.revision.PatcherType;
import ru.yandex.infra.stage.podspecs.revision.PatchersRevisionsHolderFactory;
import ru.yandex.infra.stage.podspecs.revision.model.Revision;
import ru.yandex.infra.stage.podspecs.revision.model.RevisionScheme;

import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;

public class MainRevisionsHolderFactoryTest {

    private static final int EXPECTED_FALLBACK_REVISION_ID = 1;
    private static final List<Integer> ALL_REVISION_IDS = ImmutableList.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
            14);

    private static PatchersHolder patchersHolder;

    @BeforeAll
    public static void beforeAll() {
        var patcherClassToPatcher = MainPatchersHolderFactoryTest.PATCHER_CLASSES.stream()
                .collect(Collectors.toUnmodifiableMap(
                        Function.identity(),
                        Mockito::mock
                ));

        patchersHolder = new PatchersHolder(patcherClassToPatcher);
    }

    private static RevisionScheme loadRevisionScheme() {
        return RevisionScheme.loadFromResource(Main.REVISION_SCHEME_FILE_NAME);
    }

    @Test
    public void testLoadRevisionSchemeFallbackRevisionId() {
        var revisionScheme = loadRevisionScheme();

        assertThatEquals(revisionScheme.getFallbackRevisionId(), EXPECTED_FALLBACK_REVISION_ID);
    }

    @Test
    public void testLoadRevisionSchemeRevisionIds() {
        var revisionScheme = loadRevisionScheme();

        var actualRevisionIds = Arrays.stream(revisionScheme.getRevisions())
                .map(Revision::getId)
                .collect(Collectors.toUnmodifiableList());

        assertThatEquals(actualRevisionIds, ALL_REVISION_IDS);
    }

    @Test
    public void testCreate() {
        var revisionScheme = loadRevisionScheme();

        var activeRevisionIds = ImmutableList.of(1, 3, 5);
        var revisionSchemeConfig = ConfigFactory.parseMap(
                ImmutableMap.of(PatchersRevisionsHolderFactory.ACTIVE_REVISION_IDS_CONFIG_KEY, activeRevisionIds)
        );

        var revisionsHolder = new PatchersRevisionsHolderFactory(patchersHolder).from(
                revisionScheme, revisionSchemeConfig,
                PatcherType.POD_SPEC);

        for (Revision revision : revisionScheme.getRevisions()) {
            int revisionId = revision.getId();
            boolean actualIsActive = revisionsHolder.containsRevision(revisionId);
            boolean expectedIsActive = activeRevisionIds.contains(revisionId);

            assertThatEquals(actualIsActive, expectedIsActive);
        }
    }
}
