package ru.yandex.disk.remote.webdav;

import okhttp3.Request;
import org.junit.Test;
import ru.yandex.disk.remote.exceptions.RemoteExecutionException;

public class AcceptInvitationMethodTest extends WebdavClientMethodTestCase {

    private static final String INVITE_ID = "DIRECToRY";

    @Override
    protected void invokeMethod() throws RemoteExecutionException {
        client.acceptInvitation(INVITE_ID);
    }

    @Override
    protected void checkHttpRequest(final Request request) throws Exception {
        verifyRequest("POST", "/", "share/not_approved/" + INVITE_ID, request);
    }

    @Override
    protected void prepareGoodResponse() throws Exception {
        fakeOkHttpInterceptor.addResponsePermanentlyRedirect("https://webdav.tst.yandex.ru/disk/SharedFolder1");
    }

    @Test
    public void testAcceptInvitation() throws Exception {
        prepareGoodResponse();

        final String location = client.acceptInvitation(INVITE_ID);

        assertEquals("/disk/SharedFolder1", location);
    }

    @Test(expected = InsufficientStorageException.class)
    public void testProcess507() throws Exception {
        prepareToReturnCode(507);
        client.acceptInvitation(INVITE_ID);
    }

}
