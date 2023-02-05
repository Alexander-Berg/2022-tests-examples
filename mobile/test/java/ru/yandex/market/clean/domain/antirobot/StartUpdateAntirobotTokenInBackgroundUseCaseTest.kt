package ru.yandex.market.clean.domain.antirobot

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.TestScheduler
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.domain.model.antirobot.JwsToken
import ru.yandex.market.clean.domain.usecase.health.facades.AntirobotHealthFacade
import ru.yandex.market.common.schedulers.DataSchedulers
import ru.yandex.market.internal.DaemonTaskManager
import java.util.concurrent.TimeUnit

class StartUpdateAntirobotTokenInBackgroundUseCaseTest {

    private val scheduler = TestScheduler()

    private val dataSchedulers = DataSchedulers(
        networking = scheduler,
        worker = scheduler,
        localSingleThread = scheduler
    )

    private val daemonTaskManager = DaemonTaskManager(dataSchedulers)

    private val updateAntirobotTokenUseCase = mock<UpdateAntirobotTokenUseCase>()

    private val getAntirobotTokenUseCase = mock<GetAntirobotTokenUseCase>()

    private val antirobotHealthFacade = mock<AntirobotHealthFacade>()

    private val startUpdateAntirobotTokenInBackgroundUseCase = StartUpdateAntirobotTokenInBackgroundUseCase(
        updateAntirobotTokenUseCase = updateAntirobotTokenUseCase,
        daemonTaskManager = daemonTaskManager,
        dataSchedulers = dataSchedulers,
        getAntirobotTokenUseCase = getAntirobotTokenUseCase,
        antirobotHealthFacade = antirobotHealthFacade
    )

    private val jwsToken = JwsToken.Valid(
        token = "some-token",
        expirationTimeInMillis = 10000L,
        timeUtilExpiredInMillis = 2000L
    )

    @Test
    @Suppress("TooGenericExceptionThrown")
    fun `Do retry when source is failed`() {
        var launchCount = 0
        val completable = Completable.fromCallable {
            launchCount++
            throw Exception("Some unexpected error")
        }
        whenever(updateAntirobotTokenUseCase.execute())
            .thenReturn(completable)

        startUpdateAntirobotTokenInBackgroundUseCase.execute(true)

        assertThat(launchCount).isEqualTo(0)
        scheduler.advanceTimeBy(60, TimeUnit.SECONDS)
        assertThat(launchCount).isEqualTo(2)
        scheduler.advanceTimeBy(120, TimeUnit.SECONDS)
        assertThat(launchCount).isEqualTo(3)
        scheduler.advanceTimeBy(180, TimeUnit.SECONDS)
        assertThat(launchCount).isEqualTo(4)
    }

    @Test
    fun `Repeats token load when token expired`() {
        var launchCount = 0
        val completable = Completable.fromCallable {
            launchCount++
        }

        whenever(updateAntirobotTokenUseCase.execute())
            .thenReturn(completable)
        whenever(getAntirobotTokenUseCase.execute())
            .thenReturn(Single.just(jwsToken))

        startUpdateAntirobotTokenInBackgroundUseCase.execute(true)

        assertThat(launchCount).isEqualTo(0)
        scheduler.triggerActions()
        assertThat(launchCount).isEqualTo(1)
        scheduler.advanceTimeBy(1, TimeUnit.SECONDS)
        assertThat(launchCount).isEqualTo(1)
        scheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        assertThat(launchCount).isEqualTo(2)
    }

    @Test
    @Suppress("TooGenericExceptionThrown")
    fun `Retries token load when failed and then repeats when token expired`() {
        var launchCount = 0
        var fail = true

        val completable = Completable.fromCallable {
            launchCount++
            if (fail) throw Exception("Some unexpected error")
        }

        whenever(updateAntirobotTokenUseCase.execute())
            .thenReturn(completable)
        whenever(getAntirobotTokenUseCase.execute())
            .thenReturn(Single.just(jwsToken))

        startUpdateAntirobotTokenInBackgroundUseCase.execute(true)

        assertThat(launchCount).isEqualTo(0)
        scheduler.triggerActions()
        assertThat(launchCount).isEqualTo(1)
        scheduler.advanceTimeBy(60, TimeUnit.SECONDS)
        assertThat(launchCount).isEqualTo(2)

        fail = false

        scheduler.advanceTimeBy(120, TimeUnit.SECONDS)
        assertThat(launchCount).isEqualTo(3)

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS)
        assertThat(launchCount).isEqualTo(3)

        scheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        assertThat(launchCount).isEqualTo(4)
    }

    @Test
    fun `Start updating jws token with delay`() {
        var launchCount = 0
        val completable = Completable.fromCallable {
            launchCount++
        }

        whenever(updateAntirobotTokenUseCase.execute())
            .thenReturn(completable)
        whenever(getAntirobotTokenUseCase.execute())
            .thenReturn(Single.just(jwsToken))

        startUpdateAntirobotTokenInBackgroundUseCase.execute(false)

        scheduler.triggerActions()
        assertThat(launchCount).isEqualTo(0)
        scheduler.advanceTimeBy(1, TimeUnit.SECONDS)
        assertThat(launchCount).isEqualTo(0)
        scheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        assertThat(launchCount).isEqualTo(1)
    }
}
