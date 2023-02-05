package ru.yandex.metro.utils.rx.extension

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import ru.yandex.metro.CallableSpek
import ru.yandex.metro.utils.android.network.NetworkConnectivity
import ru.yandex.metro.utils.android.network.NetworkState
import ru.yandex.metro.utils.rx.extension.retryWhenNetworkConnected

class ErrorHandlersSpec : CallableSpek(Maybe<*>::retryWhenNetworkConnected, {
    context("Not connected to the internet") {
        val errorMaybe = Maybe.error<Unit>(IllegalArgumentException())

        val networkStates = listOf(NetworkState.UNDEFINED, NetworkState.DISCONNECTED)

        val networkPublishSubject = PublishSubject.create<NetworkState>()
        val networkConnectivity = mock<NetworkConnectivity> {
            on { networkStateEvents } doReturn networkPublishSubject
        }

        for (state in networkStates) {
            context("Network state is $state") {
                it("No error is passed") {
                    val test = errorMaybe.retryWhenNetworkConnected(networkConnectivity)
                            .test()

                    networkPublishSubject.onNext(state)

                    test.assertSubscribed()
                    test.assertNoErrors()
                }
            }
        }
    }

    context("Connected to the internet after the task failed") {
        var resubscribed = false

        val errorMaybe = Maybe.create<Unit> { emitter ->
            if (resubscribed) {
                emitter.onError(IllegalArgumentException())
                resubscribed = true
            } else {
                emitter.onSuccess(Unit)
            }
        }

        context("Network state is ${NetworkState.CONNECTED_OR_CONNECTING}") {
            val networkPublishSubject = PublishSubject.create<NetworkState>()
            val networkConnectivity = mock<NetworkConnectivity> {
                on { networkStateEvents } doReturn networkPublishSubject
            }
            it("Completed successfully after retry") {
                val test = errorMaybe
                        .retryWhenNetworkConnected(networkConnectivity)
                        .test()

                networkPublishSubject.onNext(NetworkState.CONNECTED_OR_CONNECTING)

                test.assertSubscribed()
                test.assertComplete()
            }
        }
    }
})