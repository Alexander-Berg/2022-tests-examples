package ru.yandex.disk.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import ru.yandex.disk.provider.DiskContract.InvitesCursor;

import java.util.ArrayList;

import static ru.yandex.disk.provider.DiskContract.Invites.*;
import static ru.yandex.disk.sql.SQLVocabulary.CONTENT;

@RunWith(RobolectricTestRunner.class)
public class InvitesUriProcessorTest extends DiskContentProviderTest {

    private Uri countUri;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        countUri = Uri.parse(CONTENT + mockAuthority + "/" + DiskContract.Invites.COUNT_AUTHORITY);
    }

    @Test
    public void testInsert() throws Exception {
        ContentResolver cr = getContentResolver();

        ContentValues invite = new ContentValues();
        invite.put(PATH, "pAth");
        invite.put(OWNER, "oWneR");
        invite.put(READONLY, 1);
        invite.put(LENGTH, 1024);
        invite.put(DISPLAY_NAME, "Display_NAME");
        cr.insert(invitesUri, invite);

        InvitesCursor actual = new InvitesCursor(cr.query(invitesUri, null, null, null, null));
        Assert.assertTrue(actual.moveToFirst());
        Assert.assertEquals(1, actual.getCount());
        Assert.assertEquals("pAth", actual.getPath());
        Assert.assertEquals("oWneR", actual.getOwner());
        Assert.assertTrue(actual.isReadonly());
        Assert.assertEquals(1024, actual.getLength());
        Assert.assertEquals("Display_NAME", actual.getDisplayName());
        Assert.assertFalse(actual.moveToNext());
        assertUriNotified(invitesUri);
        actual.close();
    }

    @Test
    public void testBulkInsert() throws Exception {
        ArrayList<ContentValues> cv = new ArrayList<ContentValues>();
        cv.add(makeInvite1());
        cv.add(makeInvite2());

        ContentResolver cr = getContentResolver();
        int inserted = cr.bulkInsert(invitesUri, cv.toArray(new ContentValues[cv.size()]));

        Assert.assertEquals(2, inserted);

        assertUriNotified(invitesUri);
    }

    @Test
    public void testQueryAll() throws Exception {
        ContentResolver cr = getContentResolver();

        insertInvite1(cr);
        insertInvite2(cr);

        Cursor c = cr.query(invitesUri, null, null, null, null);
        Assert.assertEquals(2, c.getCount());
        assertNotificationUri(invitesUri, c);
        c.close();
    }

    @Test
    public void testQueryById() throws Exception {
        ContentResolver cr = getContentResolver();

        Uri uri1 = insertInvite1(cr);
        Uri uri2 = insertInvite2(cr);

        InvitesCursor invite1 = queryAndVerify(cr, uri1);
        InvitesCursor invite2 = queryAndVerify(cr, uri2);

        invite1.moveToFirst();
        invite2.moveToFirst();
        Assert.assertTrue(invite1.getHash() != invite2.getHash());

        invite1.close();
        invite2.close();
    }

    private InvitesCursor queryAndVerify(ContentResolver cr, Uri uri) {
        InvitesCursor invite = new InvitesCursor(cr.query(uri, null, null, null, null));
        Assert.assertEquals(1, invite.getCount());
        assertNotificationUri(uri, invite);
        return invite;
    }

    @Test
    public void testDelete() throws Exception {
        ContentResolver cr = getContentResolver();

        insertInvite1(cr);
        insertInvite2(cr);

        int deleted = cr.delete(invitesUri, null, null);
        Assert.assertEquals(2, deleted);

        Cursor c = cr.query(invitesUri, null, null, null, null);
        Assert.assertEquals(0, c.getCount());
        c.close();
    }

    @Test
    public void testUriCount() throws Exception {
        ContentResolver cr = getContentResolver();

        insertInvite1(cr);
        insertInvite2(cr);

        Cursor c = cr.query(countUri, null, null, null, null);
        Assert.assertEquals(1, c.getCount());
        c.moveToFirst();
        Assert.assertEquals(2, c.getInt(0));
        assertNotificationUri(countUri, c);
        c.close();
    }

    private Uri insertInvite1(ContentResolver cr) {
        ContentValues invite1 = makeInvite1();
        return cr.insert(invitesUri, invite1);
    }

    private ContentValues makeInvite1() {
        ContentValues invite1 = new ContentValues();
        invite1.put(PATH, "pAth1");
        invite1.put(OWNER, "oWneR");
        invite1.put(DISPLAY_NAME, "Display_NAME");
        invite1.put(READONLY, 1);
        invite1.put(LENGTH, 1024);
        return invite1;
    }

    private Uri insertInvite2(ContentResolver cr) {
        ContentValues invite2 = makeInvite2();
        return cr.insert(invitesUri, invite2);
    }

    private ContentValues makeInvite2() {
        ContentValues invite2 = new ContentValues();
        invite2.put(PATH, "pAth2");
        invite2.put(OWNER, "oWneR");
        invite2.put(DISPLAY_NAME, "Display_NAME");
        invite2.put(READONLY, 1);
        invite2.put(LENGTH, 1024);
        return invite2;
    }

}
