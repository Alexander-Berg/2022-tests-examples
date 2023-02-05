package ru.yandex.yandexbus.inhouse.mvp;

import org.junit.Before;
import org.junit.Test;
import rx.Subscription;

import java.util.NoSuchElementException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class BasePresenterTest {
    private interface TestView {}

    private BasePresenter<TestView> presenter;
    private TestView view;

    @Before
    public void beforeEachTest() {
        view = new TestView() {};
        presenter = new BasePresenterImplDelegate<>();
    }

    @Test
    public void bindView_shouldAttachViewToThePresenter() {
        presenter.bind(view);
        assertEquals(presenter.view(), view);
    }

    @Test(expected = IllegalStateException.class)
    public void bindView_shouldThrowIfPreviousViewIsNotUnbounded() {
        presenter.bind(view);
        presenter.bind(createView());
    }

    @Test(expected = NoSuchElementException.class)
    public void view_shouldThrowByDefault() {
        presenter.view();
    }

    @Test
    public void view_shouldReturnViewAfterBind() {
        presenter.bind(view);
        assertEquals(presenter.view(), view);
    }

    @Test
    public void unsubscribeOnUnbindView_shouldWorkAccordingItsContract() {
        presenter.bind(view);

        Subscription subscription1 = mock(Subscription.class);
        Subscription subscription2 = mock(Subscription.class);
        Subscription subscription3 = mock(Subscription.class);

        presenter.unsubscribeOnUnbind(subscription1, subscription2, subscription3);
        verify(subscription1, never()).unsubscribe();
        verify(subscription2, never()).unsubscribe();
        verify(subscription3, never()).unsubscribe();

        presenter.unbind(view);
        verify(subscription1).unsubscribe();
        verify(subscription2).unsubscribe();
        verify(subscription3).unsubscribe();
    }

    @Test(expected = NoSuchElementException.class)
    public void unbindView_shouldNullTheViewReference() {
        presenter.bind(view);
        presenter.unbind(view);
        presenter.view();
    }

    @Test(expected = IllegalStateException.class)
    public void unbindView_shouldThrowIfPreviousViewIsNotSameAsExpected() {
        presenter.bind(view);
        presenter.unbind(createView());
    }

    private TestView createView() {
        return new TestView() {};
    }
}
