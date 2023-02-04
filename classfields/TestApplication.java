package com.yandex.mobile.verticalapp;

import android.database.sqlite.SQLiteOpenHelper;
import com.google.gson.Gson;
import com.yandex.mobile.vertical.dagger.*;
import com.yandex.mobile.verticalcore.AppConfig;
import com.yandex.mobile.verticalcore.ApplicationCreationCallback;
import com.yandex.mobile.verticalcore.BaseApplication;
import com.yandex.mobile.verticalcore.plugin.LogPlugin;
import com.yandex.mobile.verticalcore.provider.CupboardProvider;
import com.yandex.mobile.verticalcore.provider.CupboardSQLiteOpenHelper2;
import dagger.Component;
import dagger.Module;
import dagger.Provides;
import nl.qbusict.cupboard.Cupboard;

import javax.inject.Singleton;

/**
 *
 * @author ironbcc on 23.04.2015.
 */
public class TestApplication extends BaseApplication implements ComponentProvider {
    private TestAppComponent component;

    @Override
    public void onCreate() {
        component = DaggerTestApplication_TestAppComponent.builder()
            .appContextModule(new AppContextModule(this))
            .build();
        super.onCreate();
    }

    @Override
    protected AppConfig createMainProcessConfig() {
        return AppConfig.build()
            .register(LogPlugin.createDebug());
    }

    @Override
    protected String getAppName() {
        return "com.yandex.mobile.verticalapp";
    }

    @Override
    public ApplicationCreationCallback[] getCreationCallbacks() {
        return component.getCreationCallbacks();
    }

    @Component(modules = {AppContextModule.class, SingletonsModule.class})
    @Singleton
    public interface TestAppComponent extends VerticalComponent {
        SQLiteOpenHelper getSQLiteOpenHelper();

        ApplicationCreationCallback[] getCreationCallbacks();

        void inject(TestActivity activity);
    }

    @Override
    public <T extends VerticalComponent> T getComponent(Class<T> cl) throws IllegalArgumentException {
        if (cl.isAssignableFrom(component.getClass())) {
            return (T) component;
        }
        throw new IllegalArgumentException("No such component: " + cl.getName());
    }

    public TestAppComponent getAppComponent() {
        return component;
    }

    @Module
    public static class SingletonsModule {
        private SingletonsModule() {
            throw new AssertionError("No instances.");
        }

        @Provides
        @Singleton
        public static Gson provideGson() {
            return new Gson();
        }

        @Provides
        @Singleton
        public static Cupboard provideCupboard(Gson gson) {
            return CupboardProvider.withDefaultSettings(gson).buildAndSetupCupboard();
        }

        @Provides
        @Singleton
        public static SQLiteOpenHelper provideOpenHelper(AppContextHolder appContextHolder, Cupboard cupboard) {
            return new CupboardSQLiteOpenHelper2(appContextHolder.getAppContext(), "vertical.db", 1, cupboard);
        }

        @Provides
        public static ApplicationCreationCallback[] provideCreationCallbacks() {
            return new ApplicationCreationCallback[]{};
        }
    }
}
