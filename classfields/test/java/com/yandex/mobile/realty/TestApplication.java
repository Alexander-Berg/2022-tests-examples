package com.yandex.mobile.realty;

import com.yandex.mobile.verticalcore.AppConfig;
import com.yandex.mobile.verticalcore.BaseApplication;

/**
 *
 * @author ironbcc on 29.04.2015.
 */
public class TestApplication extends BaseApplication {

    @Override
    protected AppConfig createMainProcessConfig() {
        return AppConfig.build();
    }

    @Override
    protected String getAppName() {
        return BuildConfig.APPLICATION_ID;
    }
}
