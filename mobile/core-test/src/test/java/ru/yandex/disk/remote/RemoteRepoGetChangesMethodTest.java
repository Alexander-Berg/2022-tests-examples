package ru.yandex.disk.remote;

import okhttp3.Request;
import org.junit.Test;
import ru.yandex.disk.photoslice.Change;
import ru.yandex.disk.photoslice.DeltasUpdated;
import ru.yandex.disk.remote.exceptions.PermanentException;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.disk.remote.RemoteRepoTestHelper.load;

public class RemoteRepoGetChangesMethodTest extends BaseRemoteRepoMethodTest {

    private static final Collector<Change> STUB = new Collector<>();

    @Test
    public void shouldBuilderCorrectRequest() throws Exception {
        fakeOkHttpInterceptor.addResponse(200, load("ps_delta_moment_deleted.json"));

        final PhotosliceTag photosliceTag = new PhotosliceTag(
                "8sb3I4nzKSM6UMpt169909659U7r3FpqC9toUiIwvv1431967009381", "2");

        remoteRepo.getPhotosliceChanges(photosliceTag, STUB);

        final Request request = fakeOkHttpInterceptor.getRequest();
        assertThat(request.method(), equalTo("GET"));
        assertThat(request.url().url().getPath(),
                equalTo("/v1/disk/photoslice/8sb3I4nzKSM6UMpt169909659U7r3" +
                        "FpqC9toUiIwvv1431967009381/deltas"));
        assertThat(fakeOkHttpInterceptor.getRequestQuery().get("base_revision"), equalTo("2"));
    }

    @Test
    public void shouldReturnNewPhotosliceTag() throws Exception {
        fakeOkHttpInterceptor.addResponse(200, load("ps_delta_moment_deleted.json"));

        final PhotosliceTag photosliceTag = new PhotosliceTag("abc", "2");

        final Collector<Change> Changes = new Collector<>();
        final DeltasUpdated result = remoteRepo.getPhotosliceChanges(photosliceTag, Changes);
        final PhotosliceTag newTag = result.getNewTag();

        assertThat(newTag.getId(), equalTo("abc"));
        assertThat(newTag.getRevision(), equalTo("3"));
        assertThat(result.getHasMore(), equalTo(true));
    }

    @Test
    public void shouldParseAllChanges() throws Exception {
        fakeOkHttpInterceptor.addResponse(200, load("ps_delta_two_deltas.json"));

        final Collector<Change> Changes = new Collector<>();
        remoteRepo.getPhotosliceChanges(newPhotosliceTag(), Changes);

        assertThat(Changes.size(), equalTo(4));
    }

    @Test(expected = PermanentException.class)
    public void shouldThrowPermanentExceptionOn404() throws Exception {
        fakeOkHttpInterceptor.addResponse(404);

        remoteRepo.getPhotosliceChanges(newPhotosliceTag(), STUB);
    }

    @Test(expected = PermanentException.class)
    public void shouldThrowPermanentExceptionOn401() throws Exception {
        fakeOkHttpInterceptor.addResponse(401);

        remoteRepo.getPhotosliceChanges(newPhotosliceTag(), STUB);
    }

    @Test(expected = PermanentException.class)
    public void shouldThrowPermanentExceptionOnAnyUnknownCode() throws Exception {
        fakeOkHttpInterceptor.addResponse(409);

        remoteRepo.getPhotosliceChanges(newPhotosliceTag(), STUB);
    }

    private static PhotosliceTag newPhotosliceTag() {
        return new PhotosliceTag("1", "2");
    }

}
