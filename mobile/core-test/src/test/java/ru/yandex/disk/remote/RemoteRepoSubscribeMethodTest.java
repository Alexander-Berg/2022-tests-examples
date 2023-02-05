package ru.yandex.disk.remote;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import ru.yandex.disk.remote.exceptions.PermanentException;
import ru.yandex.disk.remote.exceptions.TemporaryException;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verify;
import static ru.yandex.disk.remote.BatchRequestMethodTestHelper.*;
import static ru.yandex.disk.remote.RemoteRepoTestHelper.load;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class RemoteRepoSubscribeMethodTest extends BaseRemoteRepoMethodTest {

    @Test
    public void shouldBuildCorrectRequest() throws Exception {
        fakeOkHttpInterceptor.addResponse(200, load("batch_subscribe_response.json"));

        remoteRepo.subscribe(createTestSubscriptionRequest());

        assertBatchRequest(fakeOkHttpInterceptor.getRequest(), fakeOkHttpInterceptor.getRequestQuery());

        final JSONObject request1 = getSubRequest(fakeOkHttpInterceptor.getRequestBody(), 0);
        assertCorrectSubRequest(request1,
                "PUT",
                "/v1/" + "disk/notifications" + "/subscriptions/app" +
                        "?app_name=ru.yandex.disk" +
                        "&events=notification_mobile_v2&tags=android" +
                        "&app_instance_id=INSTANCE" +
                        "&registration_token=regId" +
                        "&platform=gcm");
        final JSONObject request2 = getSubRequest(fakeOkHttpInterceptor.getRequestBody(), 1);
        assertCorrectSubRequest(request2,
                "PUT",
                "/v1/" + "data" + "/subscriptions/app" +
                        "?app_name=ru.yandex.disk" +
                        "&app_instance_id=INSTANCE" +
                        "&registration_token=regId" +
                        "&platform=gcm" +
                        "&databases_ids=DB1,DB2");
    }

    @Test
    public void shouldParseResponse() throws Exception {
        fakeOkHttpInterceptor.addResponse(200, load("batch_subscribe_response.json"));

        final SubscriptionId subscriptionId = remoteRepo.subscribe(createTestSubscriptionRequest());

        assertThat(subscriptionId.getShortMessagesSubscriptionId(), startsWith("eyJwdXNoX3"));
        assertThat(subscriptionId.getShortMessagesSubscriptionId(), endsWith("IjoiMTU3NjAzNzYyIn0="));
        assertThat(subscriptionId.getDataSyncSubscriptionId(), startsWith("eyJwdXNoX3"));
        assertThat(subscriptionId.getDataSyncSubscriptionId(), endsWith("NTc2MDM3NjIifQ=="));

        assertThat(subscriptionId.getWebdavToken(),
                equalTo(createTestSubscriptionRequest().getWebdavToken()));
    }

    @Test(expected = TemporaryException.class)
    public void shouldThrowTemporaryErrorOn500() throws Exception {
        fakeOkHttpInterceptor.addResponse(500);

        remoteRepo.subscribe(createTestSubscriptionRequest());
    }

    @Test(expected = TemporaryException.class)
    public void shouldThrowTemporaryErrorOnInner500() throws Exception {
        fakeOkHttpInterceptor.addResponse(200, load("batch_subscribe_response_500_201.json"));

        remoteRepo.subscribe(createTestSubscriptionRequest());
    }

    @Test(expected = PermanentException.class)
    public void shouldThrowTemporaryErrorOnInner400() throws Exception {
        fakeOkHttpInterceptor.addResponse(200, load("batch_subscribe_response_201_400.json"));

        remoteRepo.subscribe(createTestSubscriptionRequest());
    }

    @Test(expected = TemporaryException.class)
    public void shouldThrowTemporaryErrorOn500OnShortMessage() throws Exception {
        fakeOkHttpInterceptor.addResponse(500);

        remoteRepo.subscribe(createTestSubscriptionRequest());
    }

    @Test(expected = PermanentException.class)
    public void shouldThrowPermanentErrorOn400() throws Exception {
        fakeOkHttpInterceptor.addResponse(404);

        remoteRepo.subscribe(createTestSubscriptionRequest());
    }

    @Test(expected = PermanentException.class)
    public void shouldThrowPermanentErrorOn400OnShortMessage() throws Exception {
        fakeOkHttpInterceptor.addResponse(404);

        remoteRepo.subscribe(createTestSubscriptionRequest());
    }

    @Test
    public void shouldInvokeWebdavSubscribe() throws Exception {
        fakeOkHttpInterceptor.addResponse(200, load("batch_subscribe_response.json"));

        final SubscriptionRequest request = createTestSubscriptionRequest();
        remoteRepo.subscribe(request);

        verify(mockWebdavClient).subscribe(request.getWebdavToken(),
                Collections.singletonList("fileId"));
    }

    private static SubscriptionRequest createTestSubscriptionRequest() {
        return SubscriptionRequest.create("regId", "INSTANCE", MessagingCloud.GCM,
            Arrays.asList("DB1", "DB2"),
            Collections.singletonList("fileId"));
    }

}
