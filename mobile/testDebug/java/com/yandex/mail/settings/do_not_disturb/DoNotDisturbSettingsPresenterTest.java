package com.yandex.mail.settings.do_not_disturb;

import com.yandex.mail.BaseMailApplication;
import com.yandex.mail.model.GeneralSettingsModel;
import com.yandex.mail.runners.IntegrationTestRunner;
import com.yandex.mail.settings.GeneralSettings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

import androidx.annotation.NonNull;
import kotlin.Pair;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(IntegrationTestRunner.class)
public class DoNotDisturbSettingsPresenterTest {

    @SuppressWarnings("NullableProblems") // @Before.
    @NonNull
    @Mock
    private BaseMailApplication baseMailApplication;

    @SuppressWarnings("NullableProblems") // @Before.
    @NonNull
    @Mock
    private GeneralSettingsModel settingsModel;

    @SuppressWarnings("NullableProblems") // @Before.
    @NonNull
    @Mock
    private DoNotDisturbSettingsView view;

    @SuppressWarnings("NullableProblems") // @Before.
    @NonNull
    private DoNotDisturbSettingsPresenter presenter;

    @Before
    public void beforeEachTest() {
        MockitoAnnotations.initMocks(this);

        presenter = createDoNotDisturbSettingsPresenter();
    }

    @NonNull
    private DoNotDisturbSettingsPresenter createDoNotDisturbSettingsPresenter() {
        return new DoNotDisturbSettingsPresenter(
                baseMailApplication,
                settingsModel
        );
    }

    @Test
    public void loadDoNotDisturbTime_shouldInvokeOnDisturbTimeLoaded() {
        final GeneralSettings settings = new GeneralSettings(RuntimeEnvironment.application);
        settings.edit()
                .setDoNotDisturbTimeFrom(0, 0)
                .setDoNotDisturbTimeTo(7, 30)
                .apply();

        when(settingsModel.getGeneralSettings()).thenReturn(settings);
        presenter.onBindView(view);
        presenter.loadDoNotDisturbTime();

        verify(view).onDoNotDisturbTimeLoaded(new Pair<>(0, 0), new Pair<>(7, 30));
    }
}
