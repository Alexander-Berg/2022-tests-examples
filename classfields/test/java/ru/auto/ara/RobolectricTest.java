package ru.auto.ara;

import android.app.Application;

import com.yandex.mobile.verticalcore.utils.AppHelper;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import ru.auto.test.runner.AllureRobolectricRunner;

/**
 * Base test setup for running with robolectric + PowerMock test runner
 * Created by aleien on 26.12.16.
 */

@RunWith(AllureRobolectricRunner.class)
@Config(sdk = 23, packageName = "ru.auto.ara.debug", manifest = Config.NONE, application = AutoApplication.class)
public abstract class RobolectricTest extends RxTest {
    private boolean isInitialSetup = true;

    @Before
    public void setupPowerMockito() {
        if (isInitialSetup) {
            // Setup for AppHelper (it's being overriden by PowerMockRule)
            Application app = RuntimeEnvironment.application;
            AppHelper.setupApp(app);
            AutoApplication.mainComponentHolder.initialize();

            isInitialSetup = false;
        }
    }

}
