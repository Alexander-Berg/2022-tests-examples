package ru.yandex.disk.provider;

import org.junit.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.disk.provider.FileTree.*;
import static ru.yandex.util.Path.asPath;

public class QueryFilesRecurcivelyMethodTest extends DiskDatabaseMethodTest {

    @Test
    public void testShouldQueryInDirectory() throws Exception {
        FileTree.create().content(directory("A").content(file("a"))).insertToDiskDatabase(diskDb);
        selection = diskDb.queryFilesRecursively(asPath("/disk/A"));

        assertThat(asList("/disk/A", "/disk/A/a"), equalTo(asPathsList(selection)));
    }

    @Test
    public void testShouldQueryInInnerDirectory() throws Exception {
        FileTree.create().content(directory("A").content(directory("B").content(file("a")))).insertToDiskDatabase(diskDb);
        selection = diskDb.queryFilesRecursively(asPath("/disk/A"));

        assertThat(asList("/disk/A", "/disk/A/B", "/disk/A/B/a"), equalTo(asPathsList(selection)));
    }

    @Test
    public void testTrickyFileTree() throws Exception {
        FileTree.create()
                .content(directory("A")
                        .content(directory("B").content(file("a"))),
                         directory("AB")
                ).insertToDiskDatabase(diskDb);
        selection = diskDb.queryFilesRecursively(asPath("/disk/A"));

        assertThat(asList("/disk/A", "/disk/A/B", "/disk/A/B/a"), equalTo(asPathsList(selection)));
    }

    @Test
    public void testTrickyFileTree2() throws Exception {
        FileTree.create()
                .content(directory("\\A")
                        .content(file("a"))
                ).insertToDiskDatabase(diskDb);

        selection = diskDb.queryFilesRecursively(asPath("/disk/\\A"));

        assertThat(asList("/disk/\\A", "/disk/\\A/a"), equalTo(asPathsList(selection)));
    }

}
