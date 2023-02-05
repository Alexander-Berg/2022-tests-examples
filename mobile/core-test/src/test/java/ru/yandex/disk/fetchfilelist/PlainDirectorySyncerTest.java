package ru.yandex.disk.fetchfilelist;

import org.junit.Test;
import ru.yandex.disk.DiskItem;
import ru.yandex.disk.provider.DiskItemBuilder;
import ru.yandex.disk.provider.FileTree;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.disk.provider.FileTree.directory;
import static ru.yandex.util.Path.asPath;

public class PlainDirectorySyncerTest extends DirectorySyncerTest {
    @Override
    protected PlainDirectorySyncer createDirectorySyncer() {
        return new PlainDirectorySyncer(diskDatabase, getDirectory());
    }

    @Test
    public void testFileInDirectoryInOfflineDirectoryShouldBeInsertAs_IN_OFFLINE_DIR() throws Exception {
        FileTree.create().content(
                directory("A").setOffline(DiskItem.OfflineMark.MARKED).content(
                        directory("B").setOffline(DiskItem.OfflineMark.IN_OFFLINE_DIRECTORY))
        ).insertToDiskDatabase(diskDatabase);

        DiskItem fileAB = new DiskItemBuilder().setPath("/disk/A/B").setIsDir(true).build();
        DiskItem fileABa = new DiskItemBuilder().setPath("/disk/A/B/a").build();

        syncer = createSyncer("/disk/A/B");

        emulateSync(fileAB, fileABa);

        DiskItem fileInDb = diskDatabase.queryFileItem(asPath("/disk/A/B/a"));

        assertThat(fileInDb.getOffline(), equalTo(DiskItem.OfflineMark.IN_OFFLINE_DIRECTORY));
    }
}
