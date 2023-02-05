package com.yandex.mail.settings;

import com.yandex.mail.runners.IntegrationTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.function.BiConsumer;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;
import androidx.preference.TwoStatePreference;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(IntegrationTestRunner.class)
public class DependencyChangerTest {

    @SuppressWarnings("NullableProblems") // beforeEachTest
    @Mock
    @NonNull
    BiConsumer<Preference, Boolean> action;

    @SuppressWarnings("NullableProblems") // beforeEachTest
    @NonNull
    TwoStatePreference twoStatePreference;

    @SuppressWarnings("NullableProblems") // beforeEachTest
    @NonNull
    DependencyChanger dependencyChanger;

    @Before
    public void beforeEachTest() {
        MockitoAnnotations.initMocks(this);

        twoStatePreference = new SwitchPreferenceCompat(IntegrationTestRunner.app());
        dependencyChanger = new DependencyChanger(twoStatePreference, action);
    }

    @Test
    public void addDependent_shouldSync() {
        Preference preference = new Preference(IntegrationTestRunner.app());

        dependencyChanger.addDependent(preference);

        verify(action).accept(preference, false);
    }

    @Test
    public void addDependants_shouldSync() {
        Preference preference1 = new Preference(IntegrationTestRunner.app());
        Preference preference2 = new Preference(IntegrationTestRunner.app());

        dependencyChanger.addDependants(preference1, preference2);

        verify(action).accept(preference1, false);
        verify(action).accept(preference2, false);
    }

    @Test
    public void removeDependants_shouldNotSyncAfterRemove() {
        Preference preference1 = new Preference(IntegrationTestRunner.app());

        dependencyChanger.addDependent(preference1);
        dependencyChanger.removeDependant(preference1);
        twoStatePreference.setChecked(true);

        verify(action).accept(preference1, false);
        verify(action, never()).accept(preference1, true);
    }

    @Test
    public void changePreference_shouldSync() {
        Preference preference1 = new Preference(IntegrationTestRunner.app());

        dependencyChanger.addDependent(preference1);

        verify(action).accept(preference1, false);

        twoStatePreference.setChecked(true);

        verify(action).accept(preference1, false);
    }
}
