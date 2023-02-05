// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.interactor.statistics

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.stub
import io.reactivex.observers.TestObserver
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.direct.domain.statistics.ReportColumn
import ru.yandex.direct.domain.statistics.ReportRow
import ru.yandex.direct.domain.statistics.SummaryReport
import ru.yandex.direct.loaders.impl.statistic.ReportTargetInfo
import ru.yandex.direct.utils.StubReportParser
import java.util.*

@RunWith(Parameterized::class)
class SummaryReportInteractorParameterizedTest(
        private val previousPeriodSummary: ReportRow?,
        private val currentPeriodSummary: ReportRow?,
        private val detailedReport: ReportRow?,
        @Suppress("unused") val testName: String
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {3}")
        fun provideTestParameters(): Collection<Array<out Any?>> {
            return listOf(
                    arrayOf(row(), row(), row(), "row-row-row"),
                    arrayOf(null, row(), row(), "null-row-row"),
                    arrayOf(row(), null, row(), "row-null-row"),
                    arrayOf(row(), row(), null, "row-row-null"),
                    arrayOf(row(), null, null, "row-null-null"),
                    arrayOf(null, row(), null, "null-row-null"),
                    arrayOf(null, null, row(), "null-null-row"),
                    arrayOf(null, null, null, "null-null-null")
            )
        }

        private fun row() = ReportRow(UUID.randomUUID()).apply { put(ReportColumn.CLICKS, 10) }
    }

    @Test
    fun fetchSummaryReportForced_works_inGeneralCase() {
        SummaryReportEnvironment().apply {
            remoteRepo.stub {
                on { fetch(any(), any()) } doReturn listOf(
                        parserFor(previousPeriodSummary),
                        parserFor(currentPeriodSummary),
                        parserFor(detailedReport)
                )
            }

            val observer = TestObserver.create<SummaryReport>()

            interactor.fetchSummaryReportForced(ReportTargetInfo.overall()).subscribe(observer)
            scheduler.triggerActions()

            observer.assertComplete().assertNoErrors().assertValueCount(1)

            val report = observer.values()[0]

            assertThat(report.detailedReport).isNotNull

            if (previousPeriodSummary != null) {
                assertThat(report.previousPeriodSummary.reportUuid).isEqualTo(previousPeriodSummary.reportUuid)
            } else {
                assertThat(report.previousPeriodSummary).isEmpty()
            }

            if (currentPeriodSummary != null) {
                assertThat(report.currentPeriodSummary.reportUuid).isEqualTo(currentPeriodSummary.reportUuid)
            } else {
                assertThat(report.currentPeriodSummary).isEmpty()
            }

            if (detailedReport != null) {
                assertThat(report.detailedReport).containsExactly(detailedReport)
            } else {
                assertThat(report.detailedReport).isEmpty()
            }
        }
    }

    private fun parserFor(reportRow: ReportRow?) =
            reportRow?.let { StubReportParser(it) } ?: StubReportParser.empty()
}
