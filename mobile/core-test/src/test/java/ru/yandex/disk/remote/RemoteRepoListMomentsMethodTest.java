package ru.yandex.disk.remote;

import okhttp3.Request;
import org.junit.Test;
import ru.yandex.disk.photoslice.IndexChange;
import ru.yandex.disk.remote.exceptions.PermanentException;
import ru.yandex.disk.remote.exceptions.TemporaryException;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static ru.yandex.disk.remote.RemoteRepoTestHelper.load;

public class RemoteRepoListMomentsMethodTest extends BaseRemoteRepoMethodTest {
    private static final ListCollector<PhotosliceApi.Moment> STUB = new ListCollector<>();

    @Test
    public void shouldBuildCorrectGetRequest() throws Exception {
        final PhotosliceTag photosliceTag =
            new PhotosliceTag("Z9nb032EaIHwvlh569194959UhIt5YvcLrb4jJhKf1428070131533", "16");
        fakeOkHttpInterceptor.addResponse(200, load("photoslice_index.json"));

        remoteRepo.listMoments(photosliceTag, STUB);

        final Request request = fakeOkHttpInterceptor.getRequest();
        assertThat(request.url().url().getPath(), equalTo("/v1/disk/photoslice/"
                + "Z9nb032EaIHwvlh569194959UhIt5YvcLrb4jJhKf1428070131533"));

        final Map<String, String> query = fakeOkHttpInterceptor.getRequestQuery();
        assertThat(query.get("revision"), equalTo("16"));
        assertThat(query.get("fields"), equalTo("index.items"));
    }

    @Test
    public void shouldGetMoments() throws Exception {
        final PhotosliceTag photosliceTag = newPhotosliceTag();
        fakeOkHttpInterceptor.addResponse(200, load("photoslice_index.json"));

        final ListCollector<PhotosliceApi.Moment> moments = new ListCollector<>();
        remoteRepo.listMoments(photosliceTag, moments);

        assertThat(moments.size(), equalTo(3));
    }

    @Test
    public void shouldParseMoment0() throws Exception {
        final PhotosliceTag photosliceTag = newPhotosliceTag();
        fakeOkHttpInterceptor.addResponse(200, load("photoslice_index_0.json"));

        final ListCollector<PhotosliceApi.Moment> moments = new ListCollector<>();
        remoteRepo.listMoments(photosliceTag, moments);

        final IndexChange moment = moments.get(0).convertToInsertChange();
        assertThat(moment, is(notNullValue()));
    }

    @Test(expected = TemporaryException.class)
    public void shouldThrowTemporaryExceptionOnIOException() throws Exception {
        fakeOkHttpInterceptor.throwIOException();

        remoteRepo.listMoments(newPhotosliceTag(), STUB);
    }

    @Test(expected = PermanentException.class)
    public void shouldThrowPermanentExceptionOnAnyUnknownCode() throws Exception {
        fakeOkHttpInterceptor.addResponse(409);

        remoteRepo.listMoments(newPhotosliceTag(), STUB);
    }

    @Test
    public void shouldNormallyWorkWithEmptyPhotoslice() throws Exception {
        fakeOkHttpInterceptor.addResponse(200, "{}");

        final ListCollector<PhotosliceApi.Moment> changes = new ListCollector<>();
        remoteRepo.listMoments(newPhotosliceTag(), changes);

        assertThat(changes, is(empty()));
    }


    private static PhotosliceTag newPhotosliceTag() {
        return new PhotosliceTag("1", "2");
    }

}
