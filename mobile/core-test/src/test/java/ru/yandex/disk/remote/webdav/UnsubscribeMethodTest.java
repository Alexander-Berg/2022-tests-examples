package ru.yandex.disk.remote.webdav;

import okhttp3.Request;
import org.json.JSONObject;
import ru.yandex.disk.remote.exceptions.RemoteExecutionException;
import ru.yandex.disk.util.URLUtil2;

public class UnsubscribeMethodTest extends WebdavClientMethodTestCase {

    private String token;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        token = new JSONObject().put("token", "%TOKEN%").toString();
    }

    @Override
    protected void invokeMethod() throws RemoteExecutionException {
        client.unsubscribe(token);
    }

    @Override
    protected void checkHttpRequest(final Request request) throws Exception {
        final String encodedToken = URLUtil2.encode(token);
        verifyRequest("POST", "/", "push=unsubscribe&token=" + encodedToken, request);
    }

}
