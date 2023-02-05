package ru.yandex.disk.remote.webdav;

import okhttp3.Request;
import ru.yandex.disk.remote.exceptions.RemoteExecutionException;

public class RejectInvitationMethodTest extends WebdavClientMethodTestCase {

    private static final String INVITE_ID = "DIRECToRY";

    @Override
    protected void invokeMethod() throws RemoteExecutionException {
        client.rejectInvitation(INVITE_ID);
    }

    @Override
    protected void checkHttpRequest(final Request request) throws Exception {
        verifyRequest("DELETE", "/", "share/not_approved/" + INVITE_ID, request);
    }

}
