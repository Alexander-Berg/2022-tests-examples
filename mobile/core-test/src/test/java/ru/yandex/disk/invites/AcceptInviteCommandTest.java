package ru.yandex.disk.invites;

import android.content.ContentResolver;
import android.content.Context;
import android.database.MatrixCursor;
import android.net.Uri;
import org.junit.Test;
import ru.yandex.disk.AppStartSessionProvider;
import ru.yandex.disk.DeveloperSettings;
import ru.yandex.disk.DiskItem;
import ru.yandex.disk.commonactions.SingleWebdavClientPool;
import ru.yandex.disk.event.EventLogger;
import ru.yandex.disk.provider.DiskContract;
import ru.yandex.disk.provider.DiskDatabase;
import ru.yandex.disk.provider.DiskFileCursor;
import ru.yandex.disk.remote.RemoteRepo;
import ru.yandex.disk.remote.RestApiClient;
import ru.yandex.disk.remote.webdav.WebdavClient;
import ru.yandex.disk.service.CommandLogger;
import ru.yandex.disk.test.AndroidTestCase2;
import ru.yandex.disk.test.TestObjectsFactory;
import ru.yandex.disk.toggle.SeparatedAutouploadToggle;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;
import static ru.yandex.disk.test.MoreMatchers.anyUri;
import static ru.yandex.disk.test.TestObjectsFactory.createCredentials;

public class AcceptInviteCommandTest extends AndroidTestCase2 {
    private AcceptInviteCommand command;
    private DiskDatabase diskDatabase;
    private ContentResolver mockContentResolver;
    private WebdavClient mockWebdavClient;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Context context = getMockContext();
        diskDatabase = TestObjectsFactory.createDiskDatabase(
                TestObjectsFactory.createSqlite(context));
        mockContentResolver = mock(ContentResolver.class);
        mockWebdavClient = mock(WebdavClient.class);
        command = new AcceptInviteCommand(new EventLogger(), mockContentResolver, new CommandLogger(),
            new RemoteRepo(createCredentials(), new SingleWebdavClientPool(mockWebdavClient), mock(RestApiClient.class),
                mock(DeveloperSettings.class), new SeparatedAutouploadToggle(false), mock(AppStartSessionProvider.class)),
            diskDatabase);

    }

    @Test
    public void shouldPutNewDirAfterInviteAcceptance() throws Exception {
        addInvite();
        when(mockWebdavClient.acceptInvitation(anyString())).thenReturn("/disk/A");

        command.execute(new AcceptInviteCommandRequest(Uri.parse("/disk/A")));

        DiskFileCursor cursor = diskDatabase.queryAll();
        DiskItem dirA = cursor.get(0);
        assertThat(dirA.getDisplayName(), equalTo("A"));
        assertThat(dirA.isShared(), equalTo(true));
        int columnIndex = cursor.getColumnIndex(DiskContract.DiskFile.ROW_TYPE);
        assertThat(cursor.isNull(columnIndex), equalTo(false));
    }

    private void addInvite() {
        MatrixCursor invites = new MatrixCursor(new String[]{
                DiskContract.Invites.PATH,
                DiskContract.Invites.DISPLAY_NAME,
                DiskContract.Invites.READONLY,
                DiskContract.Invites.LENGTH
        });
        invites.addRow(new Object[]{
                "123",
                "A",
                0,
                0
        });
        ContentResolver cr = this.mockContentResolver;
        when(cr.query(anyUri(), nullable(String[].class), nullable(String.class),
                nullable(String[].class), nullable(String.class))).thenReturn(invites);
    }
}
