package com.yandex.mail.model;

import com.yandex.mail.runners.IntegrationTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import androidx.annotation.NonNull;
import io.reactivex.observers.TestObserver;

import static com.yandex.mail.BaseMailApplication.getApplicationComponent;

@RunWith(IntegrationTestRunner.class)
public class DeveloperSettingsModelTest {

    @SuppressWarnings("NullableProblems")
    @NonNull
    private DeveloperSettingsModel developerSettingsModel;

    @Before
    public void beforeEachTest() {
        developerSettingsModel = getApplicationComponent(RuntimeEnvironment.application).developerSettingsModel();
    }

    @Test
    public void causeCrash_shouldThrowException() {
        TestObserver<Void> observer = new TestObserver<>();
        developerSettingsModel.causeCrash().subscribe(observer);
        observer.assertError(RuntimeException.class);
    }
}
