package ru.yandex.disk.provider;

import junit.framework.Assert;
import org.junit.Test;
import ru.yandex.disk.ContentDescription;
import ru.yandex.disk.DiskItem;
import ru.yandex.util.Path;

import static ru.yandex.util.Path.asPath;

public class PatchEtagLocalAndContentDescriptionMethodTest extends DiskDatabaseMethodTest {

    private ContentDescription contentDescription;
    private Path pathA;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        pathA = new Path("/disk/a");
        contentDescription = new ContentDescription();
        contentDescription.setContentType("text/plain");
        contentDescription.setMediaType("document");
        contentDescription.setHasThumbnail(true);
        contentDescription.setEtag("ETAG-LOCAL");
        contentDescription.setContentLength(100);

    }

    @Test
    public void testShouldNotInsertNewItemOnPatch() throws Exception {
        diskDb.patchEtagLocalAndContentDescription(pathA, contentDescription);

        selection = diskDb.queryAll();
        Assert.assertEquals(0, selection.getCount());
    }

    @Test
    public void testShouldPatchOnlyGivenRow() throws Exception {
        DiskItemRow fileA = new DiskItemRow();
        fileA.setPath(pathA.getParentPath(), pathA.getName());
        fileA.setIsDir(false);
        fileA.setOfflineMark(DiskItem.OfflineMark.MARKED);
        diskDb.updateOrInsert(fileA);

        DiskItemRow fileB = new DiskItemRow();
        fileB.setPath(pathA.getParentPath(), "b");
        fileB.setIsDir(false);
        fileB.setOfflineMark(DiskItem.OfflineMark.MARKED);
        diskDb.updateOrInsert(fileB);

        diskDb.patchEtagLocalAndContentDescription(pathA, contentDescription);

        selection = diskDb.queryFileByPath(asPath("/disk/b"));

        Assert.assertEquals(null, selection.get(0).getMimeType());
    }

    @Test
    public void testShouldPatchFiveFields() throws Exception {
        DiskItemRow filA = new DiskItemRow();
        filA.setPath(pathA.getParentPath(), pathA.getName());
        filA.setIsDir(false);
        filA.setOfflineMark(DiskItem.OfflineMark.MARKED);
        diskDb.updateOrInsert(filA);

        diskDb.patchEtagLocalAndContentDescription(pathA, contentDescription);

        selection = diskDb.queryFileByPath(pathA);

        DiskItem file = selection.get(0);

        Assert.assertEquals("text/plain", file.getMimeType());
        Assert.assertEquals("document", file.getMediaType());
        Assert.assertEquals(true, file.getHasThumbnail());
        Assert.assertEquals("ETAG-LOCAL", file.getETagLocal());
        Assert.assertEquals(100, file.getSize());
    }
}
