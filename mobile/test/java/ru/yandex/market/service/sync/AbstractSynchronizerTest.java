package ru.yandex.market.service.sync;

import androidx.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.BaseTest;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;

public class AbstractSynchronizerTest extends BaseTest {

    private TestSynchronizer synchronizer;

    @Test
    public void testSyncExistedDirty() {
        synchronizer.setLocalEntities(singletonList(new TestSynchronizer.TestEntity(1L, 1L, true, "b")));
        synchronizer.prepareToSync(singletonList(createServerEntity()));

        assertThat(synchronizer.localSave, empty());
        assertThat(synchronizer.serverUpload, empty());
        assertThat(synchronizer.serverUpdate, hasSize(1));
        assertThat(synchronizer.serverDelete, empty());
    }

    @Test
    public void testSyncExistedNonDirty() {
        synchronizer.setLocalEntities(singletonList(new TestSynchronizer.TestEntity(1L, 1L, false, "b")));
        synchronizer.prepareToSync(singletonList(createServerEntity()));

        assertThat(synchronizer.localSave, hasSize(1));
        assertThat(synchronizer.serverUpload, empty());
        assertThat(synchronizer.serverUpdate, empty());
        assertThat(synchronizer.serverDelete, empty());
    }

    @Test
    public void testSyncExistedEqualsDirty() {
        synchronizer.setLocalEntities(singletonList(new TestSynchronizer.TestEntity(1L, 1L, true, "a")));
        synchronizer.prepareToSync(singletonList(createServerEntity()));

        assertThat(synchronizer.localSave, empty());
        assertThat(synchronizer.serverUpload, empty());
        assertThat(synchronizer.serverUpdate, hasSize(1));
        assertThat(synchronizer.serverDelete, empty());
    }

    @Test
    public void testSyncExistedEqualsNonDirty() {
        synchronizer.setLocalEntities(singletonList(new TestSynchronizer.TestEntity(1L, 1L, false, "a")));
        synchronizer.prepareToSync(singletonList(createServerEntity()));

        assertThat(synchronizer.localSave, empty());
        assertThat(synchronizer.serverUpload, empty());
        assertThat(synchronizer.serverUpdate, empty());
        assertThat(synchronizer.serverDelete, empty());
    }

    @NonNull
    private TestSynchronizer.TestEntity createServerEntity() {
        return new TestSynchronizer.TestEntity(0L, 1L, false, "a");
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        synchronizer = new TestSynchronizer(getRobolectricContext());
    }
}