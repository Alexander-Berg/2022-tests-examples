package ru.yandex.disk.feed;

import com.yandex.disk.rest.json.Resource;
import okhttp3.Request;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import ru.yandex.disk.remote.FeedApi;
import ru.yandex.disk.remote.BaseRemoteRepoMethodTest;
import ru.yandex.disk.remote.DateFormat;
import ru.yandex.disk.remote.ResourcesApi;
import ru.yandex.disk.remote.User;
import ru.yandex.disk.remote.exceptions.NotFoundException;
import ru.yandex.disk.remote.exceptions.PermanentException;
import ru.yandex.disk.remote.exceptions.TemporaryException;
import ru.yandex.disk.util.URLUtil2;
import rx.observables.BlockingObservable;
import rx.observers.TestObserver;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static ru.yandex.disk.remote.RemoteRepoTestHelper.load;

@RunWith(RobolectricTestRunner.class)
public class RemoteRepoGetFeedBlockItemsTest extends BaseRemoteRepoMethodTest {

    @Test
    public void shouldBuilderCorrectRequest() throws Exception {
        fakeOkHttpInterceptor.addResponse(load("lenta_block_items.json"));
        final long time = System.currentTimeMillis();

        remoteRepo.getFeedBlockItems("blockId", "", time, time, 0, 2,
                "", "uid").toBlocking().value();

        final Request request = fakeOkHttpInterceptor.getRequest();
        assertThat(request.method(), equalTo("GET"));
        assertThat(request.url().url().getPath(), equalTo(FeedApi.GET_BLOCK_ITEMS));
        final Map<String, String> query = fakeOkHttpInterceptor.getRequestQuery();
        assertThat(query.get(FeedApi.RESOURCE_ID), equalTo("blockId"));
        assertThat(query.get(FeedApi.MODIFIED_GTE),
                equalTo(URLUtil2.encode(DateFormat.asString(time))));

        assertThat(query.get(FeedApi.MODIFIER_UID), equalTo("uid"));
        //TODO add projection MOBDISK-8524
        //assertThat(query.get(FeedApi.FIELDS, equalTo("")));
    }

    @Test
    public void shouldParseItems() throws Exception {
        //data from
        //curl -k 'https://api01e.dsp.yandex.net/v1/disk/lenta/resources?resource_id=69194959:f31f4664d32decc11d9fb65a42df12c231b4737e463bf6902edd6746d3caa550&media_type=image&modified_gte=2016-09-20T11:53:58+03:00&modified_lte=2016-09-21T11:53:58+03:00&offset=0&limit=6&lenta_block_id=69194959%3Af31f4664d32decc11d9fb65a42df12c231b4737e463bf6902edd6746d3caa550%3Aimage%3A69194959&modify__uid=69194950' -H 'Authorization: OAuth AQAAAAAJZNeyAAIIiAfEQga04kJUtPiyqByHQ1E' | prettyjson
        fakeOkHttpInterceptor.addResponse(load("lenta_block_items.json"));
        final ResourcesApi.UsersResources response = invokeMethodSingle();

        final List<Resource> items = response.getResourceList().getItems();
        assertThat(items.size(), equalTo(6));

        final Resource item = items.get(0);
        assertThat(item.getMd5(), equalTo("ee864558dfdfebaaf08c4313ca8730af"));
        assertThat(item.getMimeType(), equalTo("image/jpeg"));
        assertThat(item.getCreated().getTime(),
                equalTo(DateFormat.asLong("2016-09-19T12:09:11+00:00")));
        assertThat(item.getModified().getTime(),
                equalTo(DateFormat.asLong("2016-09-20T09:23:59+00:00")));
        assertThat(item.getName(), equalTo("summer paths 2k.jpg"));
        assertThat(item.getPath().getPath(),
                equalTo("/Общее дело/Новая папка/summer paths 2k.jpg"));
        assertThat(item.getSize(), equalTo(2931424L));
        assertThat(item.getMediaType(), equalTo("image"));
        assertThat(item.getType(), equalTo("file"));
    }

    @Test
    public void shouldParseUsers() throws Exception {
        fakeOkHttpInterceptor.addResponse(load("lenta_block_items_users.json"));
        final ResourcesApi.UsersResources response = invokeMethodSingle();
        final List<User> users = response.getUsers();
        assertThat(users.size(), equalTo(2));

        final User user1 = users.get(0);
        assertThat(user1.getLogin(), equalTo("user1"));
        assertThat(user1.getUid(), equalTo("1"));
    }

    @Test
    public void shouldReturnEmptyUsersIfNotPresentedInResponse() throws Exception {
        fakeOkHttpInterceptor.addResponse(load("lenta_block_items_no_users.json"));
        final ResourcesApi.UsersResources response = invokeMethodSingle();
        final List<User> users = response.getUsers();
        assertThat(users.size(), equalTo(0));
    }

    @Test
    public void shouldContentBlockInfo() throws Exception {
        fakeOkHttpInterceptor.addResponse(load("lenta_huge_block.json"));
        final ResourcesApi.UsersResources response = invokeMethodSingle();

        assertThat(response.getTotalCount(), equalTo(43));
    }

    @Test
    public void shouldThrowNotFoundExceptionOn404() throws Exception {
        fakeOkHttpInterceptor.addResponse(404, "no found");
        final TestObserver<ResourcesApi.UsersResources> observer = new TestObserver<>();

        invokeMethod().subscribe(observer);

        assertThat(observer.getOnErrorEvents().get(0), instanceOf(NotFoundException.class));
    }

    @Test
    public void shouldThrowTemporaryExceptionOn503() throws Exception {
        fakeOkHttpInterceptor.addResponse(503, "server is sick");
        final TestObserver<ResourcesApi.UsersResources> observer = new TestObserver<>();

        invokeMethod().subscribe(observer);

        assertThat(observer.getOnErrorEvents().get(0), instanceOf(TemporaryException.class));
    }

    @Test
    public void shouldThrowTemporaryExceptionOnIOException() throws Exception {
        fakeOkHttpInterceptor.throwIOException();
        final TestObserver<ResourcesApi.UsersResources> observer = new TestObserver<>();

        invokeMethod().subscribe(observer);

        assertThat(observer.getOnErrorEvents().get(0), instanceOf(TemporaryException.class));
    }

    @Test
    public void shouldThrowPermanentExceptionOnSeverIOException() throws Exception {
        fakeOkHttpInterceptor.addResponse(400);
        final TestObserver<ResourcesApi.UsersResources> observer = new TestObserver<>();

        invokeMethod().subscribe(observer);

        assertThat(observer.getOnErrorEvents().get(0), instanceOf(PermanentException.class));
    }


    @Test(expected = TemporaryException.class)
    public void shouldThrowTemporaryExceptionBadResponse() throws Throwable {
        fakeOkHttpInterceptor.addBadResponse();

        final TestObserver<ResourcesApi.UsersResources> observer = new TestObserver<>();

        final BlockingObservable<ResourcesApi.UsersResources> single = invokeMethod();
        single.subscribe(observer);

        throw observer.getOnErrorEvents().get(0);
    }

    private ResourcesApi.UsersResources invokeMethodSingle() {
        return invokeMethod().first();
    }

    private BlockingObservable<ResourcesApi.UsersResources> invokeMethod() {
        return remoteRepo.getFeedBlockItems("", "", 0, 0, 0, 2, "", "uid").toObservable().toBlocking();
    }
}