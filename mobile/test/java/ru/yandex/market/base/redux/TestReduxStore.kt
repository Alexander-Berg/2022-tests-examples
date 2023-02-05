package ru.yandex.market.base.redux

import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.mockito.kotlin.mock
import org.reduxkotlin.Dispatcher
import org.reduxkotlin.GetState
import ru.yandex.market.base.redux.action.Action
import ru.yandex.market.base.redux.action.BatchAction
import ru.yandex.market.base.redux.asyncaction.AsyncAction
import ru.yandex.market.base.redux.asyncaction.AsyncActionTools
import ru.yandex.market.base.redux.asyncaction.RxAsyncActionContext
import ru.yandex.market.base.redux.asyncaction.createAsyncActionMiddleware
import ru.yandex.market.base.redux.reducer.RootActionReducer
import ru.yandex.market.base.redux.schedulers.ReduxSchedulers
import ru.yandex.market.base.redux.store.RxAppStateStore
import ru.yandex.market.base.redux.store.configureStore

fun configureTestRxReduxStore() = RxAppStateStore(
    appStateStore = configureTestReduxStore(),
    reduxSchedulers = ReduxSchedulers(
        reducersThread = Schedulers.trampoline(),
        worker = Schedulers.trampoline()
    ),
    reduxCommonHealthAnalytics = mock()
)

fun configureTestReduxStore() = configureStore(TestState()) {

    reducer = TestReducer()

    applyMiddleware {
        createAsyncActionMiddleware(
            tools = AsyncActionTools(
                schedulers = ReduxSchedulers(
                    reducersThread = Schedulers.trampoline(),
                    worker = Schedulers.trampoline()
                ),
                reduxCommonHealthAnalytics = mock()
            )
        )
    }
}

fun <State> testRxThunkSingle(
    asyncActionOperation: RxAsyncActionContext<State>.() -> Single<State>
): AsyncAction<State> {

    val thunkLambda = { dispatch: Dispatcher, getState: GetState<State>, tools: AsyncActionTools ->

        val thunkContext = RxAsyncActionContext(
            dispatcher = dispatch,
            getState = getState,
            reduxSchedulers = tools.schedulers,
        )

        thunkContext.asyncActionOperation()
    }

    return AsyncAction(thunkLambda)

}

data class TestState(
    val testValue: String = ""
)

data class TestAction(
    val testPayload: String = ""
) : Action

class TestReducer : RootActionReducer<TestState>() {

    override fun reduce(state: TestState, action: Action): TestState {
        if (action is BatchAction) {
            if (action.actions.isEmpty()) {
                return state
            }
            return reduce(reduce(state, action.actions.first()), BatchAction(action.actions.drop(1)))
        }
        return TestState(
            testValue = reduceTestAction(state.testValue, action)
        )
    }

    private fun reduceTestAction(oldValue: String, action: Action): String {
        return when (action) {
            is TestAction -> action.testPayload
            else -> oldValue
        }
    }
}