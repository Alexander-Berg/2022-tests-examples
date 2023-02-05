// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.newui.statistics

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.stub
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.ArgumentCaptor
import ru.yandex.direct.data.ApiSampleData
import ru.yandex.direct.db.statistics.ReportDao
import ru.yandex.direct.db.statistics.ReportDatabaseEntry
import ru.yandex.direct.domain.daterange.DateRange
import ru.yandex.direct.domain.statistics.FullReport
import ru.yandex.direct.domain.statistics.FullReportData
import ru.yandex.direct.domain.statistics.Grouping
import ru.yandex.direct.domain.statistics.Metrics
import ru.yandex.direct.domain.statistics.Section
import ru.yandex.direct.domain.statistics.StatisticsLocalSettings
import ru.yandex.direct.domain.statistics.StatisticsSortOrder
import ru.yandex.direct.interactor.clients.CurrentClientInteractor
import ru.yandex.direct.interactor.statistics.ReportKey
import ru.yandex.direct.interactor.statistics.StatisticsInteractor
import ru.yandex.direct.interactor.statistics.StatisticsSettingsInteractor
import ru.yandex.direct.loaders.impl.statistic.FullReportSettings
import ru.yandex.direct.loaders.impl.statistic.ReportTarget
import ru.yandex.direct.loaders.impl.statistic.ReportTargetInfo
import ru.yandex.direct.repository.statistics.ReportLocalRepository
import ru.yandex.direct.repository.statistics.StatisticsLocalRepository
import ru.yandex.direct.repository.statistics.StatisticsRemoteRepository
import ru.yandex.direct.utils.CurrencyInitializer
import ru.yandex.direct.utils.FunctionalTestEnvironment
import ru.yandex.direct.utils.emptyCloseableList
import ru.yandex.direct.web.ApiInstanceHolder
import ru.yandex.direct.web.report.ReportClient
import ru.yandex.direct.web.report.response.Report
import java.util.Date
import java.util.UUID

class StatisticsTablePresenterTest {
    @Test
    fun presenterShould_fetchReportFromServer_ifOpenedAndHasNoCachedReport() {
        Environment().apply {
            reportClient.stub {
                on { getReport(any()) } doReturn Report.fromString(ApiSampleData.reportOverall)
            }
            fullReportDao.stub {
                on { getLatestReport(any()) } doReturn null as ReportDatabaseEntry?
            }

            presenter.attachView(view, null)
            presenter.setArguments(StatisticsTablePresenter.Arguments(reportTarget))
            presenter.onResume()
            scheduler.triggerActions()

            verify(fullReportDao).getLatestReport(reportKey)
            verify(reportClient).getReport(any())
            verify(fullReportDao).bindReportWithKey(eq(reportKey), any())
            verify(localRepo).insert(any())

            val captor = ArgumentCaptor.forClass(FullReport::class.java)
            verify(view).showReport(captor.capture())
            assertThat(captor.value.data).isSameAs(reportData)
        }
    }

    @Test
    fun presenterShould_showCachedReport_ifHasCachedReport() {
        Environment().apply {
            val report = FullReport(UUID.randomUUID(), reportSettings, Date(), reportCurrency)
            fullReportDao.stub {
                on { getLatestReport(any()) } doReturn ReportDatabaseEntry.create("", report)
            }

            presenter.attachView(view, null)
            presenter.setArguments(StatisticsTablePresenter.Arguments(reportTarget))
            presenter.onResume()
            scheduler.triggerActions()

            verify(fullReportDao).getLatestReport(reportKey)
            verify(reportClient, never()).getReport(any())
            verify(localRepo, never()).insert(any())
            verify(localRepo).selectForFullReport(report.uuid, sortOrder, reportTarget.kind)

            val captor = ArgumentCaptor.forClass(FullReport::class.java)
            verify(view).showReport(captor.capture())
            assertThat(captor.value.data).isSameAs(reportData)
        }
    }

    @Test
    fun presenterShould_showFetchReportFromServer_ifCachedReportIsTooOld() {
        Environment().apply {
            val report = FullReport(UUID.randomUUID(), reportSettings, Date(0), reportCurrency)
            reportClient.stub {
                on { getReport(any()) } doReturn Report.fromString(ApiSampleData.reportOverall)
            }
            fullReportDao.stub {
                on { getLatestReport(any()) } doReturn ReportDatabaseEntry.create("", report)
            }

            presenter.attachView(view, null)
            presenter.setArguments(StatisticsTablePresenter.Arguments(reportTarget))
            presenter.onResume()
            scheduler.triggerActions()

            verify(fullReportDao).getLatestReport(reportKey)
            verify(localRepo).selectForFullReport(report.uuid, sortOrder, reportTarget.kind)
            verify(reportClient).getReport(any())
            verify(fullReportDao).bindReportWithKey(eq(reportKey), any())
            verify(localRepo).insert(any())

            val captor = ArgumentCaptor.forClass(FullReport::class.java)
            verify(view, times(2)).showReport(captor.capture())
            assertThat(captor.value.data).isSameAs(reportData)
        }
    }

    @Test
    fun presenterShould_showFetchReportFromServer_ifCachedReportHasOutdatedSettings() {
        Environment().apply {
            val outdatedSettings = reportSettings.newBuilder().setSection(Section.LOCATION_OF_PRESENCE).build()
            val report = FullReport(UUID.randomUUID(), outdatedSettings, Date(0), reportCurrency)
            reportClient.stub {
                on { getReport(any()) } doReturn Report.fromString(ApiSampleData.reportOverall)
            }
            fullReportDao.stub {
                on { getLatestReport(any()) } doReturn ReportDatabaseEntry.create("", report)
            }

            presenter.attachView(view, null)
            presenter.setArguments(StatisticsTablePresenter.Arguments(reportTarget))
            presenter.onResume()
            scheduler.triggerActions()

            verify(fullReportDao).getLatestReport(reportKey)
            verify(localRepo).selectForFullReport(report.uuid, sortOrder, reportTarget.kind)
            verify(reportClient).getReport(any())
            verify(fullReportDao).bindReportWithKey(eq(reportKey), any())
            verify(localRepo).insert(any())

            val captor = ArgumentCaptor.forClass(FullReport::class.java)
            verify(view, times(2)).showReport(captor.capture())
            assertThat(captor.value.data).isSameAs(reportData)
        }
    }

    @Test
    fun presenterShould_showFetchReportFromServer_onSwipeRefresh() {
        Environment().apply {
            reportClient.stub {
                on { getReport(any()) } doReturn Report.fromString(ApiSampleData.reportOverall)
            }
            val swipes = PublishSubject.create<Any>()
            view.stub {
                on { refreshSwipes } doReturn swipes
            }

            presenter.attachView(view, null)
            presenter.setArguments(StatisticsTablePresenter.Arguments(reportTarget))
            presenter.onResume()
            scheduler.triggerActions()
            swipes.onNext(Any())
            scheduler.triggerActions()

            verify(reportClient, times(2)).getReport(any())
            verify(view, times(2)).showReport(any())
        }
    }

    class Environment : FunctionalTestEnvironment() {
        val reportCurrency = ApiSampleData.currency[0]

        val section = Section.MOBILE_PLATFORM

        val grouping = Grouping.SELECTED_RANGE

        val sortOrder = StatisticsSortOrder.bySectionCriteria(section, false)

        val metrics = listOf(Metrics.IMPRESSIONS, Metrics.CLICKS, Metrics.CTR)

        val dateRange = DateRange.DEFAULT

        val reportTarget = ReportTargetInfo.overall()

        val reportData = FullReportData(sortOrder, emptyCloseableList())

        val includeVat = true

        val fragmentSettings = StatisticsLocalSettings.getDefault(ReportTarget.OVERALL).apply {
            section = this@Environment.section
            grouping = this@Environment.grouping
            sortOrder = this@Environment.sortOrder
        }

        val reportSettings = FullReportSettings.Builder()
                .setReportTargetInfo(reportTarget)
                .setIncludeVat(includeVat)
                .setDateRange(dateRange)
                .setGrouping(grouping)
                .setMetrics(metrics)
                .setSection(section)
                .build()

        val reportKey = ReportKey.forTarget(reportTarget).toString()

        val fullReportDao = mock<ReportDao>()

        val reportClient = mock<ReportClient>()

        val localRepo = mock<StatisticsLocalRepository> {
            on { selectForFullReport(any(), any(), any()) } doReturn reportData
        }

        val clientInteractor = mock<CurrentClientInteractor> {
            on { currency } doReturn reportCurrency
            on { currentClientInfo } doReturn Maybe.just(ApiSampleData.clientInfo)
        }

        val remoteRepo = StatisticsRemoteRepository(ApiInstanceHolder.just(reportClient), scheduler, mock())

        val fullReportRepo = ReportLocalRepository(fullReportDao)

        val settingsInteractor = StatisticsSettingsInteractor(configuration, scheduler)

        val interactor = StatisticsInteractor(mock(), localRepo, remoteRepo, fullReportRepo,
                scheduler, scheduler, scheduler, clientInteractor, settingsInteractor)

        val presenter = StatisticsTablePresenter(
            defaultErrorResolution,
            scheduler,
            interactor,
            settingsInteractor,
            passportInteractor,
            mock(),
            mock(),
            mock(),
            mock(),
            mock()
        )

        val view = mock<StatisticsTableView>().stubViewMethods().stub {
            on { refreshSwipes } doReturn Observable.never()
        }

        init {
            configuration.stub {
                on { statisticsDateRange } doReturn dateRange
                on { getStatisticsSettings(any()) } doReturn fragmentSettings
                on { enabledMetrics } doReturn metrics
                on { isReportVatEnabled } doReturn includeVat
            }
        }
    }

    companion object {
        @JvmStatic
        @BeforeClass
        fun runBeforeAllTests() {
            CurrencyInitializer.injectTestDataInStaticFields()
        }
    }
}