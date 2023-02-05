package ru.yandex.disk.remote;

import org.junit.Test;
import ru.yandex.disk.remote.exceptions.PermanentException;
import ru.yandex.disk.remote.exceptions.TemporaryException;

import static org.hamcrest.Matchers.*;
import static ru.yandex.disk.remote.MockResponses.*;

public class RemoteRepoGetOperationStatusMethodTest extends BaseRemoteRepoMethodTest {

    @Test
    public void shouldBuildCorrectRequest() throws Exception {
        fakeOkHttpInterceptor.addResponse(200, RESPONSE_SUCCESS);

        remoteRepo.getOperationStatus("123");

        final String path = fakeOkHttpInterceptor.getRequest().url().url().getPath();
        assertThat(path, equalTo("/v1/disk/operations/123"));
    }


    @Test
    public void shouldReturnOKIfSuccessResponse() throws Exception {
        fakeOkHttpInterceptor.addResponse(200, RESPONSE_SUCCESS);

        final OperationStatus status = remoteRepo.getOperationStatus("123");

        assertThat(status, equalTo(OperationStatus.OK));
    }

    @Test
    public void shouldReturnInProgressIfInProgressResponse() throws Exception {
        fakeOkHttpInterceptor.addResponse(200, RESPONSE_IN_PROGRESS);

        final OperationStatus status = remoteRepo.getOperationStatus("123");

        assertThat(status, not(equalTo(OperationStatus.OK)));
        assertThat(status.getId(), equalTo("123"));
    }

    @Test(expected = TemporaryException.class)
    public void shouldThrowTemporaryExceptionOnIOException() throws Exception {
        fakeOkHttpInterceptor.throwIOException();

        remoteRepo.getOperationStatus("123");
    }

    @Test(expected = TemporaryException.class)
    public void shouldThrowTemporaryExceptionOn5XX() throws Exception {
        fakeOkHttpInterceptor.addResponse(500);

        remoteRepo.getOperationStatus("123");
    }

    @Test(expected = PermanentException.class)
    public void shouldPermanentExceptionOnAnyCodeExcepting5XX() throws Exception {
        fakeOkHttpInterceptor.addResponse(499);

        remoteRepo.getOperationStatus("123");
    }

    @Test(expected = PermanentException.class)
    public void shouldPermanentExceptionOnIncorrectResponseBody() throws Exception {
        fakeOkHttpInterceptor.addResponse(200, "blabla");

        remoteRepo.getOperationStatus("123");
    }

    @Test(expected = PermanentException.class)
    public void shouldPermanentExceptionIfUnknownStatus() throws Exception {
        fakeOkHttpInterceptor.addResponse(200, RESPONSE_UNKNOWN);

        remoteRepo.getOperationStatus("123");
    }

}