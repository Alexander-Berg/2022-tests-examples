package ru.yandex.disk.provider;

import org.hamcrest.MatcherAssert;
import org.junit.Test;
import ru.yandex.util.Path;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.disk.provider.FileTree.*;

public class DeleteByPathMethodTest extends DiskDatabaseMethodTest {

    @Test
    public void testShouldFilesInInnerDirectory() throws Exception {
        FileTree.create().content(
                                  directory("A").content(
                                                         directory("B").content(file("a"))
                                          ),
                                  directory("AB").content()

                ).insertToDiskDatabase(diskDb);

        diskDb.deleteByPath(new Path("/disk/A"));

        selection = diskDb.queryAll();

        MatcherAssert.assertThat(asList("/disk/AB"), equalTo(asPathsList(selection)));
    }

}
