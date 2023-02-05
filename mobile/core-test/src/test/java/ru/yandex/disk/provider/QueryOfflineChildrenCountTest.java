package ru.yandex.disk.provider;

import org.junit.Test;
import ru.yandex.disk.FileItem.OfflineMark;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.disk.provider.FileTree.directory;
import static ru.yandex.disk.provider.FileTree.file;

public class QueryOfflineChildrenCountTest extends DiskDatabaseMethodTest {

    @Test
    public void testQueryOfflineChildrenCount() throws Exception {
        FileTree.create().content(
                directory("A").content(
                        directory("B").setOffline(OfflineMark.IN_OFFLINE_DIRECTORY)
                                .content(file("a").setOffline(OfflineMark.IN_OFFLINE_DIRECTORY)),
                        file("C").setOffline(OfflineMark.IN_OFFLINE_DIRECTORY)
                ).setOffline(OfflineMark.MARKED)

        ).insertToDiskDatabase(diskDb);

        assertThat(diskDb.queryOfflineChildrenCount("/disk/A"), equalTo(3));
    }
}
