package ru.yandex.disk.test;

import android.content.Context;
import org.robolectric.RuntimeEnvironment;

public abstract class AndroidTestCase2 extends TestCase2 {

    protected Context mContext;

    protected void setUp() throws Exception {
        mContext = RuntimeEnvironment.application;
    }

    public Context getMockContext() {
        if (mContext == null) {
            throw new IllegalStateException("call after super.setUp()");
        }
        return mContext;
    }
}
