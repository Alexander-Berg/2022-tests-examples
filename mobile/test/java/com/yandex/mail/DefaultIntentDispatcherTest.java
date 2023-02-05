package com.yandex.mail;

import android.content.Context;
import android.content.Intent;

import org.junit.Test;

import androidx.fragment.app.Fragment;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultIntentDispatcherTest {

    private static final int ANY_REQUEST_CODE = 1;

    @Test
    public void startActivity_shouldStartActivityIfThereAreNoInterceptors() {
        Context context = mock(Context.class);
        Intent intent = mock(Intent.class);
        new DefaultIntentDispatcher().startActivity(context, intent);
        verify(context).startActivity(intent);
    }

    @Test
    public void startActivity_shouldStartActivityIfNoInterceptorHandledAction() {
        Context context = mock(Context.class);
        Intent intent = mock(Intent.class);
        DefaultIntentDispatcher dispatcher = new DefaultIntentDispatcher();

        IntentDispatcher interceptor = mock(IntentDispatcher.class);
        when(interceptor.startActivity(context, intent)).thenReturn(false);
        dispatcher.addInterceptor(interceptor);

        dispatcher.startActivity(context, intent);
        verify(interceptor).startActivity(context, intent);
        verify(context).startActivity(intent);
    }

    @Test
    public void startActivity_shouldNotStartActivityIfInterceptorHandlesAction() {
        Context context = mock(Context.class);
        Intent intent = mock(Intent.class);
        DefaultIntentDispatcher dispatcher = new DefaultIntentDispatcher();

        IntentDispatcher interceptor = mock(IntentDispatcher.class);
        when(interceptor.startActivity(context, intent)).thenReturn(true);
        dispatcher.addInterceptor(interceptor);

        dispatcher.startActivity(context, intent);
        verify(interceptor).startActivity(context, intent);
        verify(context, never()).startActivity(intent);
    }

    @Test
    public void startActivityForResult_shouldStartActivityIfThereAreNoInterceptors() {
        Fragment fragment = mock(Fragment.class);
        Intent intent = mock(Intent.class);
        new DefaultIntentDispatcher().startActivityForResult(fragment, intent, ANY_REQUEST_CODE);
        verify(fragment).startActivityForResult(intent, ANY_REQUEST_CODE);
    }

    @Test
    public void startActivityForResult_shouldStartActivityIfNoInterceptorHandledAction() {
        Fragment fragment = mock(Fragment.class);
        Intent intent = mock(Intent.class);
        DefaultIntentDispatcher dispatcher = new DefaultIntentDispatcher();

        IntentDispatcher interceptor = mock(IntentDispatcher.class);
        when(interceptor.startActivityForResult(fragment, intent, ANY_REQUEST_CODE)).thenReturn(false);
        dispatcher.addInterceptor(interceptor);

        dispatcher.startActivityForResult(fragment, intent, ANY_REQUEST_CODE);
        verify(interceptor).startActivityForResult(fragment, intent, ANY_REQUEST_CODE);
        verify(fragment).startActivityForResult(intent, ANY_REQUEST_CODE);
    }

    @Test
    public void startActivityForResult_shouldNotStartActivityIfInterceptorHandlesAction() {
        Fragment fragment = mock(Fragment.class);
        Intent intent = mock(Intent.class);
        DefaultIntentDispatcher dispatcher = new DefaultIntentDispatcher();

        IntentDispatcher interceptor = mock(IntentDispatcher.class);
        when(interceptor.startActivityForResult(fragment, intent, ANY_REQUEST_CODE)).thenReturn(true);
        dispatcher.addInterceptor(interceptor);

        dispatcher.startActivityForResult(fragment, intent, ANY_REQUEST_CODE);
        verify(interceptor).startActivityForResult(fragment, intent, ANY_REQUEST_CODE);
        verify(fragment, never()).startActivityForResult(intent, ANY_REQUEST_CODE);
    }
}
