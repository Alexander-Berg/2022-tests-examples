package ru.auto.core_ui.tea

import io.qameta.allure.kotlin.junit4.AllureRunner
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.core_ui.tea.TeaFeatureSimpleSetTest.TestFeature.Eff
import ru.auto.core_ui.tea.TeaFeatureSimpleSetTest.TestFeature.Msg
import ru.auto.core_ui.tea.TeaFeatureSimpleSetTest.TestFeature.State
import ru.auto.core_ui.util.Disposable
import kotlin.test.assertEquals

@RunWith(AllureRunner::class)
class TeaFeatureSimpleSetTest {

    private object TestFeature {
        object State

        sealed class Msg {
            object OnSomeAction : Msg()
            object OnCloseClicked : Msg()
        }

        sealed class Eff {
            object SomeAction : Eff()
            object Close : Eff()
        }
    }

    private val feature: Feature<Msg, State, Eff> = TeaFeatureSimpleSet(State, ::reduce)

    private fun reduce(msg: Msg, state: State): Pair<State, Set<Eff>> = when (msg) {
        is Msg.OnSomeAction -> {
            state to setOf(Eff.SomeAction)
        }
        is Msg.OnCloseClicked -> {
            state to setOf(Eff.Close)
        }
    }

    private class CallCountConsumer<T> : Consumer<T> {
        var count = 0

        override fun invoke(value: T) {
            count++
        }
    }


    @Test
    fun testFeatureDisposeConcurrentModificationException() {
        feature.wrapWithSyncEffectHandler { eff, _ -> if (eff is Eff.Close) feature.dispose() }
            .subscribe({ }, { })
        feature.accept(Msg.OnCloseClicked)
    }

    @Test
    fun testFeatureConsumerDisposeConcurrentModificationException() {
        var disposable: Disposable? = null
        disposable = feature.subscribe({ }, { if (it is Eff.Close) disposable?.dispose() })
        feature.accept(Msg.OnCloseClicked)
    }

    @Test
    fun testFeatureConsumerAfterDispose() {
        val stateCallCountConsumer = CallCountConsumer<State>()
        val effCallCountConsumer = CallCountConsumer<Eff>()
        // stateCallCount = 1, effCallCount = 0
        val disposable = feature.subscribe(stateCallCountConsumer, effCallCountConsumer)
        disposable.dispose()
        // stateCallCount = 1, effCallCount = 0 // after dispose not should changed!!!
        feature.accept(Msg.OnSomeAction)

        assertEquals(stateCallCountConsumer.count, 1, "state consume call after dispose")
        assertEquals(effCallCountConsumer.count, 0, "eff consume call after dispose")
    }

    @Test
    fun testShouldConsumeAfterResubscribe() {
        val stateCallCountConsumer = CallCountConsumer<State>()
        val effCallCountConsumer = CallCountConsumer<Eff>()
        // stateCallCount = 1, effCallCount = 0
        val disposable = feature.subscribe(stateCallCountConsumer, effCallCountConsumer)
        // stateCallCount = 2, effCallCount = 1
        feature.accept(Msg.OnSomeAction)
        disposable.dispose()
        // stateCallCount = 3, effCallCount = 1
        val newDisposable = feature.subscribe(stateCallCountConsumer, effCallCountConsumer)
        // stateCallCount = 4, effCallCount = 2
        feature.accept(Msg.OnSomeAction)
        newDisposable.dispose()

        assertEquals(stateCallCountConsumer.count, 4, "state not consumed after resubscribe")
        assertEquals(effCallCountConsumer.count, 2, "eff not consumed after resubscribe")
    }
}
