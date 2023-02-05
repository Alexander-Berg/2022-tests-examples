package ru.yandex.disk.remote;

import okhttp3.Request;
import org.junit.Test;
import ru.yandex.disk.remote.exceptions.PermanentException;
import ru.yandex.disk.remote.exceptions.TemporaryException;

import static org.hamcrest.Matchers.*;
import static ru.yandex.disk.remote.RemoteRepoTestHelper.load;

public class RemoteRepoInitPhotosliceMethodTest extends BaseRemoteRepoMethodTest {
    @Test
    public void shouldBuildCorrectInitRequest() throws Exception {
        fakeOkHttpInterceptor.addResponse(200, load("photoslice_init.json"));

        remoteRepo.initPhotoslice();

        final Request request = fakeOkHttpInterceptor.getRequest();
        assertThat(request.method(), equalTo("POST"));
        assertThat(request.url().url().getPath(), equalTo("/v1/disk/photoslice"));
    }

    @Test
    public void shouldReturnPhotosliceTag() throws Exception {
        fakeOkHttpInterceptor.addResponse(200, load("photoslice_init.json"));

        final PhotosliceTag tag = remoteRepo.initPhotoslice();

        assertThat(tag.getId(), equalTo("Z9nb032EaIHwvlh569194959UhIt5YvcLrb4jJhKf1428070131533"));
        assertThat(tag.getRevision(), equalTo("16"));
    }

    @Test(expected = TemporaryException.class)
    public void shouldThrowTemporaryExceptionOnIOException() throws Exception {
        fakeOkHttpInterceptor.throwIOException();

        remoteRepo.initPhotoslice();
    }

    @Test(expected = PermanentException.class)
    public void shouldThrowPermanentExceptionOnAnyUnknownCode() throws Exception {
        fakeOkHttpInterceptor.addResponse(409);

        remoteRepo.initPhotoslice();
    }

}
