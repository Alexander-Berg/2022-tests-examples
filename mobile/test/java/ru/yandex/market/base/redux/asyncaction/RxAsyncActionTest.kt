package ru.yandex.market.base.redux.asyncaction

import io.reactivex.Completable
import io.reactivex.Single
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import ru.yandex.market.base.redux.TestAction
import ru.yandex.market.base.redux.TestState
import ru.yandex.market.base.redux.configureTestReduxStore
import ru.yandex.market.base.redux.testRxThunkSingle

class RxAsyncActionTest {

    private val testStore = configureTestReduxStore()

    @Before
    fun setUp() {
        testStore.dispatch(TestAction(""))
    }

    @Test
    fun `Can dispatch completable action from thunk`() {
        val payload = "new value"
        val testAction = TestAction(payload)

        val testThunk = rxAsyncAction<TestState> {
            dispatch(testAction).ignoreElement()
        }

        (testStore.dispatch(testThunk) as Completable)
            .subscribe()

        Assert.assertTrue(testStore.state.testValue == payload)
    }

    @Test
    fun `Can dispatch single action from thunk`() {
        val payload = "new value"
        val testAction = TestAction(payload)

        val testThunk = testRxThunkSingle<TestState> {
            dispatch(testAction)
        }

        Assert.assertTrue(testStore.state.testValue == "")

        testDispatch(testThunk)
            .test()
            .assertValue { it.testValue == payload }

        Assert.assertTrue(testStore.state.testValue == payload)
    }

    @Test
    fun `Can dispatch and get new state from thunk`() {
        val payload = "some domain logic result"

        val testThunk = testRxThunkSingle<TestState> {
            Single.just(payload)
                .flatMap { value ->
                    dispatch(TestAction(value))
                }
        }

        Assert.assertTrue(testStore.state.testValue == "")

        testDispatch(testThunk)
            .test()
            .assertValue { it.testValue == payload }

        Assert.assertTrue(testStore.state.testValue == payload)
    }

    @Test
    fun `Can dispatch several actions from async action and get most recent state`() {
        val firstPayload = "first action"
        val secondPayload = "second action"
        val thirdPayload = "third action"

        val testThunk = testRxThunkSingle<TestState> {
            dispatch(
                TestAction(firstPayload),
                TestAction(secondPayload),
                TestAction(thirdPayload),
            )
        }

        Assert.assertTrue(testStore.state.testValue == "")

        testDispatch(testThunk)
            .test()
            .assertValue { it.testValue == thirdPayload }

        Assert.assertTrue(testStore.state.testValue == thirdPayload)
    }

    private fun testDispatch(testAsyncAction: AsyncAction<TestState>): Single<TestState> {
        @Suppress("UNCHECKED_CAST")
        return testStore.dispatch(testAsyncAction) as Single<TestState>
    }
}