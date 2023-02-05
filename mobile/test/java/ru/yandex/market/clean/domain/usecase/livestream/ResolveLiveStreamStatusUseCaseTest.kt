package ru.yandex.market.clean.domain.usecase.livestream

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.feature.videosnippets.ui.vo.LiveStreamType
import ru.yandex.market.datetime.DateTimeProvider
import ru.yandex.market.utils.Duration
import ru.yandex.market.utils.TimeUnit
import java.util.Date

@RunWith(Parameterized::class)
class ResolveLiveStreamStatusUseCaseTest(
    private val input: Input,
    private val output: LiveStreamType
) {

    private val dateTimeProvider = mock<DateTimeProvider>()
    private val resolveLiveStreamStatusUseCase = ResolveLiveStreamStatusUseCase(dateTimeProvider)

    @Test
    fun `Should return correct live status`() {
        whenever(dateTimeProvider.currentUnixTimeInMillis).thenReturn(input.currentTime)
        resolveLiveStreamStatusUseCase.execute(input.startTime, input.duration)
            .test()
            .assertValue(output)
    }

    class Input(
        val currentTime: Long,
        val startTime: Date,
        val duration: Duration
    )

    companion object {

        @Parameterized.Parameters
        @JvmStatic
        fun data(): Iterable<Array<*>> {
            return listOf(
                //0
                arrayOf(
                    Input(
                        currentTime = 5000L,
                        startTime = Date(5000),
                        duration = Duration(200.0, TimeUnit.MILLISECONDS)
                    ),
                    LiveStreamType.ON_AIR
                ),
                //1
                arrayOf(
                    Input(
                        currentTime = 5000L,
                        startTime = Date(4900),
                        Duration(200.0, TimeUnit.MILLISECONDS)
                    ),
                    LiveStreamType.ON_AIR
                ),
                //2
                arrayOf(
                    Input(
                        currentTime = 5201L,
                        startTime = Date(5000),
                        Duration(200.0, TimeUnit.MILLISECONDS)
                    ),
                    LiveStreamType.FINISHED
                ),
                //3
                arrayOf(
                    Input(
                        currentTime = 5000L,
                        startTime = Date(6000),
                        Duration(200.0, TimeUnit.MILLISECONDS)
                    ),
                    LiveStreamType.SCHEDULED
                )
            )
        }
    }
}