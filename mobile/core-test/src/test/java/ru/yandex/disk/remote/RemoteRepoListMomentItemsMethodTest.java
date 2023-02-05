package ru.yandex.disk.remote;

import okhttp3.Request;
import org.junit.Test;
import ru.yandex.disk.photoslice.ItemChange;
import ru.yandex.disk.remote.exceptions.PermanentException;
import ru.yandex.disk.remote.exceptions.TemporaryException;
import ru.yandex.disk.test.TestUnits;

import java.util.Map;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.disk.remote.RemoteRepoTestHelper.load;

public class RemoteRepoListMomentItemsMethodTest extends BaseRemoteRepoMethodTest {

    private static final Collector<ItemChange> STUB = new Collector<>();

    static {
        TestUnits.setDefaultCharsetLikeOnTeamcity();
    }

    @Test
    public void shouldBuilderCorrectRequest() throws Exception {
        fakeOkHttpInterceptor.addResponse(200, load("photoslice_moment_items.json"));
        remoteRepo.listMomentItems(new PhotosliceTag("photosliceId", "revision"),
                singletonList("momentId"), STUB);

        final Request request = fakeOkHttpInterceptor.getRequest();
        assertThat(request.method(), equalTo("GET"));
        assertThat(request.url().url().getPath(), equalTo("/v1/disk/photoslice/photosliceId"));
        final Map<String, String> query = fakeOkHttpInterceptor.getRequestQuery();
        assertThat(query.get("revision"), equalTo("revision"));
        assertThat(query.get("fields"), equalTo("clusters.items"));
        assertThat(query.get("cluster_ids"), equalTo("momentId"));
    }

    @Test
    public void shouldGetMoments() throws Exception {
        fakeOkHttpInterceptor.addResponse(200, load("photoslice_moment_items.json"));

        final Collector<ItemChange> items = new Collector<>();
        remoteRepo.listMomentItems(newPhotosliceTag(), singletonList("3"), items);

        assertThat(items.size(), equalTo(4));
    }

    @Test(expected = TemporaryException.class)
    public void shouldThrowTemporaryExceptionOnIOException() throws Exception {
        fakeOkHttpInterceptor.throwIOException();

        remoteRepo.listMomentItems(newPhotosliceTag(), singletonList("3"), STUB);
    }

    @Test(expected = PermanentException.class)
    public void shouldThrowPermanentExceptionOnAnyUnknownCode() throws Exception {
        fakeOkHttpInterceptor.addResponse(409);

        remoteRepo.listMomentItems(newPhotosliceTag(), singletonList("3"), STUB);
    }

    private PhotosliceTag newPhotosliceTag() {
        return new PhotosliceTag("1", "2");
    }

}