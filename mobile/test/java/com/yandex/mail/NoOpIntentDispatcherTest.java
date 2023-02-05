package com.yandex.mail;

import android.content.Context;
import android.content.Intent;

import org.junit.Test;

import androidx.fragment.app.Fragment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class NoOpIntentDispatcherTest {

    @Test
    public void startActivity_Context_Intent_shouldReturnFalse() {
        assertThat(new NoOpIntentDispatcher().startActivity(mock(Context.class), mock(Intent.class))).isFalse();
    }

    @Test
    public void startActivityForResult_Fragment_Intent_RequestCode_shouldReturnFalse() {
        assertThat(new NoOpIntentDispatcher().startActivityForResult(mock(Fragment.class), mock(Intent.class), 1)).isFalse();
    }
}
