package ru.yandex.disk.remote.webdav;

import okhttp3.Request;
import org.json.JSONObject;
import ru.yandex.disk.remote.exceptions.RemoteExecutionException;

import java.util.Collections;

public class SubscribeMethodTest extends WebdavClientMethodTestCase {

    private String token;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        token = new JSONObject().put("token", "TOKEN").toString();
    }

    protected void checkHttpRequest(final Request request) throws Exception {
        final String encodedToken = "%7B%22token%22%3A%22TOKEN%22%7D";
        final String query = "push=subscribe&token=" + encodedToken + "&allow=" +
            "share_invite_new,space_is_low,space_is_full,photoslice_updated,album_deltas_updated,albums,quota_overdraft";
        verifyRequest("POST", "/", query, request);
    }

    @Override
    protected void invokeMethod() throws RemoteExecutionException {
        client.subscribe(token, Collections.<String>emptyList());
    }
}
