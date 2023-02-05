package ru.yandex.disk.remote.webdav;

import okhttp3.Request;
import org.junit.Test;
import ru.yandex.disk.invites.Invite;
import ru.yandex.disk.remote.exceptions.RemoteExecutionException;

import java.util.List;

public class GetInvitesMethodTest extends WebdavClientReadMethodTestCase {

    protected void checkHttpRequest(Request request) throws Exception {
        verifyRequest("PROPFIND", "/", "share/not_approved/", request);
        assertHasHeader("Depth", 1, request);
    }

    @Test
    public void testGetInvites() throws Exception {
        prepareToReturnContentFromFile(207, "GetInvitesResponse.xml");

        List<Invite> invites = client.getInvites();

        assertNotNull(invites);
        assertEquals(2, invites.size());

        Invite firstInvite = invites.get(0);
        assertEquals("fdc234521c0c710b597f0bc113970038", firstInvite.getPath());
        assertEquals("Documents", firstInvite.getDisplayName());
        assertFalse(firstInvite.getReadonly());
        assertEquals(418830, firstInvite.getFileSize());

        Invite secondInvite = invites.get(1);
        assertTrue(secondInvite.getReadonly());
        assertEquals("Vasily Pupkin", secondInvite.getOwnerName());
    }

    @Override
    protected void invokeMethod() throws RemoteExecutionException {
        client.getInvites();
    }

    @Override
    protected void prepareGoodResponse() throws Exception {
        prepareToReturnContentFromFile(207, "GetInvitesResponse.xml");
    }

    @Override
    protected int getGoodCode() {
        return 207;
    }

}
