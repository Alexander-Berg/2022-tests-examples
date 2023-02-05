package ru.yandex.disk.provider;

import org.hamcrest.MatcherAssert;
import org.junit.Test;
import ru.yandex.disk.DiskItem;
import ru.yandex.util.Path;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.disk.provider.DiskDatabase.DirectorySyncStatus.SYNCING;
import static ru.yandex.disk.provider.FileTree.*;

public class ClearEtagLocalRecursivelyMethodTest extends DiskDatabaseMethodTest {

    @Test
    public void testUpdateInnerFiles() throws Exception {
        FileTree.create().content(directory("A").setSyncStatus(SYNCING)
                .content(directory("B").setSyncStatus(SYNCING)
                        .content(file("a.txt").setEtagLocal("ABC"))),
                                  directory("C").setSyncStatus(SYNCING)
                ).insertToDiskDatabase(diskDb);

        Path dirA = new Path("/disk/A");
        diskDb.clearEtagLocalRecursively(dirA);

        selection = diskDb.queryFilesRecursively(dirA);
        MatcherAssert.assertThat(selection.getCount(), equalTo(3));
        for (DiskItem file : selection) {
            String eTagLocal = file.getETagLocal();
            MatcherAssert.assertThat(file.getPath() + " has etaglocal = " + eTagLocal, null, equalTo(eTagLocal));
        }
    }

    @Test
    public void testUpdateOnlyGivenDirectory() throws Exception {
        FileTree.create().content(
                                  directory("A").setSyncStatus(SYNCING),
                                  directory("B").setSyncStatus(SYNCING)
                ).insertToDiskDatabase(diskDb);

        String newValue = "CBA";
        diskDb.clearEtagLocalRecursively(new Path("/disk/A"));

        selection = diskDb.queryFileByPath(new Path("/disk/B"));
        DiskItem dirB = selection.get(0);
        MatcherAssert.assertThat(dirB.getPath() + " has etaglocal = "
                + dirB.getETagLocal(), SYNCING, equalTo(dirB.getETagLocal()));
    }
}