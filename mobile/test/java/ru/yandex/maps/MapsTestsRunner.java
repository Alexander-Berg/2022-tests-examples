package ru.yandex.maps;

import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Method;

import ru.yandex.yandexmaps.app.TestMapsApplication;

public class MapsTestsRunner extends RobolectricTestRunner {

    private static final int SDK_EMULATE_LEVEL = 23;

    public MapsTestsRunner(Class<?> klass) throws InitializationError {
        super(klass);
        System.setProperty("android.package", "ru.yandex.yandexmaps");
    }

    @Override
    public Config getConfig(Method method) {
        final Config defaultConfig = super.getConfig(method);

        return new Config.Implementation(
                new int[]{SDK_EMULATE_LEVEL},
                defaultConfig.minSdk(),
                defaultConfig.maxSdk(),
                defaultConfig.manifest(),
                defaultConfig.qualifiers(),
                //https://github.com/robolectric/robolectric/issues/1954
                //https://github.com/robolectric/robolectric-gradle-plugin/issues/142
                System.getProperty("android.package"),
                defaultConfig.resourceDir(),
                defaultConfig.assetDir(),
                defaultConfig.shadows(),
                defaultConfig.instrumentedPackages(),
                TestMapsApplication.class,
                defaultConfig.libraries()
        );
    }
}
