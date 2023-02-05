package com.yandex.mail.fragment;

import com.yandex.mail.runners.IntegrationTestRunner;
import com.yandex.mail.tools.SupportFragmentController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.annotation.NonNull;

import static android.os.Looper.getMainLooper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

@RunWith(IntegrationTestRunner.class)
public class BaseListFragmentTest {

    @SuppressWarnings("NullableProblems") // @Before
    @NonNull
    private SupportFragmentController<BaseListFragment> controller;

    @Before
    public void setUp() {
        controller = SupportFragmentController.of(new TestBaseListFragment());
    }

    @Test
    public void test_isNotStartedBeforeOnStart() {
        controller.create();
        assertThat(controller.get().isStarted()).isFalse();
    }

    @Test
    public void test_isStartedAfterOnStart() {
        controller.create().start();
        assertThat(controller.get().isStarted()).isTrue();
        controller.resume();
        assertThat(controller.get().isStarted()).isTrue();
    }

    @Test
    public void test_isNotStartedAfterOnStop() {
        controller.create().start();
        controller.stop();
        assertThat(controller.get().isStarted()).isFalse();
        controller.destroy();
        assertThat(controller.get().isStarted()).isFalse();
    }

    @Test
    public void runOnUiThreadIfActivityIsAlive_runsIfActivityIsAlive() {
        final Runnable mockRunnable = mock(Runnable.class);
        controller.create().start();
        controller.get().runOnUiThreadIfActivityIsAlive(mockRunnable);
        verify(mockRunnable).run();
    }

    @Test
    public void runOnUiThreadIfActivityIsAlive_doesNotRunIfDetached() {
        controller.create().start();
        shadowOf(getMainLooper()).idle();
        // trigger detach
        controller.get().getActivity().getSupportFragmentManager().beginTransaction().remove(controller.get()).commit();
        shadowOf(getMainLooper()).idle();

        final Runnable mockRunnable = mock(Runnable.class);
        controller.get().runOnUiThreadIfActivityIsAlive(mockRunnable);

        shadowOf(getMainLooper()).idle();

        verify(mockRunnable, never()).run();
    }

    public static class TestBaseListFragment extends BaseListFragment {

    }
}
