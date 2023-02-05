package ru.yandex.market;

import android.content.Context;
import android.os.Build;

import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import ru.yandex.market.rx.schedulers.YSchedulers;
import ru.yandex.market.utils.StreamUtils;

/**
 * Common ancestor for all robolectric-tests
 */
@RunWith(RobolectricTestRunner.class)
@Config(
        sdk = {Build.VERSION_CODES.P},
        application = NoOpMarketApplication.class,
        shadows = {
                ShadowMetricaPushBroadcastReceiver.class,
                ShadowNetwork.class,
                NetworkSecurityPolicyWorkaround.class,
        }
)
/**
 * Этот класс - сборник костылей, который использовали потому, что не умели писать unit-тесты правильно.
 * Просто не используйте его.
 */
@Deprecated
public abstract class BaseTest {

    public BaseTest() {
        YSchedulers.setTestMode();
    }

    protected void fail(String msg) {
        Assert.fail(msg);
    }

    @CallSuper
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    protected Context getRobolectricContext() {
        return ApplicationProvider.getApplicationContext();
    }

    @Nullable
    public String getStringResource(@NonNull final String path) {

        BufferedReader reader = null;
        StringBuilder res = new StringBuilder();

        try {
            URL url = getClass().getResource(path);
            if (url != null) {
                reader = new BufferedReader(new FileReader(url.getPath()));
                String str = reader.readLine();
                while (str != null) {
                    res.append(str);
                    str = reader.readLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            StreamUtils.close(reader);
        }
        if (res.length() == 0) {
            throw new IllegalStateException("File not found " + path);
        }
        return res.toString();
    }
}
