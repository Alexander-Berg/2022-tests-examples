package ru.yandex.disk.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import ru.yandex.disk.provider.DiskContract.DiskFile;

import static ru.yandex.disk.sql.SQLVocabulary.CONTENT;

@RunWith(RobolectricTestRunner.class)
public class DiskQueueContentProviderTest extends DiskContentProviderTest implements DiskFile {

    @Test
    public void testQueryWithUser() throws Exception {

        final ContentResolver cr = getMockContentResolver();
        final Uri uri = getRequestUri("megauser");

        try {
            final Cursor files = cr.query(uri, null, null, null, null);
            files.close();
        } catch (final IllegalArgumentException e) {
            Assert.fail("uri without ?user=username");
        }
    }

    @Test
    public void testUpdateWithUser() throws Exception {

        final ContentResolver cr = getMockContentResolver();
        final Uri uri = getRequestUri("megauser");

        try {
            cr.update(uri, new ContentValues(), null, null);
        } catch (final IllegalArgumentException e) {
            Assert.fail("uri without ?user=username");
        }
    }

    @Test
    public void testInsertWithUser() throws Exception {

        final ContentResolver cr = getMockContentResolver();
        final Uri uri = getRequestUri("megauser");

        try {
            cr.insert(uri, null);
        } catch (final IllegalArgumentException e) {
            Assert.fail("uri without ?user=username");
        }
    }

    @Test
    public void testBulkInsertWithUser() throws Exception {

        final ContentResolver cr = getMockContentResolver();
        final Uri uri = getRequestUri("megauser");

        try {
            cr.delete(uri, null, null);
        } catch (final IllegalArgumentException e) {
            Assert.fail("uri without ?user=username");
        }
    }

    @Test
    public void testDeleteWithUser() throws Exception {

        final ContentResolver cr = getMockContentResolver();
        final Uri uri = getRequestUri("megauser");
        System.out.println(uri);

        try {
            cr.delete(uri, null, null);
        } catch (final IllegalArgumentException e) {
            Assert.fail("uri without ?user=username");
        }
    }

    private Uri getRequestUri(final String username) {
        return Uri.parse(CONTENT + mockAuthority + "/" + DiskContract.Queue.makeQueuePath(username, null));
    }
}
