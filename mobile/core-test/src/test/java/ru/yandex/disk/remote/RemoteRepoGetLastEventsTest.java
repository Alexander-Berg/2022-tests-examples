package ru.yandex.disk.remote;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import ru.yandex.disk.DiskItem;
import ru.yandex.disk.recent.RecentEvent;
import ru.yandex.disk.recent.RecentEventGroup;
import ru.yandex.disk.util.MediaTypes;
import ru.yandex.disk.util.URLUtil2;

import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static ru.yandex.disk.remote.RemoteRepoTestHelper.load;

@RunWith(RobolectricTestRunner.class)
public class RemoteRepoGetLastEventsTest extends BaseRemoteRepoMethodTest {

    private String body;
    private TimeZone defaultTimeZone;

    //TODO request only needed fields
    //TODO request only needed event-types

    @Override
    public void setUp() throws Exception {
        super.setUp();
        defaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("GTM"));
        DateFormat.reset();
        body = load("recent_get_last_events.json");
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        TimeZone.setDefault(defaultTimeZone);
    }

    @Test
    public void shouldSendCorrectRequest() throws Exception {
        fakeOkHttpInterceptor.addResponse(body);
        String pageLoadTime = "2100-01-01T00:00:00+00:00";
        remoteRepo.getLastEvents(null, pageLoadTime, 0, 10, 10).toBlocking().last();

        Map<String, String> query = fakeOkHttpInterceptor.getRequestQuery();
        assertThat(query.get("page_load_date"), equalTo(URLUtil2.encode(pageLoadTime)));
        assertThat(query.get("event_type"), equalTo("fs-store,fs-store-photostream,fs-move,"
                        + "fs-rename,fs-copy,fs-store-download,fs-aviary-render,fs-store-update,"
                        + "fs-set-public,fs-store-office,social-import"));
    }

    @Test
    public void shouldParseAllGroups() throws Exception {
        fakeOkHttpInterceptor.addResponse(body);
        List<RecentEventGroup> groups = invokeMethodAndGetList();
        assertThat(groups.size(), equalTo(5));
    }

    @Test
    public void shouldParseGroupFields() throws Exception {
        fakeOkHttpInterceptor.addResponse(body);
        List<RecentEventGroup> groups = invokeMethodAndGetList();
        RecentEventGroup group = groups.get(1);
        assertThat(group.getEvents().size(), equalTo(3));
        assertThat(group.getMinDate(), equalTo(DateFormat.asLong("2016-01-18T13:08:10+00:00")));
        assertThat(group.getMaxDate(), equalTo(DateFormat.asLong("2016-01-18T13:27:34+00:00")));
        assertThat(group.getCount(), equalTo(3));
    }

    @Test
    public void shouldParseEventFields() throws Exception {
        fakeOkHttpInterceptor.addResponse(body);
        List<RecentEventGroup> groups = invokeMethodAndGetList();
        RecentEventGroup group = groups.get(1);
        RecentEvent event = group.getEvents().get(0);
        assertThat(event.getGroupPath(), equalTo("/disk/docs/mydoc.txt"));
        assertThat(event.getPath().getPath(), equalTo("/disk/docs/mydoc.txt"));
        assertThat(event.getDiskItem(), notNullValue());
    }

    @Test
    public void shouldParseDiskItem() throws Exception {
        fakeOkHttpInterceptor.addResponse(body);
        List<RecentEventGroup> groups = invokeMethodAndGetList();
        RecentEventGroup group = groups.get(1);
        RecentEvent event = group.getEvents().get(0);
        assertThat(event.getMediaType(), equalTo("document"));
        DiskItem text = event.getDiskItem();
        assertThat(text.getPath(), equalTo("/disk/docs/mydoc.txt"));
        assertThat(text.getDisplayName(), equalTo("mydoc.txt"));
        assertThat(text.getLastModified(), equalTo(DateFormat.asLong("2016-01-18T13:28:16+00:00")));
        assertThat(text.getETag(), equalTo("09c569393fadd7d11e42925698e63c1b"));
        assertThat(text.getMediaType(), equalTo(MediaTypes.DOCUMENT));
        assertThat(text.getMimeType(), equalTo("text/plain"));
        assertThat(text.getSize(), equalTo(17L));


        //TODO These are not all fields, but is it necessary to request other feilds?
    }

    @Test
    public void shouldParseDeadItem() throws Exception {
        fakeOkHttpInterceptor.addResponse(body);
        List<RecentEventGroup> groups = invokeMethodAndGetList();
        RecentEventGroup group = groups.get(1);

        assertThat(group.getEvents().get(0).getDiskItem(), notNullValue());
        RecentEvent dead = group.getEvents().get(1);
        assertThat(dead.getDiskItem(), nullValue());
        assertThat(dead.getMediaType(), equalTo(MediaTypes.DOCUMENT));
    }

    @Test
    public void shouldParseAllSupportedEventTypes() {
        fakeOkHttpInterceptor.addResponse(body);
        List<RecentEventGroup> groups = invokeMethodAndGetList();
        for (RecentEventGroup group : groups) {
            for (RecentEvent event : group.getEvents()) {
                assertThat(event.getGroupPath(), notNullValue());
                assertThat(event.getPath(), notNullValue());
            }
        }
    }

    private List<RecentEventGroup> invokeMethodAndGetList() {
        String pageLoadDate = "2100-01-01T00:00:00+00:00";
        return remoteRepo.getLastEvents(null, pageLoadDate, 0, 10, 10)
                .toBlocking().single().getGroups();
    }
}
