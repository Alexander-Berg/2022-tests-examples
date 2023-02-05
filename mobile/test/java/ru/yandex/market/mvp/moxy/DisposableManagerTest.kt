package ru.yandex.market.mvp.moxy

import io.reactivex.Observable
import io.reactivex.disposables.Disposables
import io.reactivex.schedulers.TestScheduler
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.yandex.market.base.presentation.core.schedule.DisposableManager

class DisposableManagerTest {

    private val manager = DisposableManager<String>()

    @Test
    fun `Disposes previous disposable on replace`() {
        val first = Disposables.empty()
        val second = Disposables.empty()
        assertFalse(first.isDisposed)
        assertFalse(second.isDisposed)
        manager.replaceDisposableInChannel(CHANNEL_FIRST, first)
        manager.replaceDisposableInChannel(CHANNEL_FIRST, second)
        assertTrue(first.isDisposed)
        assertFalse(second.isDisposed)
    }

    @Test
    fun `Replacing disposable for null unsubscribe current disposable`() {
        val disposable = Disposables.empty()
        assertFalse(disposable.isDisposed)
        manager.replaceDisposableInChannel(CHANNEL_FIRST, disposable)
        manager.replaceDisposableInChannel(CHANNEL_FIRST, null)
        assertTrue(disposable.isDisposed)
    }

    @Test
    fun `Calling unsubscribe properly unsubscribe current disposable`() {
        val disposable = Disposables.empty()
        assertFalse(disposable.isDisposed)
        manager.replaceDisposableInChannel(CHANNEL_FIRST, disposable)
        manager.disposeChannel(CHANNEL_FIRST)
        assertTrue(disposable.isDisposed)
    }

    @Test
    fun `isUnsubscribed returns current disposable state`() {
        val disposable = Disposables.empty()
        assertFalse(disposable.isDisposed)
        manager.replaceDisposableInChannel(CHANNEL_FIRST, disposable)
        assertFalse(disposable.isDisposed)
        assertFalse(manager.isChannelDisposed(CHANNEL_FIRST))
        disposable.dispose()
        assertTrue(disposable.isDisposed)
        assertTrue(manager.isChannelDisposed(CHANNEL_FIRST))
    }

    @Test
    fun `isUnsubscribed returns false when no disposable being subscribed`() {
        assertTrue(manager.isChannelDisposed(CHANNEL_FIRST))
    }

    @Test
    fun `Unsubscribe one channel does not unsubscribe another`() {
        val first = Disposables.empty()
        val second = Disposables.empty()
        assertFalse(first.isDisposed)
        assertFalse(second.isDisposed)
        manager.replaceDisposableInChannel(CHANNEL_FIRST, first)
        manager.replaceDisposableInChannel(CHANNEL_SECOND, second)
        manager.disposeChannel(CHANNEL_FIRST)
        assertFalse(manager.isChannelDisposed(CHANNEL_SECOND))
        assertTrue(manager.isChannelDisposed(CHANNEL_FIRST))
    }

    @Test
    fun `Do not unsubscribe channels with condition`() {
        val scheduler = TestScheduler()
        val observable = Observable.just("123").observeOn(scheduler)
        val observer = observable.test()

        manager.replaceDisposableInChannel(CHANNEL_FIRST, observer)

        observer.assertValueCount(0)
        manager.disposeChannelIf { it != CHANNEL_FIRST }
        scheduler.triggerActions()

        observer.assertValueCount(1)
    }

    @Test
    fun `isActive is opposite to isDisposed`() {
        val scheduler = TestScheduler()
        val observable = Observable.just("123")
            .observeOn(scheduler)

        manager.replaceDisposableInChannel(CHANNEL_FIRST, observable.test())

        assertFalse(manager.isChannelDisposed(CHANNEL_FIRST))
        assertTrue(manager.isChannelActive(CHANNEL_FIRST))

        manager.disposeChannel(CHANNEL_FIRST)

        assertTrue(manager.isChannelDisposed(CHANNEL_FIRST))
        assertFalse(manager.isChannelActive(CHANNEL_FIRST))
    }

    companion object {
        private const val CHANNEL_FIRST = "First"
        private const val CHANNEL_SECOND = "Second"
    }
}