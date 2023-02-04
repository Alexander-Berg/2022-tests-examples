package com.yandex.mobile.job;

import android.content.Context;

import com.yandex.mobile.job.utils.AppHelper;

import org.junit.Before;
import org.robolectric.Robolectric;

/**
 *
 * @author ironbcc on 28.04.2015.
 */
public abstract class BaseTest {

    protected Context context;

    @Before
    public void baseSetup() {
        context = Robolectric.getShadowApplication().getApplicationContext();
        AppHelper.setupApp(context);
    }
}
