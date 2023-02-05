package ru.yandex.disk.provider;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.database.AbstractCursor;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import com.google.common.base.Function;
import org.robolectric.shadows.ShadowContentResolver;
import ru.yandex.disk.test.AndroidTestCase2;
import ru.yandex.disk.test.CursorTracker;
import ru.yandex.disk.test.CursorTrackers;
import ru.yandex.disk.test.SeclusiveContext;

import java.util.List;

import static com.google.common.collect.Lists.transform;
import static org.robolectric.Shadows.shadowOf;

public abstract class ProviderTestCase3<T extends ContentProvider> extends AndroidTestCase2 {

    private ContentResolver resolver;
    private SeclusiveContext providerContext;
    private final Class<T> providerClass;
    private T provider;
    private CursorTracker cursorTracker;
    protected String mockAuthority;

    public ProviderTestCase3(Class<T> providerClass) {
        this.providerClass = providerClass;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        providerContext = new SeclusiveContext(mContext);
        mockAuthority = getMockContext().getApplicationInfo().packageName + ".minidisk";
        resolver = providerContext.getContentResolver();
        provider = providerClass.newInstance();
        provider.attachInfo(providerContext, null);
        cursorTracker = new CursorTracker();

        CursorTrackers.registerProvider(mockAuthority, provider, cursorTracker);
    }

    @Override
    protected void tearDown() throws Exception {
        if (provider != null) {
            provider.shutdown();
        }
        super.tearDown();
    }

    public T getProvider() {
        return provider;
    }

    public SeclusiveContext getMockContext() {
        return providerContext;
    }

    public ContentResolver getContentResolver() {
        return resolver;
    }

    public ContentResolver getMockContentResolver() {
        return resolver;
    }

    protected void assertUriNotified(Uri uri) {
        assertUriNotified(uri, getContentResolver());
    }

    public static void assertUriNotified(Uri uri, ContentResolver contentResolver) {
        ShadowContentResolver cr = shadowOf(contentResolver);
        List<Uri> changedUris = transform(cr.getNotifiedUris(),
                new Function<ShadowContentResolver.NotifiedUri, Uri>() {
                    @Override
                    public Uri apply(ShadowContentResolver.NotifiedUri input) {
                        return input.uri;
                    }
                });
        assertEquals(1, changedUris.size());
        assertTrue(changedUris.contains(uri));
    }

    protected static void assertNotificationUri(Uri expected, Cursor actualCursor) {
        if (actualCursor instanceof AbstractCursor) {
            AbstractCursor absCursor = (AbstractCursor) actualCursor;
            assertEquals(expected, absCursor.getNotificationUri());
        } else {
            CursorWrapper cursorWrapper = (CursorWrapper) actualCursor;
            assertNotificationUri(expected, cursorWrapper.getWrappedCursor());
        }
    }

    public void assertAllCursorClosed() {
        cursorTracker.checkState();
    }

}
