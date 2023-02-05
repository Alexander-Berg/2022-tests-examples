package com.yandex.launcher.alice.uri.http;

import android.net.Uri;
import com.yandex.launcher.test.TestApplication;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 26, manifest = Config.NONE, packageName = "com.yandex.launcher", application = TestApplication.class)
public class YandexDialogStoreInnerHttpUriHandler_handleUriTest {

    private static final String DIALOG_URL = "https://dialogs.yandex.ru/store";
    private static final String RESULT_DIALOG_URL = "https://dialogs.yandex.ru/store?client=yandex-launcher";

    private YandexDialogStoreInnerHttpUriHandler mUriHandler;
    private ArgumentCaptor<Uri> mUriCaptor;

    @Before
    public void setUp() throws Exception {
        mUriHandler = mock(YandexDialogStoreInnerHttpUriHandler.class);
        doCallRealMethod().when(mUriHandler).handleUri(any(Uri.class));
        mUriCaptor = ArgumentCaptor.forClass(Uri.class);
    }

    @Test
    public void handleDialogUri_getParameterAdded() {
        mUriHandler.handleUri(Uri.parse(DIALOG_URL));

        verify(mUriHandler, times(1)).handleUriInner(mUriCaptor.capture());

        Assert.assertEquals(RESULT_DIALOG_URL, mUriCaptor.getValue().toString());
    }
}
