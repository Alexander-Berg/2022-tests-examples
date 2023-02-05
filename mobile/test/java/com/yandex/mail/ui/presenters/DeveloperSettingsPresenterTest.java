package com.yandex.mail.ui.presenters;

import com.yandex.mail.BaseMailApplication;
import com.yandex.mail.model.DeveloperSettingsModel;
import com.yandex.mail.ui.presenters.configs.DeveloperSettingsPresenterConfig;
import com.yandex.mail.ui.views.DeveloperSettingsView;

import org.junit.Before;
import org.junit.Test;

import androidx.annotation.NonNull;
import io.reactivex.schedulers.Schedulers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DeveloperSettingsPresenterTest {

    @SuppressWarnings("NullableProblems") // Initialized in @Before.
    @NonNull
    private BaseMailApplication application;

    @SuppressWarnings("NullableProblems") // Initialized in @Before.
    @NonNull
    private DeveloperSettingsPresenterConfig presenterConfig;

    @SuppressWarnings("NullableProblems") // Initialized in @Before.
    @NonNull
    private DeveloperSettingsModel developerSettingsModel;

    @SuppressWarnings("NullableProblems") // Initialized in @Before.
    @NonNull
    private DeveloperSettingsPresenter developerSettingsPresenter;

    @SuppressWarnings("NullableProblems") // Initialized in @Before.
    @NonNull
    private DeveloperSettingsView developerSettingsView;

    @Before
    public void beforeEachTest() {
        application = mock(BaseMailApplication.class);
        presenterConfig = new DeveloperSettingsPresenterConfig(
                Schedulers.trampoline(),
                Schedulers.trampoline()
        );

        developerSettingsModel = mock(DeveloperSettingsModel.class);

        developerSettingsPresenter = new DeveloperSettingsPresenter(application, presenterConfig, developerSettingsModel);
        developerSettingsView = mock(DeveloperSettingsView.class);
    }

    @Test
    public void onBindView_shouldSetTinyDancerState() {
        when(developerSettingsModel.isTinyDancerEnabled()).thenReturn(true);

        developerSettingsPresenter.onBindView(developerSettingsView);
        verify(developerSettingsModel).isTinyDancerEnabled();
        verify(developerSettingsView).onTinyDancerStateChange(true);
    }

    @Test
    public void changeTinyDancerState_shouldCallModelIfPassTrue() {
        developerSettingsPresenter.changeTinyDancerState(true);
        verify(developerSettingsModel).setTinyDancerEnabled(true);
        verify(developerSettingsModel).applyDeveloperSettings();
    }

    @Test
    public void changeTinyDancerState_shouldCallModelIfPassFalse() {
        developerSettingsPresenter.changeTinyDancerState(false);
        verify(developerSettingsModel).setTinyDancerEnabled(false);
        verify(developerSettingsModel).applyDeveloperSettings();
    }

    @Test
    public void applyNewSettings_shouldCallModel() {
        verify(developerSettingsModel, never()).applyDeveloperSettings();

        developerSettingsPresenter.applyNewSettings();
        verify(developerSettingsModel).applyDeveloperSettings();
    }

    @Test
    public void applyForceNewYear_isPassedToModel() {
        developerSettingsPresenter.applyForceNewYear(true);
        verify(developerSettingsModel).forceNewYear(true);

        developerSettingsPresenter.applyForceNewYear(false);
        verify(developerSettingsModel).forceNewYear(false);
    }

    @Test
    public void isNewYearForced_isPassedToModel() {
        developerSettingsPresenter.isNewYearForced();
        verify(developerSettingsModel).isNewYearForced();
    }

    @Test
    public void callSyncOfAllFolders_isPassedToModel() {
        developerSettingsPresenter.callSyncOfAllFolders();
        verify(developerSettingsModel).callSyncOfAllFolders();
    }
}
