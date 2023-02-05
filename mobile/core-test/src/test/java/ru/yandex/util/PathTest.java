package ru.yandex.util;

import org.junit.Test;
import ru.yandex.disk.test.TestCase2;

public class PathTest extends TestCase2 {

    @Test
    public void testIsSubpathOf() throws Exception {

        Path dir = new Path("/disk/dir");
        Path subdir = new Path("/disk/dir/subdir");
        Path notsubdir = new Path("/disk/dirsubdir");

        assertTrue(dir.isSubpathOf(dir));
        assertFalse(dir.isSubpathOf(subdir));
        assertFalse(dir.isSubpathOf(notsubdir));

        assertTrue(subdir.isSubpathOf(dir));
        assertTrue(subdir.isSubpathOf(subdir));
        assertFalse(subdir.isSubpathOf(notsubdir));

        assertFalse(notsubdir.isSubpathOf(dir));
        assertFalse(notsubdir.isSubpathOf(subdir));
        assertTrue(notsubdir.isSubpathOf(notsubdir));
    }

    @Test
    public void testUnhiddenSubpathOf() {
        assertFalse(new Path("/disk/dir/.thumbs").isUnhiddenSubpathOf(new Path("/disk/dir")));
        assertFalse(new Path("/disk/dir/.thumbs/1").isUnhiddenSubpathOf(new Path("/disk/dir")));
        assertFalse(new Path("/disk/dir/DCIM/.thumbs").isUnhiddenSubpathOf(new Path("/disk/dir")));

        assertFalse(new Path("/not-subpath").isUnhiddenSubpathOf(new Path("/disk/dir")));

        assertTrue(new Path("/disk/dir/thumbs").isUnhiddenSubpathOf(new Path("/disk/dir")));
        assertTrue(new Path("/disk/dir/thumbs/1").isUnhiddenSubpathOf(new Path("/disk/dir")));
        assertTrue(new Path("/disk/dir/DCIM/thumbs").isUnhiddenSubpathOf(new Path("/disk/dir")));
    }
}
