package ru.yandex.disk.remote;

import org.junit.Test;
import ru.yandex.disk.fetchfilelist.SyncException;
import ru.yandex.disk.remote.exceptions.RemoteExecutionException;
import ru.yandex.disk.trash.TrashItem;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static ru.yandex.disk.remote.RemoteRepoTestHelper.*;

public class RemoteRepoListTrashMethodTest extends BaseRemoteRepoMethodTest {
    private static final String body;

    static {
        body = load("list_trash_directory_and_file.json");
    }

    private static final RemoteRepoOnNext<TrashItem, SyncException> STUB_CALLBACK = stubOnNext();

    @Test
    public void shouldConvertApiObjectsToTrashItems() throws Exception {
        fakeOkHttpInterceptor.addResponse(body);

        final Collector<TrashItem> items = new Collector<>();
        remoteRepo.listTrash(wrapCollector(items));

        assertThat(items.size(), equalTo(2));

        final TrashItem dir = items.get(0);
        assertThat(dir.getPath(), equalTo("trash:/Music"));
        assertThat(dir.isDir(), equalTo(true));
        assertThat(dir.getETag(), nullValue());

        final TrashItem file = items.get(1);
        assertThat(file.getPath(), equalTo("trash:/Disk Wallpaper.jpg"));
        assertThat(file.isDir(), equalTo(false));
        assertThat(file.getETag(), equalTo("914fe1f19c06f6aa6c25abf02fa06f80"));
        assertThat(file.getDisplayName(), equalTo("Disk Wallpaper.jpg"));
        assertThat(file.getSize(), equalTo(937199L));
        assertThat(file.getLastModified(), equalTo(time("2013-02-18T11:19:50+00:00")));
        assertThat(file.getMimeType(), equalTo("image/jpeg"));
        // TODO add this line after implementation on server
        // assertThat(file.getMediaType(), equalTo("image"))
        assertThat(file.getHasThumbnail(), equalTo(true));
    }

    @Test(expected = RemoteExecutionException.class)
    public void shouldThrowWebdavExceptionOnIOException() throws Exception {
        fakeOkHttpInterceptor.throwIOException();

        remoteRepo.listTrash(STUB_CALLBACK);
    }

    @Test(expected = RemoteExecutionException.class)
    public void shouldThrowWebdavExceptionOnServerIOException() throws Exception {
        fakeOkHttpInterceptor.addResponse(500);

        remoteRepo.listTrash(STUB_CALLBACK);
    }

    @Test
    public void shouldRequestAllPages() throws Exception {
        fakeOkHttpInterceptor.addResponse(load("list_trash_page_1.json"));
        fakeOkHttpInterceptor.addResponse(load("list_trash_page_2.json"));
        fakeOkHttpInterceptor.addResponse(load("list_trash_page_3.json"));

        final Collector<TrashItem> items = new Collector<>();
        remoteRepo.listTrash(wrapCollector(items));

        assertThat(items.size(), equalTo(5));
        final Map<String, String> secondQuery = fakeOkHttpInterceptor.getRequestQuery(1);
        assertThat(secondQuery.get("offset"), equalTo("2"));

        final Map<String, String> thirdQuery = fakeOkHttpInterceptor.getRequestQuery(2);
        assertThat(thirdQuery.get("offset"), equalTo("4"));
    }

    private RemoteRepoOnNext<TrashItem, SyncException> wrapCollector(final Collector<TrashItem> collector) {
        return new RemoteRepoOnNext<TrashItem, SyncException>() {
            @Override
            public void onNext(final TrashItem item) {
                collector.onNext(item);
            }
        };
    }
}
