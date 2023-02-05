package com.yandex.launcher;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import com.yandex.launcher.common.app.ApplicationStateMonitor;
import com.yandex.launcher.common.app.IdleStateController;
import com.yandex.launcher.common.metrica.CommonMetricaFacade;
import com.yandex.launcher.common.util.DeviceUtils;
import com.yandex.launcher.common.util.ExternalContextFactory;
import com.yandex.launcher.app.AuxThreadInternal;
import com.yandex.launcher.app.TestApplication;
import com.yandex.launcher.preferences.PreferencesManager;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import java.io.InputStream;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {21}, application = TestApplication.class, packageName = "com.yandex.launcher")
public abstract class BaseRobolectricTest {

    private IdleStateController idleStateController;

    public BaseRobolectricTest() {
        ShadowLog.stream = System.out;
    }

    @Before
    public void setUp() throws Exception {
        AuxThreadInternal.restart();
        ApplicationStateMonitor.init(getAppContext());
        CommonMetricaFacade.init(getAppContext(), CommonMetricaFacade.TYPE_COMMON);
        ExternalContextFactory.init(getAppContext());
        PreferencesManager.init(getAppContext());
        DeviceUtils.init(getAppContext());

        idleStateController = IdleStateController.getInstance();
        if (idleStateController == null) {
            idleStateController = new IdleStateController(getAppContext());
        }
        TestCommonAppState.init(getAppContext());
    }

    @After
    public void tearDown() {
        if (idleStateController != null) {
            idleStateController.onTerminate();
        }
    }

    protected Context getAppContext() {
        return ApplicationProvider.getApplicationContext();
    }

    protected InputStream getResourceInputStream(String fileName) {
        return this.getClass().getClassLoader().getResourceAsStream(fileName);
    }

    protected static void setTime(long time) {
        final int tryCount = 5;
        for (int j = 0; j < tryCount; j++) {
            try {
                Robolectric.getForegroundThreadScheduler().advanceTo(time);
            } catch (Exception e) {
                if (j == tryCount-1) {
                    throw e;
                } else {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e2) {
                        // skip
                    }
                }
            }
        }
    }
}
