package ru.auto.ara;

import org.junit.Before;
import rx.plugins.RxJavaHooks;
import rx.schedulers.Schedulers;

/**
 * @author aleien on 13.04.17.
 */

public abstract class RxTest {
    private static boolean isInitialSetup = true;

    @Before
    public void setupRxJavaSchedulers() {
        RxSetupper.setupRxJavaSchedulers();
    }

    public static class RxSetupper {
        public static void setupRxJavaSchedulers() {
            if (isInitialSetup) {
                // Setup to replace all schedulers with Schedulers.immediate()
                RxJavaHooks.setOnIOScheduler(scheduler -> Schedulers.immediate());
                RxJavaHooks.setOnComputationScheduler(scheduler -> Schedulers.immediate());
                RxJavaHooks.setOnNewThreadScheduler(scheduler -> Schedulers.immediate());

                isInitialSetup = false;
            }
        }
    }
}
