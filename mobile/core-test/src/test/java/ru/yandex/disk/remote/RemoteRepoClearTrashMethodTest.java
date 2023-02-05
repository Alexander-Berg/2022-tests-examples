package ru.yandex.disk.remote;

import org.junit.Test;
import ru.yandex.disk.remote.exceptions.PermanentException;
import ru.yandex.disk.remote.exceptions.TemporaryException;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.disk.remote.RemoteRepoTestHelper.encoded;
import static ru.yandex.disk.remote.RemoteRepoTestHelper.load;

public class RemoteRepoClearTrashMethodTest extends BaseRemoteRepoMethodTest {

    @Test
    public void shouldReturnOKOn204() throws Exception {
        fakeOkHttpInterceptor.addResponse(204);

        OperationStatus status = remoteRepo.clearTrash();

        assertThat(status, equalTo(OperationStatus.OK));
    }

    @Test
    public void shouldReturnPendingOn202() throws Exception {
        fakeOkHttpInterceptor.addResponse(202, load("operation.json"));

        OperationStatus status = remoteRepo.clearTrash();

        assertThat(status.getId(),
                equalTo("740238c77255a520ae963fed3be130f40b0473e424d5dd8b3a8b268ad09e077c"));
    }

    @Test
    public void shouldBuildCorrectRequest() throws Exception {
        fakeOkHttpInterceptor.addResponse(204);

        remoteRepo.clearTrash();

        Map<String, String> query = fakeOkHttpInterceptor.getRequestQuery();
        assertThat(query.get("path"), equalTo(encoded("/")));
    }

    @Test(expected = TemporaryException.class)
    public void shouldTrowTemporaryExceptionOnIOException() throws Exception {
        fakeOkHttpInterceptor.throwIOException();

        remoteRepo.clearTrash();
    }

    @Test(expected = TemporaryException.class)
    public void shouldTrowTemporaryExceptionOn500() throws Exception {
        fakeOkHttpInterceptor.addResponse(500);

        remoteRepo.clearTrash();
    }


    @Test(expected = PermanentException.class)
    public void shouldTrowPermanentExceptionOn400() throws Exception {
        fakeOkHttpInterceptor.addResponse(400);

        remoteRepo.clearTrash();
    }

}