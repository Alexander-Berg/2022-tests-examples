package ru.yandex.disk.provider;

import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import org.junit.Test;
import org.robolectric.android.controller.ContentProviderController;

import ru.yandex.disk.sql.SQLiteOpenHelper2;
import ru.yandex.disk.sql.SQLiteDatabase2;

import static org.mockito.Mockito.*;

@TargetApi(Build.VERSION_CODES.FROYO)
public class SQLiteUriProcessorTest extends DiskContentProviderTest {

    /**
     * We can not run this test because SQLiteDatabase can not be mocked.
     */
    @Test
    public void testBulkInsert() throws Exception {
        ContentProvider provider = mock(ContentProvider.class);
        when(provider.getContext()).thenReturn(getMockContext());
        ContentProviderController.of(provider).create("data");

        SQLiteOpenHelper2 db = mock(SQLiteOpenHelper2.class);
        SQLiteDatabase2 wdb = mock(SQLiteDatabase2.class);
        when(db.getWritableDatabase()).thenReturn(wdb);
        SQLiteUriProcessor<?> processor = new InvitesUriProcessor(getProvider().getContext(), () -> db);

        ContentValues invite = new ContentValues();
        invite.put(DiskContract.Invites.PATH, "path");
        processor.bulkInsert(invitesUri, new ContentValues[]{invite});

        verify(wdb).beginTransaction();
        verify(wdb).insert(anyString(), eq(SQLiteDatabase.CONFLICT_NONE), any(ContentValues.class));
        verify(wdb).setTransactionSuccessful();
        verify(wdb).endTransaction();

    }

}
