package ru.yandex.disk.feed;

import com.yandex.disk.rest.json.Resource;
import com.yandex.disk.rest.json.ResourceList;
import com.yandex.disk.rest.util.ResourcePath;
import ru.yandex.disk.provider.DiskDatabase;
import ru.yandex.disk.remote.ResourcesApi;
import ru.yandex.disk.remote.User;
import ru.yandex.util.Path;

import javax.annotation.Nonnull;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.*;

public final class MockFeedResources {

    static final String MD5 = "cf3a2784cc9029ec5b9c12bc183c2600";
    static final String MIME = "application/json";
    static final String NAME = "snapshot.json";
    static final String PATH = "/lenta-data/snapshot.json";
    static final String DISK_PATH = new Path(DiskDatabase.ROOT_PATH, PATH).getPath();
    static final String MEDIA = "unknown";
    static final long SIZE = 287136L;
    static final long TIME = System.currentTimeMillis();

    private static final AtomicLong resourceIdCounter = new AtomicLong();

    public static Resource createResource() {
        final Resource resource = mock(Resource.class);
        when(resource.getMd5()).thenReturn(MD5);
        when(resource.getMimeType()).thenReturn(MIME);
        when(resource.getName()).thenReturn(NAME);
        when(resource.getPath()).thenReturn(new ResourcePath("disk", PATH));
        when(resource.getSize()).thenReturn(SIZE);
        when(resource.getMediaType()).thenReturn(MEDIA);
        when(resource.getModified()).thenReturn(new Date(TIME));
        when(resource.getResourceId()).thenReturn("uid:" + resourceIdCounter.incrementAndGet());
        return resource;
    }

    public static ResourcesApi.UsersResources createSingleResource() {
        return createResources(createResource());
    }

    public static ResourcesApi.UsersResources createResources(final Resource... item) {
        final ResourcesApi.UsersResources resource =
                mock(ResourcesApi.UsersResources.class, CALLS_REAL_METHODS);
        final ResourceList list = mock(ResourceList.class);
        when(resource.getResourceList()).thenReturn(list);
        when(resource.getPath()).thenReturn(new ResourcePath("disk", PATH));
        when(resource.getTotalCount()).thenReturn(1);
        when(resource.getModified()).thenReturn(new Date(0));
        when(resource.getResourceId()).thenReturn("uid:" + resourceIdCounter.incrementAndGet());

        final List<User> users = asList(
                createUser("testuser1", "1"),
                createUser("testuser2", "2"),
                createUser("testuser3", "3")
        );
        when(resource.getUsers()).thenReturn(users);
        when(list.getItems()).thenReturn(asList(item));
        return resource;
    }

    @Nonnull
    private static User createUser(final String login, final String uid) {
        final User user = mock(User.class);
        when(user.getLogin()).thenReturn(login);
        when(user.getUid()).thenReturn(uid);
        return user;
    }
}