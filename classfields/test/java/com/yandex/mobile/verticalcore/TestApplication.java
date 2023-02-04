package com.yandex.mobile.verticalcore;

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
