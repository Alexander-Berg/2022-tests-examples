package com.yandex.mail.theme;


import com.yandex.mail.model.DeveloperSettingsModel;
import com.yandex.mail.model.GeneralSettingsModel;
import com.yandex.mail.runners.IntegrationTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Calendar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.reactivex.observers.TestObserver;
import io.reactivex.subjects.PublishSubject;
import kotlin.Pair;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(IntegrationTestRunner.class)
public class ThemeModelTest {

    @Mock
    @SuppressWarnings("NullableProblems") // Before
    @NonNull
    GeneralSettingsModel mockSettingsModel;

    @Mock
    @SuppressWarnings("NullableProblems") // Before
    @NonNull
    DeveloperSettingsModel mockDeveloperSettingsModel;

    @SuppressWarnings("NullableProblems") // Before
    @NonNull
    private ThemeModel themeModel;

    @SuppressWarnings("NullableProblems") // Before
    @NonNull
    private PublishSubject<Pair<Boolean, String>> accountThemeSettings;

    @SuppressWarnings("NullableProblems") // Before
    @NonNull
    private TestObserver<Theme> testObserver;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        themeModel = new ThemeModel(IntegrationTestRunner.app(), mockSettingsModel, mockDeveloperSettingsModel, null);

        accountThemeSettings = PublishSubject.create();
        when(mockSettingsModel.observeThemeInfo(1)).thenReturn(accountThemeSettings);

        testObserver = new TestObserver<>();
        themeModel.observeThemesForAccount(1).subscribe(testObserver);
    }

    @Test
    public void themeObservable_shouldEmitNoThemeOnDisabledThemesInSettings() {
        accountThemeSettings.onNext(themeSettings(false, ""));
        testObserver.assertValue(NoTheme.INSTANCE);
    }

    @Test
    public void themeObservalbe_shouldEmitThemeWhenThemesIsEnabled() {
        accountThemeSettings.onNext(themeSettings(true, "bears"));
        testObserver.assertValue(new DownloadableTheme("bears", themeModel));
    }

//    @Test
//    public void themeObservable_shouldEmitNewYearWhenItsNewYear() {
//        when(mockDeveloperSettingsModel.isNewYearForced()).thenReturn(true);
//
//        accountThemeSettings.onNext(themeSettings(false, null));
//        testObserver.assertValue(NewYearTheme.INSTANCE);
//    }
//
//    @Test
//    public void themeObservable_shouldEmitNewYearThemeInsteadOfDownloadableTheme() {
//        when(mockDeveloperSettingsModel.isNewYearForced()).thenReturn(true);
//
//        accountThemeSettings.onNext(themeSettings(true, "bears"));
//        testObserver.assertValue(NewYearTheme.INSTANCE);
//    }
//
//    @Test
//    public void setNewYearThemeActivated_asFalseShouldSwitchToDefaultBehaviour() {
//        when(mockDeveloperSettingsModel.isNewYearForced()).thenReturn(true);
//
//        accountThemeSettings.onNext(themeSettings(true, "bears"));
//        themeModel.setNewYearThemeActivated(1, false);
//
//        testObserver.assertValues(NewYearTheme.INSTANCE, new DownloadableTheme("bears", themeModel));
//    }

    @Test
    public void themeObservable_shouldEmitAnotherValueOnSettingsChanges() {
        accountThemeSettings.onNext(themeSettings(false, ""));
        accountThemeSettings.onNext(themeSettings(true, "bears"));

        testObserver.assertValues(NoTheme.INSTANCE, new DownloadableTheme("bears", themeModel));
    }

    @Test
    public void isNewYearNow_shouldWorkCorrectly() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2015, Calendar.DECEMBER, 1);
        assertThat(themeModel.isNewYearNow(calendar)).isFalse();

        calendar.set(2015, Calendar.DECEMBER, 31);
        assertThat(themeModel.isNewYearNow(calendar)).isTrue();
    }

    @NonNull
    private Pair<Boolean, String> themeSettings(boolean themesEnabled, @Nullable String themeName) {
        return new Pair<>(themesEnabled, themeName);
    }
}
