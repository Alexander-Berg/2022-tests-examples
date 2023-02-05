package ru.yandex.disk.test;

import android.content.ContentProvider;
import android.database.Cursor;
import android.net.Uri;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.android.controller.ContentProviderController;

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CursorTrackers {
    public static void registerProvider(String providerAuthority, ContentProvider provider,
                                 final CursorTracker cursorTracker) {
        ContentProvider spy = spy(provider);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Cursor cursor = (Cursor) invocation.callRealMethod();
                cursorTracker.add(cursor);
                return cursor;
            }
        }).when(spy).query(any(Uri.class), any(String[].class), nullable(String.class), any(String[].class),
                nullable(String.class));
        ContentProviderController.of(spy).create(providerAuthority);
    }
}
