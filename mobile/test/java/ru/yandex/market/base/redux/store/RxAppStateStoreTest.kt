package ru.yandex.market.base.redux.store

import org.junit.Before
import org.junit.Test
import ru.yandex.market.base.redux.TestAction
import ru.yandex.market.base.redux.TestState
import ru.yandex.market.base.redux.asyncaction.rxAsyncAction
import ru.yandex.market.base.redux.configureTestRxReduxStore

class RxAppStateStoreTest {

    private val testStore = configureTestRxReduxStore()

    @Before
    fun setUp() {
        testStore.dispatchAction(TestAction(""))
    }

    @Test
    fun `Can observe state changes with observable and send  action`() {
        val payload = "payload"

        val testSubscriber = testStore.subscribe().test()
        testStore.dispatchAction(TestAction(payload))

        testSubscriber
            .assertNoErrors()
            .assertNotComplete()
            .assertValues(
                TestState(testValue = ""),
                TestState(testValue = payload)
            )
    }

    @Test
    fun `Can send thunk`() {
        val payload = "payload"

        val testSubscriber = testStore.subscribe().test()
        val thunk = rxAsyncAction<TestState> {
            dispatch(TestAction(payload)).ignoreElement()
        }
        testStore.dispatchAsyncAction(thunk)
            .subscribe()

        testSubscriber
            .assertNoErrors()
            .assertNotComplete()
            .assertValues(
                TestState(testValue = ""),
                TestState(testValue = payload)
            )
    }
}