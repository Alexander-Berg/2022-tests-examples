package ru.yandex.supercheck.domain.session

import ru.yandex.supercheck.model.domain.rateme.AppOpening
import ru.yandex.supercheck.model.domain.rateme.RateMeStatus

object TestData {

    const val ZERO_PAYMENT_COUNT = 0
    const val ODD_PAYMENT_COUNT = 3
    const val EVEN_PAYMENT_COUNT = 10

    private const val SESSION_TIME_IN_MILLIS = RateMeConfig.SESSION_TIME_IN_MILLIS
    private const val MIN_TIME = RateMeConfig.MIN_TIME_APP_USAGE_BEFORE_SHOW_IN_MILLIS

    val noMinTimeAppUsage = listOf(
        AppOpening("", 0L, RateMeStatus.NONE),
        AppOpening("", 1 * SESSION_TIME_IN_MILLIS + 1L, RateMeStatus.NONE),
        AppOpening("", 2 * SESSION_TIME_IN_MILLIS + 1L, RateMeStatus.NONE),
        AppOpening("", 3 * SESSION_TIME_IN_MILLIS + 1L, RateMeStatus.NONE),
        AppOpening("", 4 * SESSION_TIME_IN_MILLIS + 1L, RateMeStatus.NONE)
    )

    val noEnoughSessions = listOf(
        AppOpening("", 0L, RateMeStatus.NONE),
        AppOpening("", MIN_TIME, RateMeStatus.NONE),
        AppOpening("", MIN_TIME + SESSION_TIME_IN_MILLIS - 1, RateMeStatus.NONE)
    )

    val firstShowConditions: List<AppOpening> =
        mutableListOf(
            AppOpening("", 0L, RateMeStatus.NONE),
            AppOpening("", MIN_TIME, RateMeStatus.NONE)
        ).apply {
            for (i in 0..RateMeConfig.SESSIONS_INTERVAL_TO_SHOW - 2) {
                add(AppOpening("", last().timestamp + SESSION_TIME_IN_MILLIS, RateMeStatus.NONE))
            }
        }

    private val firstShownConditions
        get() = mutableListOf(
            *firstShowConditions.toTypedArray(),
            AppOpening(
                "",
                firstShowConditions.last().timestamp + SESSION_TIME_IN_MILLIS,
                RateMeStatus.SHOWN
            )
        )

    val afterEnoughCountNotShownSessions: List<AppOpening> =
        firstShownConditions.addIntervalWithCurrentOpening()

    val openingInsideShownSession: List<AppOpening> = mutableListOf(
        *firstShownConditions.toTypedArray(),
        AppOpening(
            "",
            firstShownConditions.last().timestamp + SESSION_TIME_IN_MILLIS - 1L,
            RateMeStatus.NONE
        )
    )

    val notEnoughPreviousSessionsCountWithNoneStatus: List<AppOpening> =
        firstShownConditions.apply {
            addInterval()
        }

    val sessionIsSkippedInPreviousAppOpening: List<AppOpening> = firstShownConditions.apply {
        add(AppOpening("", last().timestamp + SESSION_TIME_IN_MILLIS, RateMeStatus.SKIPPED))
        addInterval()
    }

    val skipIntervalIsPassed: List<AppOpening> =
        mutableListOf(*sessionIsSkippedInPreviousAppOpening.toTypedArray()).apply {
            add(AppOpening("", last().timestamp + SESSION_TIME_IN_MILLIS, RateMeStatus.NONE))
        }

    private fun getConditionsWithFirstShowStatus(status: RateMeStatus): MutableList<AppOpening> {
        return mutableListOf(
            *firstShowConditions.toMutableList().apply { removeAt(firstShowConditions.lastIndex) }.toTypedArray(),
            AppOpening(
                "",
                firstShowConditions.last().timestamp + SESSION_TIME_IN_MILLIS,
                status
            )
        )
    }

    private fun MutableList<AppOpening>.addIntervalWithCurrentOpening(): MutableList<AppOpening> {
        return apply {
            for (i in 0..RateMeConfig.SESSIONS_INTERVAL_TO_SHOW) {
                add(AppOpening("", last().timestamp + SESSION_TIME_IN_MILLIS, RateMeStatus.NONE))
            }
        }
    }

    private fun MutableList<AppOpening>.addInterval(): MutableList<AppOpening> {
        return apply {
            for (i in 0 until RateMeConfig.SESSIONS_INTERVAL_TO_SHOW) {
                add(AppOpening("", last().timestamp + SESSION_TIME_IN_MILLIS, RateMeStatus.NONE))
            }
        }
    }

    val skippedOnFirstPeriodNotPassed: List<AppOpening> =
        getConditionsWithFirstShowStatus(RateMeStatus.SKIPPED_ON_FIRST_PERIOD)
            .addIntervalWithCurrentOpening()

    val skippedOnFirstPeriodPassed: List<AppOpening> =
        getConditionsWithFirstShowStatus(RateMeStatus.SKIPPED_ON_FIRST_PERIOD).apply {
            addInterval()
            add(
                AppOpening(
                    "",
                    last().timestamp + RateMeConfig.FIRST_SKIP_TIME_IN_MILLIS,
                    RateMeStatus.NONE
                )
            )
        }

    val skippedOnSecondPeriodNotPassed: List<AppOpening> =
        getConditionsWithFirstShowStatus(RateMeStatus.SKIPPED_ON_SECOND_PERIOD).addIntervalWithCurrentOpening()

    val skippedOnSecondPeriodPassed: List<AppOpening> =
        getConditionsWithFirstShowStatus(RateMeStatus.SKIPPED_ON_SECOND_PERIOD).apply {
            addInterval()
            add(
                AppOpening(
                    "",
                    last().timestamp + RateMeConfig.SECOND_SKIP_TIME_IN_MILLIS,
                    RateMeStatus.NONE
                )
            )
        }

    val skippedOnThirdPeriodNotPassed: List<AppOpening> =
        getConditionsWithFirstShowStatus(RateMeStatus.SKIPPED_ON_THIRD_PERIOD).addIntervalWithCurrentOpening()

    val skippedOnThirdPeriodPassed: List<AppOpening> =
        getConditionsWithFirstShowStatus(RateMeStatus.SKIPPED_ON_THIRD_PERIOD).apply {
            addInterval()
            add(
                AppOpening(
                    "",
                    last().timestamp + RateMeConfig.THIRD_SKIP_TIME_IN_MILLIS,
                    RateMeStatus.NONE
                )
            )
        }

}