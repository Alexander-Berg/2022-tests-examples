package ru.auto.test.common.presentation

import ru.auto.core_ui.tea.Feature
import ru.auto.core_ui.tea.TeaObservableEffectHandler
import ru.auto.core_ui.tea.wrapWithObservableEffectHandler
import ru.auto.data.util.unsubscribeSafe
import rx.Completable
import rx.Observable
import rx.Subscription
import rx.subjects.PublishSubject

fun <Msg : Any, State : Any, Effect : Any> Feature<Msg, State, Effect>.wrapWithObservableEffectHandler(
    initialEffects: Set<Effect> = emptySet(),
    transform: Observable<Effect>.() -> Observable<Msg>,
) = wrapWithObservableEffectHandler(
    initialEffects = initialEffects,
    TeaRxObservableEffectHandler(transform)
)

fun <Msg : Any, State : Any, Effect : Any> Feature<Msg, State, Effect>.wrapWithCompletableEffectHandler(
    initialEffects: Set<Effect> = emptySet(),
    transform: Observable<Effect>.() -> Completable,
) = wrapWithObservableEffectHandler(
    initialEffects = initialEffects,
    TeaRxCompletableEffectHandler(transform)
)

private class TeaRxObservableEffectHandler<Effect, Msg>(
    transform: Observable<Effect>.() -> Observable<Msg>,
) : TeaObservableEffectHandler<Effect, Msg> {
    private val commands: PublishSubject<Effect> = PublishSubject.create()
    private var listener: ((Msg) -> Unit)? = null

    private val subscription: Subscription = commands.transform()
        .subscribe { listener?.invoke(it) }

    override fun subscribe(listener: (Msg) -> Unit) {
        this.listener = listener
    }

    override fun invoke(eff: Effect) {
        commands.onNext(eff)
    }

    override fun dispose() {
        subscription.unsubscribeSafe()
        listener = null
    }
}

private class TeaRxCompletableEffectHandler<Effect, Msg>(
    transform: Observable<Effect>.() -> Completable,
) : TeaObservableEffectHandler<Effect, Msg> {
    private val commands: PublishSubject<Effect> = PublishSubject.create()

    private val subscription: Subscription = commands.transform().subscribe()

    override fun subscribe(listener: (Msg) -> Unit) {
        // do nothing
    }

    override fun invoke(eff: Effect) {
        commands.onNext(eff)
    }

    override fun dispose() {
        subscription.unsubscribeSafe()
    }
}
