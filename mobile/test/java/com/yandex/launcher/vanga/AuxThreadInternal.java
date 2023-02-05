package com.yandex.launcher.vanga;
import com.yandex.launcher.common.app.AuxThread;

public class AuxThreadInternal {

    /**
     * Use in setUp() method if your tests uses AuxHandler.createHandler();
     */
    public static void restart() {
        AuxThread.getInstance().restartForTest();
    }
}
