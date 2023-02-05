package com.yandex.mail.ui.presenters;

import com.yandex.mail.BaseMailApplication;
import com.yandex.mail.runners.UnitTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import io.reactivex.disposables.Disposable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.failBecauseExceptionWasNotThrown;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(UnitTestRunner.class)
public class PresenterTest {

    @SuppressWarnings("NullableProblems") // Initialized in @Before.
    @NonNull
    private Object view;

    @SuppressWarnings("NullableProblems") // Initialized in @Before.
    @NonNull
    private Presenter<Object> presenter;

    @Before
    public void beforeEachTest() {
        view = new Object();
        presenter = new Presenter<>(mock(BaseMailApplication.class));
    }

    @Test
    public void shouldUnsubscribeSubscriptionsOnUnBindView() {
        Disposable subscription1 = mock(Disposable.class);
        Disposable subscription2 = mock(Disposable.class);

        presenter.unsubscribeOnUnbindView(subscription1);
        presenter.unsubscribeOnUnbindView(subscription2);

        presenter.onBindView(view);
        verify(subscription1, never()).dispose();
        verify(subscription2, never()).dispose();

        presenter.onUnbindView(view);
        verify(subscription1).dispose();
        verify(subscription2).dispose();
    }

    @Test
    public void shouldUnsubscribeSubscriptionsOnPause() {
        Disposable subscription1 = mock(Disposable.class);
        Disposable subscription2 = mock(Disposable.class);

        presenter.unsubscribeOnPause(subscription1);
        presenter.unsubscribeOnPause(subscription2);

        presenter.onResume();
        verify(subscription1, never()).dispose();
        verify(subscription2, never()).dispose();

        presenter.onPause();
        verify(subscription1).dispose();
        verify(subscription2).dispose();
    }

    @Test
    public void shouldUnsubscribePauseSubscriptionsOnUnbind() {
        Disposable subscription1 = mock(Disposable.class);
        Disposable subscription2 = mock(Disposable.class);

        presenter.onBindView(view);

        presenter.unsubscribeOnPause(subscription1);
        presenter.unsubscribeOnPause(subscription2);

        presenter.onResume();
        verify(subscription1, never()).dispose();
        verify(subscription2, never()).dispose();

        presenter.onUnbindView(view);
        verify(subscription1).dispose();
        verify(subscription2).dispose();
    }

    @Test
    public void shouldNotUnsubscribeOnUnbindViewSubscriptionsOnPause() {
        Disposable subscription1 = mock(Disposable.class);
        Disposable subscription2 = mock(Disposable.class);

        presenter.unsubscribeOnUnbindView(subscription1);
        presenter.unsubscribeOnUnbindView(subscription2);

        presenter.onBindView(view);
        verify(subscription1, never()).dispose();
        verify(subscription2, never()).dispose();

        presenter.onPause();
        verify(subscription1, never()).dispose();
        verify(subscription2, never()).dispose();
    }

    @Test
    public void shouldDisposeOnUnBindView() {
        Disposable disposable1 = mock(Disposable.class);
        Disposable disposable2 = mock(Disposable.class);

        presenter.unsubscribeOnUnbindView(disposable1);
        presenter.unsubscribeOnUnbindView(disposable2);

        presenter.onBindView(view);
        verify(disposable1, never()).dispose();
        verify(disposable2, never()).dispose();

        presenter.onUnbindView(view);
        verify(disposable1).dispose();
        verify(disposable2).dispose();
    }

    @Test
    public void shouldDisposeOnPause() {
        Disposable disposable1 = mock(Disposable.class);
        Disposable disposable2 = mock(Disposable.class);

        presenter.unsubscribeOnPause(disposable1);
        presenter.unsubscribeOnPause(disposable2);

        presenter.onResume();
        verify(disposable1, never()).dispose();
        verify(disposable2, never()).dispose();

        presenter.onPause();
        verify(disposable1).dispose();
        verify(disposable2).dispose();
    }

    @Test
    public void shouldDisposePauseDisposablesOnUnbind() {
        Disposable disposable1 = mock(Disposable.class);
        Disposable disposable2 = mock(Disposable.class);

        presenter.onBindView(view);

        presenter.unsubscribeOnPause(disposable1);
        presenter.unsubscribeOnPause(disposable2);

        presenter.onResume();
        verify(disposable1, never()).dispose();
        verify(disposable2, never()).dispose();

        presenter.onUnbindView(view);
        verify(disposable1).dispose();
        verify(disposable2).dispose();
    }

    @Test
    public void shouldNotDisposeOnUnbindDisposablesOnPause() {
        Disposable disposable1 = mock(Disposable.class);
        Disposable disposable2 = mock(Disposable.class);

        presenter.unsubscribeOnUnbindView(disposable1);
        presenter.unsubscribeOnUnbindView(disposable2);

        presenter.onBindView(view);
        presenter.onPause();

        verify(disposable1, never()).dispose();
        verify(disposable2, never()).dispose();
    }

    @Test
    public void shouldRunActionWhenNotPaused() {
        presenter.onResume();
        Runnable action = mock(Runnable.class);
        presenter.runActionWhenNotPaused(action);

        verify(action).run();
    }

    @Test
    public void shouldRunActionWhenResumed() {
        presenter.onPause();
        Runnable action = mock(Runnable.class);
        presenter.runActionWhenNotPaused(action);

        verify(action, never()).run();

        presenter.onResume();
        verify(action).run();
    }

    @Test
    public void view_shouldBeNullByDefault() {
        assertThat(presenter.view()).isNull();
    }

    @Test
    public void onBindView_shouldAttachViewToPresenter() {
        presenter.onBindView(view);
        assertThat(presenter.view()).isSameAs(view);
    }

    @Test
    public void onBindView_shouldThrowIfPreviousViewIsNotUnbounded() {
        presenter.onBindView(view);

        try {
            presenter.onBindView(new Object());
            failBecauseExceptionWasNotThrown(IllegalStateException.class);
        } catch (IllegalStateException expected) {
            assertThat(expected).hasMessage("Previous view is not unbounded! previousView = " + view);
        }
    }

    @Test
    public void executeIfViewBound_shouldExecuteAction() {
        presenter.onBindView(view);
        //noinspection unchecked
        Consumer<Object> action = mock(Consumer.class);

        presenter.executeIfViewBound(action);
        verify(action).accept(view);
        verifyNoMoreInteractions(action);
    }

    @Test
    public void executeIfViewBound_shouldNotExecuteAction() {
        //noinspection unchecked
        Consumer<Object> action = mock(Consumer.class);

        presenter.executeIfViewBound(action);
        verifyNoInteractions(action);
    }

    @Test
    public void onUnbindView_shouldNullTheViewReference() {
        presenter.onBindView(view);
        assertThat(presenter.view()).isSameAs(view);

        presenter.onUnbindView(view);
        assertThat(presenter.view()).isNull();
    }

    @Test
    public void onUnbindView_shouldThrowIfPreviousViewIsNotSameAsExpected() {
        presenter.onBindView(view);
        Object unexpectedView = new Object();

        try {
            presenter.onUnbindView(unexpectedView);
            failBecauseExceptionWasNotThrown(IllegalStateException.class);
        } catch (IllegalStateException expected) {
            assertThat(expected).hasMessage("Unexpected view! previousView = " + view + ", view to unbind = " + unexpectedView);
        }
    }
}
