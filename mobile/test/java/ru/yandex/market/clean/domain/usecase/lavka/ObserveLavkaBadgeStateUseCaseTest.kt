package ru.yandex.market.clean.domain.usecase.lavka

import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.clean.data.repository.lavka.LavkaScreenVisitedRepository
import ru.yandex.market.clean.data.repository.lavka.LavkaStartupRepository
import ru.yandex.market.clean.domain.model.lavka2.Lavka24HoursBadgeConfig
import ru.yandex.market.clean.domain.model.lavka2.LavkaBadgeContext
import ru.yandex.market.clean.domain.model.lavka2.LavkaBadgeState
import ru.yandex.market.clean.domain.model.lavka2.LavkaDiscountInfo
import ru.yandex.market.clean.domain.model.lavka2.LavkaServiceInfo
import ru.yandex.market.clean.domain.model.lavka2.LavkaServiceInfoStatus
import ru.yandex.market.clean.domain.model.lavka2.LavkaStartupInfo
import ru.yandex.market.clean.domain.usecase.lavka2.ObserveLavkaServiceInfoUseCase
import ru.yandex.market.clean.domain.usecase.lavka2.badge.ObserveLavkaBadgeStateUseCase
import ru.yandex.market.common.dateformatter.DateFormatter
import ru.yandex.market.datetime.DateTimeProvider
import ru.yandex.market.optional.Optional
import ru.yandex.market.utils.createDate
import java.util.Date

@RunWith(Parameterized::class)
class ObserveLavkaBadgeStateUseCaseTest(
    private val arguments: InputArguments,
    private val expectedState: LavkaBadgeState
) {

    data class InputArguments(
        val badgeContext: LavkaBadgeContext,
        val currentDateTime: Date,
        val lavkaLastVisitTime: Date?,
        val lavkaVisitedDuringSession: Boolean,
        val lavka24HoursBadgeConfig: Lavka24HoursBadgeConfig?,
        val lavkaDiscountInfo: LavkaDiscountInfo?
    )

    private val lavkaScreenVisitedRepository = mock<LavkaScreenVisitedRepository> {
        on { observeVisitedOnce() } doReturn Observable.just(arguments.lavkaLastVisitTime != null)
        on { observeVisitedDuringSession() } doReturn Observable.just(arguments.lavkaVisitedDuringSession)
        on { getLavkaTabVisitedTime() } doReturn Single.just(Optional.ofNullable(arguments.lavkaLastVisitTime))
    }

    private val observeServiceInfoUseCase = mock<ObserveLavkaServiceInfoUseCase> {
        on { execute() } doReturn Observable.just(
            Optional.ofNullable(
                LavkaServiceInfo(
                    status = LavkaServiceInfoStatus.OPEN,
                    availableAt = null,
                    deliveryText = null,
                    eta = null,
                    depotId = null,
                    isSurge = false,
                    availability = null,
                    rewardBlocks = emptyList(),
                    informers = emptyList(),
                    isLavkaNewbie = false,
                    juridicalInfo = null,
                    lavkaDiscountInfo = arguments.lavkaDiscountInfo
                )
            )
        )
    }

    private val lavkaStartUpInfo: LavkaStartupInfo = mock {
        on { lavka24BadgeShowTimeConfig } doReturn arguments.lavka24HoursBadgeConfig
    }

    private val lavkaStartupRepository = mock<LavkaStartupRepository> {
        on { getStartupInfo() } doReturn Single.just(Optional.of(lavkaStartUpInfo))
    }

    private val dateTimeProvider = mock<DateTimeProvider> {
        on { currentDateTime } doReturn arguments.currentDateTime
    }

    private val dateFormatter = DateFormatter(mock(), dateTimeProvider)

    private val observeLavkaBadgeStateUseCase = ObserveLavkaBadgeStateUseCase(
        lavkaScreenVisitedRepository,
        observeServiceInfoUseCase,
        lavkaStartupRepository,
        dateTimeProvider,
        dateFormatter
    )

    @Test
    fun `check lavka badge state`() {
        observeLavkaBadgeStateUseCase.execute(arguments.badgeContext)
            .test()
            .assertNoErrors()
            .assertValue(expectedState)
    }

    companion object {
        private val LAVKA_24H_BADGE_CONFIG_ONE_DAY = Lavka24HoursBadgeConfig(
            showFrom = "19:00",
            showTo = "22:00"
        )

        private val LAVKA_24H_BADGE_CONFIG_CROSS_MIDNIGHT = Lavka24HoursBadgeConfig(
            showFrom = "20:00",
            showTo = "07:00"
        )

        private val DATETIME_CURRENT_MIDNIGHT_1AM = createDate(2022, 4, 7, hour = 2)
        private val DATETIME_CURRENT_DAY = createDate(2022, 4, 7, hour = 15)
        private val DATETIME_CURRENT_EVENING = createDate(2022, 4, 7, hour = 21)
        private val DATETIME_NEXT_MIDNIGHT_1AM = createDate(2022, 4, 8, hour = 1)
        private val DATETIME_NEXT_MIDNIGHT_2AM = createDate(2022, 4, 8, hour = 2)

        private fun argsFor24HoursBadgeTest(
            currentDateTime: Date,
            badgeConfig: Lavka24HoursBadgeConfig,
            lavkaLastVisitTime: Date? = null,
        ) = InputArguments(
            badgeContext = LavkaBadgeContext.NAVIGATION_BAR,
            currentDateTime = currentDateTime,
            lavkaLastVisitTime = lavkaLastVisitTime,
            lavkaVisitedDuringSession = false,
            lavka24HoursBadgeConfig = badgeConfig,
            lavkaDiscountInfo = null
        )

        @Parameterized.Parameters(name = "{index}: expected {1}")
        @JvmStatic
        fun data(): Iterable<Array<*>> = listOf(
            // 0
            // Lavka badge on navigation tab, out of 24 hours badge show interval (within one day)
            arrayOf(
                argsFor24HoursBadgeTest(
                    currentDateTime = DATETIME_CURRENT_DAY,
                    badgeConfig = LAVKA_24H_BADGE_CONFIG_ONE_DAY
                ),
                LavkaBadgeState.NewBadge
            ),

            // 1
            // Lavka badge on navigation tab, within 24 hours badge show interval (within one day)
            arrayOf(
                argsFor24HoursBadgeTest(
                    currentDateTime = DATETIME_CURRENT_EVENING,
                    badgeConfig = LAVKA_24H_BADGE_CONFIG_ONE_DAY
                ),
                LavkaBadgeState.TwentyFourHoursBadge
            ),

            // 2
            // Lavka badge on navigation tab, out of 24 hours badge show interval (crossing midnight)
            arrayOf(
                argsFor24HoursBadgeTest(
                    currentDateTime = DATETIME_CURRENT_DAY,
                    badgeConfig = LAVKA_24H_BADGE_CONFIG_CROSS_MIDNIGHT
                ),
                LavkaBadgeState.NewBadge
            ),

            // 3
            // Lavka badge on navigation tab, within 24 hours badge show interval (crossing midnight, before midnight)
            arrayOf(
                argsFor24HoursBadgeTest(
                    currentDateTime = DATETIME_CURRENT_EVENING,
                    badgeConfig = LAVKA_24H_BADGE_CONFIG_CROSS_MIDNIGHT
                ),
                LavkaBadgeState.TwentyFourHoursBadge
            ),

            // 4
            // Lavka badge on navigation tab, within 24 hours badge show interval (crossing midnight, after midnight)
            arrayOf(
                argsFor24HoursBadgeTest(
                    currentDateTime = DATETIME_NEXT_MIDNIGHT_2AM,
                    badgeConfig = LAVKA_24H_BADGE_CONFIG_CROSS_MIDNIGHT
                ),
                LavkaBadgeState.TwentyFourHoursBadge
            ),

            // 5
            // Don't show 24/7 badge on navigation tab after lavka visit during current 24/7 badge show interval (yesterday)
            arrayOf(
                argsFor24HoursBadgeTest(
                    currentDateTime = DATETIME_NEXT_MIDNIGHT_2AM,
                    badgeConfig = LAVKA_24H_BADGE_CONFIG_CROSS_MIDNIGHT,
                    lavkaLastVisitTime = DATETIME_CURRENT_EVENING
                ),
                LavkaBadgeState.NoBadge
            ),

            // 6
            // Don't show 24/7 badge on navigation tab after lavka visit during current 24/7 badge show interval (today)
            arrayOf(
                argsFor24HoursBadgeTest(
                    currentDateTime = DATETIME_NEXT_MIDNIGHT_2AM,
                    badgeConfig = LAVKA_24H_BADGE_CONFIG_CROSS_MIDNIGHT,
                    lavkaLastVisitTime = DATETIME_NEXT_MIDNIGHT_1AM
                ),
                LavkaBadgeState.NoBadge
            ),

            // 7
            // Show 24/7 badge on navigation tab after lavka visit during previous 24/7 badge show interval (today)
            arrayOf(
                argsFor24HoursBadgeTest(
                    currentDateTime = DATETIME_CURRENT_EVENING,
                    badgeConfig = LAVKA_24H_BADGE_CONFIG_CROSS_MIDNIGHT,
                    lavkaLastVisitTime = DATETIME_CURRENT_MIDNIGHT_1AM
                ),
                LavkaBadgeState.TwentyFourHoursBadge
            )

        )

    }
}
