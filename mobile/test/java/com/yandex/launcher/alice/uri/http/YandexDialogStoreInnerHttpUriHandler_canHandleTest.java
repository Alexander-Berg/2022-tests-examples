package com.yandex.launcher.alice.uri.http;

import android.net.Uri;
import androidx.test.core.app.ApplicationProvider;
import com.yandex.launcher.alice.component.LauncherYandexSearchProvider;
import com.yandex.launcher.test.TestApplication;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(ParameterizedRobolectricTestRunner.class)
@Config(sdk = 26, manifest = Config.NONE, packageName = "com.yandex.launcher", application = TestApplication.class)
public class YandexDialogStoreInnerHttpUriHandler_canHandleTest {

    @ParameterizedRobolectricTestRunner.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[] { "https://dialogs.yandex.ru/store", true },
                             new Object[] { "https://dialogs.test.voicetech.yandex.ru/store", true },
                             new Object[] { "https://dialogs.google.ru/store", false },
                             new Object[] { "https://yandex.ru/store", false },
                             new Object[] { "https://yandex.ru/", false },
                             new Object[] { "https://google.ru/", false });
    }

    private final String mUriString;
    YandexDialogStoreInnerHttpUriHandler mHandler;
    Uri mUri;
    boolean mCanBeHandled;

    public YandexDialogStoreInnerHttpUriHandler_canHandleTest(String uriString, boolean canBeHandled) {
        mUriString = uriString;
        mCanBeHandled = canBeHandled;
    }

    @Before
    public void setUp() {
        mHandler = new YandexDialogStoreInnerHttpUriHandler(ApplicationProvider.getApplicationContext(),
                new LauncherYandexSearchProvider());
        mUri = Uri.parse(mUriString);
    }

    @Test
    public void checkHandlerCanHandleDialogUri_uriCanBeHandled() {
        boolean canBeHandled = mHandler.canHandle(mUri);

        Assert.assertEquals("Can't handle URI: " + mUriString, mCanBeHandled, canBeHandled);
    }
}
