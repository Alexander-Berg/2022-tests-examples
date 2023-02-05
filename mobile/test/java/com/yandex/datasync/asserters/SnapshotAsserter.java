/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.asserters;

import androidx.annotation.Nullable;

import com.yandex.datasync.wrappedModels.Snapshot;

import static com.yandex.datasync.asserters.CollectionAsserter.assertCollection;
import static com.yandex.datasync.asserters.CollectionAsserter.assertCollectionIgnoreRevision;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public final class SnapshotAsserter {

    public static void assertSnapshot(@Nullable final Snapshot actual,
                                      @Nullable final Snapshot expected) {
        assertNotNull(actual);
        assertNotNull(expected);

        assertThat(actual.getRevision(), is(not(0)));
        assertThat(actual.getRevision(), is(expected.getRevision()));

        assertNotNull(actual.getDatabaseId());
        assertNotNull(actual.getDatabaseContext());

        assertThat(actual.getDatabaseId(), is(expected.getDatabaseId()));
        assertThat(actual.getDatabaseContext(), is(expected.getDatabaseContext()));

        assertThat(actual.getCollectionsCount(), is(expected.getCollectionsCount()));
        final String actualCollectionsIds[] = actual.getCollectionIds();
        final String expectedCollectionsIds[] = expected.getCollectionIds();

        assertNotNull(actualCollectionsIds);
        assertNotNull(expectedCollectionsIds);

        assertThat(actualCollectionsIds.length, is(expectedCollectionsIds.length));

        for (final String actualCollectionId : actualCollectionsIds) {
            assertCollection(actual.getCollection(actualCollectionId),
                             expected.getCollection(actualCollectionId));
        }
    }

    public static void assertSnapshotIgnoreRevision(@Nullable final Snapshot actual,
                                                    @Nullable final Snapshot expected) {
        assertNotNull(actual);
        assertNotNull(expected);

        assertNotNull(actual.getDatabaseId());
        assertNotNull(actual.getDatabaseContext());

        assertThat(actual.getDatabaseContext(), is(expected.getDatabaseContext()));

        assertThat(actual.getCollectionsCount(), is(expected.getCollectionsCount()));
        final String actualCollectionsIds[] = actual.getCollectionIds();
        final String expectedCollectionsIds[] = expected.getCollectionIds();

        assertNotNull(actualCollectionsIds);
        assertNotNull(expectedCollectionsIds);

        assertThat(actualCollectionsIds.length, is(expectedCollectionsIds.length));

        for (final String actualCollectionId : actualCollectionsIds) {
            assertCollectionIgnoreRevision(actual.getCollection(actualCollectionId),
                                           expected.getCollection(actualCollectionId));
        }
    }
}
