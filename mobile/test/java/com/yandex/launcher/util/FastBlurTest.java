package com.yandex.launcher.util;

import android.graphics.Bitmap;

import com.yandex.launcher.common.util.BitmapUtils;
import com.yandex.launcher.BaseRobolectricTest;

import org.junit.Test;

import androidx.annotation.NonNull;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FastBlurTest extends BaseRobolectricTest {

    public FastBlurTest() throws NoSuchFieldException, IllegalAccessException {
    }

    @Test
    public void testForExceptions() throws Exception {
        BitmapUtils.fastblur(createBitmap(4, 4), 2);
        BitmapUtils.fastblur(createBitmap(32, 32), 32);
        BitmapUtils.fastblur(createBitmap(1280, 1920), 50);
        BitmapUtils.fastblur(createBitmap(1280, 1920), 100);
        BitmapUtils.fastblur(createBitmap(1280, 1920), 1165);
    }

    @NonNull
    private Bitmap createBitmap(int w, int h) {
        Bitmap bmp = mock(Bitmap.class);
        when(bmp.copy(null, true)).thenReturn(bmp);
        when(bmp.getWidth()).thenReturn(w);
        when(bmp.getHeight()).thenReturn(h);
        return bmp;
    }

}
