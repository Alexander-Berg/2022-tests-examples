package ru.yandex.disk;

import ru.yandex.disk.app.ComponentService;
import ru.yandex.disk.app.ComponentServiceExtractor;
import ru.yandex.disk.app.DiskServicesAnalyzer;
import ru.yandex.disk.provider.DiskComponentsController;
import ru.yandex.disk.provider.DiskContentProvider;
import ru.yandex.disk.provider.DiskUriProcessorMatcher;
import ru.yandex.disk.replication.PackagesBroadcastReceiver;
import ru.yandex.disk.service.CommandScheduler;
import ru.yandex.disk.service.CommandStarter;
import ru.yandex.disk.service.DiskService;

import javax.annotation.Nullable;
import javax.inject.Provider;

import static ru.yandex.disk.util.ReflectionUtils.setField;

@SuppressWarnings("unchecked")
public class InjectionUtils {
    public static void setUpInjectionServiceForDiskContentProvider(
            final DiskUriProcessorMatcher matcher) {
        ComponentServiceExtractor.setImpl(c -> new ComponentService() {

            @Nullable
            @Override
            public <T> T getComponent(final Class<T> componentClass) {
                return (T) (DiskContentProvider.Component) provider ->
                        setField(provider, "sqliteUriProcessorMatcher",
                                (Provider<DiskUriProcessorMatcher>) () -> matcher);
            }
        });
    }

    public static void setUpInjectionServiceForDiskService(final CommandStarter commandStarter,
                                                           final CommandScheduler commandScheduler,
                                                           final DiskComponentsController diskComponentsController) {
        ComponentServiceExtractor.setImpl(context -> new ComponentService() {

            @Nullable
            @Override
            public <T> T getComponent(final Class<T> componentClass) {
                return (T) (DiskService.Component) r -> {
                    setField(r, "commandStarter", commandStarter);
                    setField(r, "diskComponentsController", diskComponentsController);
                    setField(r, "commandScheduler", commandScheduler);
                };
            }
        });
    }

    public static void setUpInjectionServiceForPackagesBroadcastReceiver(final DiskServicesAnalyzer analyzer) {
        ComponentServiceExtractor.setImpl(context -> new ComponentService() {
            @Override
            public <T> T getComponent(final Class<T> componentClass) {
                return (T) new PackagesBroadcastReceiver.Component() {
                    @Override
                    public void inject(final PackagesBroadcastReceiver receiver) {
                        setField(receiver, "analyzer", analyzer);
                    }

                    @Override
                    public boolean isAnonymous() {
                        return false;
                    }
                };
            }
        });
    }

}
