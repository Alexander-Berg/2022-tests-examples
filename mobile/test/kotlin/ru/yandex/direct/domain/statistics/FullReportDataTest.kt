// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.domain.statistics

import com.nhaarman.mockito_kotlin.mock
import io.reactivex.schedulers.TestScheduler
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.Test
import ru.yandex.direct.data.ApiSampleData
import ru.yandex.direct.domain.daterange.DateRange
import ru.yandex.direct.loaders.impl.statistic.FullReportSettings
import ru.yandex.direct.loaders.impl.statistic.ReportTargetInfo
import ru.yandex.direct.repository.statistics.ReportColumnsIndex
import ru.yandex.direct.repository.statistics.RxReportParser
import ru.yandex.direct.util.CloseableList
import ru.yandex.direct.utils.toCloseable
import ru.yandex.direct.web.report.request.DateRangeType
import ru.yandex.direct.web.report.response.Report
import java.util.UUID

class FullReportDataTest {
    private val aggregationTestSettings = FullReportSettings.Builder()
            .setMetrics(Metrics.values().toList())
            .setDateRange(DateRange.fromPreset(DateRangeType.LAST_7_DAYS))
            .setReportTargetInfo(ReportTargetInfo.overall())
            .setGrouping(Grouping.SELECTED_RANGE)
            .setSection(Section.CAMPAIGN)
            .setIncludeVat(true)
            .build()

    private val aggregationTestData = ApiSampleData::class.java.getResource("aggregation-test-data.tsv").readText()

    private val aggregationTestTotal = ApiSampleData::class.java.getResource("aggregation-test-total.tsv").readText()

    @Test
    fun updateSectionsList_shouldPrepareDataCorrectly_forReportByDay() {
        val sortOrder = StatisticsSortOrder.bySectionCriteria(Section.MOBILE_PLATFORM, false)
        val rows = parseReportRows(ApiSampleData.reportByDaySettings, ApiSampleData.reportByDay)
        val fullReportData = FullReportData(sortOrder, rows)
        assertThat(fullReportData.firstInSection).containsExactlyElementsOf(listOf(0, 8, 16))
        assertThat(fullReportData.hasSingleRowForEachSection()).isFalse()
    }

    @Test
    fun updateSectionsList_shouldPrepareDataCorrectly_forOverallReport() {
        val sortOrder = StatisticsSortOrder.bySectionCriteria(Section.MOBILE_PLATFORM, false)
        val rows = parseReportRows(ApiSampleData.reportOverallSettings, ApiSampleData.reportOverall)
        val fullReportData = FullReportData(sortOrder, rows)
        assertThat(fullReportData.firstInSection).containsExactlyElementsOf(listOf(0, 1, 2))
        assertThat(fullReportData.hasSingleRowForEachSection()).isTrue()
    }

    @Test
    fun totalLine_shouldBeCalculatedCorrectly_forFullReport() {
        runTotalLineTest(reverse = false)
    }

    @Test
    fun totalLine_shouldBeCalculatedCorrectly_forReversedReport() {
        runTotalLineTest(reverse = true)
    }

    private fun runTotalLineTest(reverse: Boolean) {
        val rows = parseReportRows(aggregationTestSettings, aggregationTestData)
        val total = parseReportRows(aggregationTestSettings, aggregationTestTotal)[0]
        val data = FullReportData(StatisticsSortOrder.bySectionCriteria(Section.CAMPAIGN, reverse), rows)
        for (column in aggregationTestSettings.reportColumns) {
            if (column is ReportMetricsColumn<*>) {
                assertThat(data.totalRow!![column]!!.toDouble())
                        .isCloseTo(total[column]!!.toDouble(), Offset.offset(0.01))
            }
        }
    }

    private fun parseReportRows(settings: FullReportSettings, rawReport: String): CloseableList<ReportRow> {
        val report = Report.fromString(rawReport)
        val index = ReportColumnsIndex(report)
        val parser = RxReportParser(TestScheduler(), report, UUID.randomUUID(), settings.reportColumns, index, mock())
        return parser.parse().sortedWith(compareBy({ it.sectionCriteria }, { it.timestamp })).toCloseable()
    }
}