package ru.yandex.infra.stage.podspecs.revision.model;

import org.junit.jupiter.api.Test;

import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;

public class RevisionSchemeTest {

    public static final String DUMMY_REVISION_SCHEME_FILE_NAME = "dummy_revision_scheme.json";

    @Test
    public void testLoadFromResource() {
        var revisionScheme = RevisionScheme.loadFromResource(
                DUMMY_REVISION_SCHEME_FILE_NAME
        );

        assertThatEquals(revisionScheme, DummyRevisionSchemeTestData.DUMMY_REVISION_SCHEME);
    }
}
