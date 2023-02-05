package ru.yandex.disk.remote;

import org.json.JSONObject;
import org.junit.Test;
import ru.yandex.disk.remote.exceptions.PermanentException;
import ru.yandex.disk.remote.exceptions.TemporaryException;

import static org.mockito.Mockito.verify;
import static ru.yandex.disk.remote.BatchRequestMethodTestHelper.*;
import static ru.yandex.disk.remote.RemoteRepoTestHelper.load;

public class RemoteRepoUnsubscribeMethodTest extends BaseRemoteRepoMethodTest {

    @Test
    public void shouldBuildCorrectRequest() throws Exception {
        fakeOkHttpInterceptor.addResponse(200, load("batch_unsubscribe_response.json"));

        remoteRepo.unsubscribe(createTestSubscriptionId());

        assertBatchRequest(fakeOkHttpInterceptor.getRequest(), fakeOkHttpInterceptor.getRequestQuery());

        assertCorrectSubRequest(0, "disk/notifications", "%25SUB_ID%25");
        assertCorrectSubRequest(1, "data", "%25DATA_SYNC_ID%25");
    }

    private void assertCorrectSubRequest(final int which, final String service, final String id)
            throws Exception {
        final JSONObject request = getSubRequest(fakeOkHttpInterceptor.getRequestBody(), which);
        BatchRequestMethodTestHelper.assertCorrectSubRequest(request,
                "DELETE", "/v1/" + service + "/subscriptions/app/" + id);
    }

    private SubscriptionId createTestSubscriptionId() {
        return new SubscriptionId("%SUB_ID%", "%DATA_SYNC_ID%", "%TOKEN%");
    }

    @Test(expected = TemporaryException.class)
    public void shouldThrowTemporaryErrorOn500() throws Exception {
        fakeOkHttpInterceptor.addResponse(500);

        remoteRepo.unsubscribe(createTestSubscriptionId());
    }

    @Test(expected = PermanentException.class)
    public void shouldThrowPermanentErrorOn400() throws Exception {
        fakeOkHttpInterceptor.addResponse(404);

        remoteRepo.unsubscribe(createTestSubscriptionId());
    }

    @Test
    public void shouldInvokeWebdavClientToo() throws Exception {
        fakeOkHttpInterceptor.addResponse(200, "{}");

        remoteRepo.unsubscribe(createTestSubscriptionId());

        verify(mockWebdavClient).unsubscribe("%TOKEN%");
    }
}
