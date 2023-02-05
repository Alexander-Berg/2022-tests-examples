package ru.yandex.autotests.mobile.disk.android.core.provider;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import ru.yandex.autotests.mobile.disk.android.core.api.DiskApi;
import ru.yandex.autotests.mobile.disk.android.steps.DiskApiSteps;

import static ru.yandex.autotests.mobile.disk.data.AccountConstants.TEST_USER_API;

public class TestUserApiStepsProvider implements Provider<DiskApiSteps> {
    @Inject
    @Named(TEST_USER_API)
    private DiskApi diskApi;


    @Override
    public DiskApiSteps get() {
        return new DiskApiSteps(diskApi);
    }
}
