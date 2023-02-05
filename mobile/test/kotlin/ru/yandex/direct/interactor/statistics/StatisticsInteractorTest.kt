// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.interactor.statistics

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.stub
import com.nhaarman.mockito_kotlin.verify
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.TestScheduler
import org.assertj.core.api.AbstractBooleanAssert
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import ru.yandex.direct.Configuration
import ru.yandex.direct.data.ApiSampleData
import ru.yandex.direct.domain.daterange.DateRange
import ru.yandex.direct.domain.daterange.Duration
import ru.yandex.direct.domain.statistics.FullReport
import ru.yandex.direct.domain.statistics.FullReportData
import ru.yandex.direct.domain.statistics.Grouping
import ru.yandex.direct.domain.statistics.Metrics
import ru.yandex.direct.domain.statistics.ReportColumn
import ru.yandex.direct.domain.statistics.ReportRow
import ru.yandex.direct.domain.statistics.Section
import ru.yandex.direct.domain.statistics.StatisticsLocalSettings
import ru.yandex.direct.domain.statistics.StatisticsSortOrder
import ru.yandex.direct.interactor.clients.CurrentClientInteractor
import ru.yandex.direct.loaders.impl.statistic.FullReportSettings
import ru.yandex.direct.loaders.impl.statistic.ReportTarget
import ru.yandex.direct.loaders.impl.statistic.ReportTargetInfo
import ru.yandex.direct.newui.Constants
import ru.yandex.direct.repository.statistics.ReportLocalRepository
import ru.yandex.direct.repository.statistics.ReportParser
import ru.yandex.direct.repository.statistics.StatisticsLocalRepository
import ru.yandex.direct.repository.statistics.StatisticsRemoteRepository
import ru.yandex.direct.web.report.request.DateRangeType
import java.util.*

class StatisticsInteractorTest {
    private lateinit var localRepo: StatisticsLocalRepository

    private lateinit var remoteRepo: StatisticsRemoteRepository

    private lateinit var reportRepo: ReportLocalRepository

    private lateinit var configuration: Configuration

    private lateinit var interactor: StatisticsInteractor

    private lateinit var clientInteractor: CurrentClientInteractor

    private val scheduler = TestScheduler()

    private val reportUuid = UUID.randomUUID()

    private val reportCurrency = ApiSampleData.currency[0]

    @Before
    fun runBeforeEachTest() {
        localRepo = mock()
        remoteRepo = mock()
        reportRepo = mock()
        configuration = mock()
        clientInteractor = mock {
            on { currency } doReturn reportCurrency
            on { currentClientInfo } doReturn Maybe.just(ApiSampleData.clientInfo)
        }
        interactor = StatisticsInteractor(mock(), localRepo, remoteRepo, reportRepo,
                scheduler, scheduler, scheduler, clientInteractor,
                StatisticsSettingsInteractor(configuration, scheduler))
    }

    @Test
    fun getFullReportFromDb_shouldLoadReportData() {
        val reportTargetInfo = ReportTargetInfo.overall()
        val reportData = mock<FullReportData>()
        val report = getSomeReport(emptyList())
        val sortOrder = StatisticsSortOrder.bySectionCriteria(Section.CAMPAIGN, false)
        val reportKey = ReportKey.forTarget(reportTargetInfo)
        localRepo.stub {
            on { selectForFullReport(report.uuid, sortOrder, reportTargetInfo.kind) } doReturn reportData
        }
        reportRepo.stub {
            on { selectLatestFullReport(reportKey) } doReturn report
        }
        val observer = TestObserver.create<FullReport>()
        interactor.getFullReportFromDb(reportKey, sortOrder, reportTargetInfo.kind).subscribe(observer)
        scheduler.triggerActions()
        verify(reportRepo).selectLatestFullReport(reportKey)
        verify(localRepo).selectForFullReport(report.uuid, sortOrder, reportTargetInfo.kind)
        observer.assertValueCount(1)
        assertThat(observer.values()[0].data).isSameAs(reportData)
    }

    @Test
    fun fetchFullReportForced_shouldCacheReportAndPerformCleanup() {
        val sortOrder = StatisticsSortOrder.bySectionCriteria(Section.CAMPAIGN, false)
        val reportData = mock<FullReportData> {
            on { data } doReturn emptyList<ReportRow>()
        }
        val desiredSettings = StatisticsLocalSettings.getDefault(ReportTarget.OVERALL)
        val reportSettings = getReportForSettings(desiredSettings, emptyList()).build()
        val reportKey = ReportKey.forTarget(ReportTargetInfo.overall())
        val reportParser = mock<ReportParser> {
            on { parse() } doReturn emptyList<ReportRow>()
            on { parseAsync() } doReturn Observable.just(emptyList())
        }
        localRepo.stub {
            on { selectForFullReport(any(), any(), any()) } doReturn reportData
        }
        remoteRepo.stub {
            on { fetch(any(), eq(reportSettings)) } doReturn reportParser
        }
        configuration.stub {
            on { statisticsDateRange } doReturn DateRange.DEFAULT
            on { isReportVatEnabled } doReturn true
            on { enabledMetrics } doReturn listOf(Metrics.IMPRESSIONS, Metrics.CLICKS)
        }
        val observer = TestObserver.create<FullReport>()
        interactor.fetchFullReportForced(desiredSettings, ReportTargetInfo.overall(), reportKey, sortOrder)
                .subscribe(observer)
        scheduler.triggerActions()
        observer.assertValueCount(1)
        val fetchedReport = observer.values()[0]
        verify(reportRepo).bind(reportKey, fetchedReport)
    }

    @Test
    fun tryUpdateReportMetrics_shouldFilterReportMetrics_ifReportContainsAllMetrics() {
        configuration.stub {
            on { enabledMetrics } doReturn listOf(Metrics.IMPRESSIONS, Metrics.CLICKS)
        }
        val someReport = getSomeReport(listOf(Metrics.CLICKS, Metrics.COST, Metrics.IMPRESSIONS))
        assertThat(interactor.tryUpdateReportMetrics(someReport).metrics)
                .isEqualTo(listOf(Metrics.IMPRESSIONS, Metrics.CLICKS))
    }

    @Test
    fun tryUpdateReportMetrics_shouldKeepReportAsIs_ifReportDoesNotContainDesiredMetrics() {
        configuration.stub {
            on { enabledMetrics } doReturn listOf(Metrics.COST)
        }
        val someReport = getSomeReport(listOf(Metrics.IMPRESSIONS, Metrics.CLICKS))
        assertThat(interactor.tryUpdateReportMetrics(someReport).metrics)
                .isEqualTo(listOf(Metrics.IMPRESSIONS, Metrics.CLICKS))
    }

    @Test
    fun tryUpdateReportMetrics_shouldKeepReportData_ifFullReportHasData() {
        configuration.stub {
            on { enabledMetrics } doReturn listOf(Metrics.COST)
        }
        val someReport = getSomeReport(listOf(Metrics.COST, Metrics.IMPRESSIONS))
        someReport.data = mock()
        assertThat(interactor.tryUpdateReportMetrics(someReport).data).isSameAs(someReport.data)
    }

    @Test
    fun isReportUpdateRequired_returnsTrue_ifReportIsNull() {
        assertThat(interactor.isReportUpdateRequired(null, StatisticsLocalSettings.getDefault(ReportTarget.OVERALL)))
                .isTrue()
    }

    @Test
    fun isReportUpdateRequired_returnsFalse_ifReportIsFresh() {
        assertUpdateRequiredForReportWithTimestamp(Date()).isFalse()
    }

    @Test
    fun isReportUpdateRequired_returnsTrue_ifReportIsRotten() {
        assertUpdateRequiredForReportWithTimestamp(
                Constants.INVALIDATION_INTERVAL.add(Duration.millis(1)).subtractFrom(Date())
        ).isTrue()
    }

    @Test
    fun isReportUpdateRequired_returnsTrue_ifVatSettingsChanged() {
        assertUpdateRequiredForReportWithSettings { setIncludeVat(true) }.isTrue()
    }

    @Test
    fun isReportUpdateRequired_returnsTrue_ifGroupingChanged() {
        assertUpdateRequiredForReportWithSettings { setGrouping(Grouping.DATE) }.isTrue()
    }

    @Test
    fun isReportUpdateRequired_returnsTrue_ifSectionChanged() {
        assertUpdateRequiredForReportWithSettings { setSection(Section.MOBILE_PLATFORM) }.isTrue()
    }

    @Test
    fun isReportUpdateRequired_returnsTrue_ifDateRangeChanged() {
        assertUpdateRequiredForReportWithSettings { setDateRange(DateRange.fromPreset(DateRangeType.LAST_365_DAYS)) }
                .isTrue()
    }

    @Test
    fun isReportUpdateRequired_returnsTrue_ifNeedToLoadNewMetrics() {
        assertUpdateRequiredForReportWithSettings { setMetrics(listOf(Metrics.COST, Metrics.CLICKS)) }.isTrue()
    }

    @Test
    fun isReportUpdateRequired_returnsFalse_ifReportHasRequiredMetrics() {
        assertUpdateRequiredForReportWithSettings { setMetrics(listOf(Metrics.IMPRESSIONS, Metrics.CLICKS)) }.isFalse()
    }

    @Test
    fun aggregateRowsWithSameSection_shouldNotAggregate_ifHasOnlyOneRow() {
        val source = listOf(
                row(0, "1", 10)
        )
        runAggregateRowsWithSameSectionTest(source, emptyList(), emptyList())
    }

    @Test
    fun aggregateRowsWithSameSection_shouldAggregate_ifHasTwoRows() {
        val source = listOf(
                row(0, "1", 10),
                row(1, "1", 10)
        )
        runAggregateRowsWithSameSectionTest(source, listOf(row(null, "1", 20)), source)
    }

    @Test
    fun aggregateRowsWithSameSection_shouldNotAggregate_ifGotEmptyList() {
        runAggregateRowsWithSameSectionTest(emptyList(), emptyList(), emptyList())
    }

    @Test
    fun aggregateRowsWithSameSection_shouldNotAggregate_ifNothingToAggregate() {
        val source = listOf(
                row(0, "0", 10),
                row(1, "1", 10),
                row(2, "2", 10),
                row(3, "3", 10)
        )
        runAggregateRowsWithSameSectionTest(source, emptyList(), emptyList())
    }

    @Test
    fun aggregateRowsWithSameSection_shouldAggregate_inTheMiddleOfTheList() {
        val source = listOf(
                row(0, "1", 10),
                row(1, "2", 10),
                row(2, "2", 10),
                row(3, "3", 10)
        )
        val toInsert = listOf(
                row(null, "2", 20)
        )
        val toDelete = listOf(
                row(1, "2", 10),
                row(2, "2", 10)
        )
        runAggregateRowsWithSameSectionTest(source, toInsert, toDelete)
    }

    @Test
    fun aggregateRowsWithSameSection_shouldAggregate_inTheEndOfTheList() {
        val source = listOf(
                row(0, "1", 10),
                row(1, "2", 10),
                row(2, "3", 10),
                row(3, "3", 10)
        )
        val toInsert = listOf(
                row(null, "3", 20)
        )
        val toDelete = listOf(
                row(2, "3", 10),
                row(3, "3", 10)
        )
        runAggregateRowsWithSameSectionTest(source, toInsert, toDelete)
    }

    @Test
    fun aggregateRowsWithSameSection_shouldAggregate_inTheBeginningOfTheList() {
        val source = listOf(
                row(0, "1", 10),
                row(1, "1", 10),
                row(2, "2", 10),
                row(3, "3", 10)
        )
        val toInsert = listOf(
                row(null, "1", 20)
        )
        val toDelete = listOf(
                row(0, "1", 10),
                row(1, "1", 10)
        )
        runAggregateRowsWithSameSectionTest(source, toInsert, toDelete)
    }

    @Test
    fun aggregateRowsWithSameSection_shouldAggregate_twoPairs() {
        val source = listOf(
                row(0, "1", 10),
                row(1, "1", 10),
                row(2, "2", 10),
                row(3, "2", 10)
        )
        val toInsert = listOf(
                row(null, "1", 20),
                row(null, "2", 20)
        )
        runAggregateRowsWithSameSectionTest(source, toInsert, source)
    }

    @Test
    fun aggregateRowsWithSameSection_shouldAggregate_twoSeparatedPairs() {
        val source = listOf(
                row(0, "1", 10),
                row(1, "1", 10),
                row(2, "2", 10),
                row(3, "3", 10),
                row(4, "3", 10)
        )
        val toInsert = listOf(
                row(null, "1", 20),
                row(null, "3", 20)
        )
        val toDelete = listOf(
                row(0, "1", 10),
                row(1, "1", 10),
                row(3, "3", 10),
                row(4, "3", 10)
        )
        runAggregateRowsWithSameSectionTest(source, toInsert, toDelete)
    }

    private fun runAggregateRowsWithSameSectionTest(
            source: List<ReportRow>, toInsert: List<ReportRow>, toDelete: List<ReportRow>
    ) {
        val res = interactor.doAggregateRowsWithSameSection(source)
        assertThat(res.rowsToInsert).usingRecursiveFieldByFieldElementComparator().containsExactlyElementsOf(toInsert)
        assertThat(res.rowsToDelete).usingRecursiveFieldByFieldElementComparator().containsExactlyElementsOf(toDelete)
    }

    private fun assertUpdateRequiredForReportWithTimestamp(reportDate: Date) =
            runReportSettingsTest(reportDate, { })

    private fun assertUpdateRequiredForReportWithSettings(settingsEditor: FullReportSettings.Builder.() -> Unit) =
            runReportSettingsTest(Date(), settingsEditor)

    private fun runReportSettingsTest(
            reportDate: Date,
            settingsEditor: FullReportSettings.Builder.() -> Unit
    ): AbstractBooleanAssert<*> {
        val metrics = listOf(Metrics.IMPRESSIONS)
        configuration.stub {
            on { enabledMetrics } doReturn metrics
            on { isReportVatEnabled } doReturn false
            on { statisticsDateRange } doReturn DateRange.DEFAULT
        }
        val desiredSettings = StatisticsLocalSettings.getDefault(ReportTarget.OVERALL)
        val reportSettings = getReportForSettings(desiredSettings, metrics)
        settingsEditor(reportSettings)
        val report = FullReport(reportUuid, reportSettings.build(), reportDate, reportCurrency)
        return assertThat(interactor.isReportUpdateRequired(report, desiredSettings))
    }

    private fun getReportForSettings(settings: StatisticsLocalSettings, metrics: List<Metrics>) =
            FullReportSettings.Builder()
                    .setSection(settings.section)
                    .setGrouping(settings.grouping)
                    .setReportTargetInfo(ReportTargetInfo.overall())
                    .setIncludeVat(false)
                    .setMetrics(metrics)
                    .setDateRange(DateRange.DEFAULT)

    private fun getSomeReport(metrics: List<Metrics>) =
            FullReport(
                    reportUuid,
                    getReportForSettings(StatisticsLocalSettings.getDefault(ReportTarget.OVERALL), metrics).build(),
                    Date(),
                    reportCurrency
            )

    private fun row(id: Long?, section: String, clicks: Long): ReportRow {
        val row = ReportRow(reportUuid)
        row.id = id
        row.sectionCriteria = section
        row.sectionCriteriaId = section.hashCode().toLong()
        row.sectionCriteriaExtra = section
        row.put(ReportColumn.CLICKS, clicks)
        return row
    }
}