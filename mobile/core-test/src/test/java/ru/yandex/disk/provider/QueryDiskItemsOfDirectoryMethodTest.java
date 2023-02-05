package ru.yandex.disk.provider;

import org.junit.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.util.Path.asPath;

public class QueryDiskItemsOfDirectoryMethodTest extends DiskDatabaseMethodTest {

    @Test
    public void testQueryFileItemsOfDirectory() throws Exception {
        String root = DiskDatabase.ROOT_PATH.getPath();
        DiskItemRow dirToList = new DiskItemRow();
        dirToList.setPath(root, "dirToList");
        dirToList.setIsDir(true);
        diskDb.updateOrInsert(dirToList);

        DiskItemRow file = new DiskItemRow();
        file.setPath(root + "/dirToList", "file.txt");
        file.setIsDir(false);
        diskDb.updateOrInsert(file);

        DiskItemRow fileDeeper = new DiskItemRow();
        fileDeeper.setPath(root + "/dirToList/deeper", "fileDeeper.txt");
        fileDeeper.setIsDir(false);
        diskDb.updateOrInsert(fileDeeper);

        DiskItemRow fileUpper = new DiskItemRow();
        fileUpper.setPath(root, "fileUpper.txt");
        fileUpper.setIsDir(false);
        diskDb.updateOrInsert(fileUpper);

        selection = diskDb.queryFileItemsOfDirectory(asPath(root + "/dirToList"));

        assertThat(asList(root + "/dirToList/file.txt"), equalTo(asPathsList(selection)));
    }
}