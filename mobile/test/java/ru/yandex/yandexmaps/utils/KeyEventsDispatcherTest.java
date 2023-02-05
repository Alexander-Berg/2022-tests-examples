package ru.yandex.yandexmaps.utils;

import android.view.KeyEvent;

import org.junit.Test;

import javax.annotation.NonNullByDefault;

import io.reactivex.disposables.Disposable;
import io.reactivex.observers.TestObserver;
import ru.yandex.maps.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

@NonNullByDefault
public class KeyEventsDispatcherTest extends BaseTest {


    @Test
    public void emitEvent_whenSubscribed() {
        final KeyEventsDispatcher dispatcher = new KeyEventsDispatcher();
        final KeyEvent event = volumeUp();

        final TestObserver<Object> subscriber = dispatcher.volumeUps().cast(Object.class).test();
        dispatcher.dispatch(event);
        subscriber.assertValues(event);
    }

    @Test
    public void handleEvent_onlyWhenClientIsSubscribedToEvents(){
        final KeyEventsDispatcher dispatcher = new KeyEventsDispatcher();

        assertThat(dispatcher.dispatch(volumeUp())).isFalse();
        assertThat(dispatcher.dispatch(volumeDown())).isFalse();

        final Disposable subscription = dispatcher.volumeUps().test();

        assertThat(dispatcher.dispatch(volumeUp())).isTrue();
        assertThat(dispatcher.dispatch(volumeDown())).isFalse();

        subscription.dispose();

        assertThat(dispatcher.dispatch(volumeUp())).isFalse();
        assertThat(dispatcher.dispatch(volumeDown())).isFalse();
    }

    @Test
    public void handleEvent_onMultipleSubscriptionToTheSameEvent() {
        final KeyEventsDispatcher dispatcher = new KeyEventsDispatcher();

        assertThat(dispatcher.dispatch(volumeDown())).isFalse();

        final Disposable firstSubscription = dispatcher.volumeDowns().subscribe();

        assertThat(dispatcher.dispatch(volumeDown())).isTrue();

        final Disposable secondSubscription = dispatcher.volumeDowns().subscribe();

        assertThat(dispatcher.dispatch(volumeDown())).isTrue();

        firstSubscription.dispose();

        assertThat(dispatcher.dispatch(volumeDown())).isTrue();

        secondSubscription.dispose();

        assertThat(dispatcher.dispatch(volumeDown())).isFalse();
    }

    private KeyEvent volumeUp() {
        return new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP);
    }

    private KeyEvent volumeDown() {
        return new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN);
    }
}