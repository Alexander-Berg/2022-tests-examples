// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.interactor.statistics

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import io.reactivex.schedulers.TestScheduler
import ru.yandex.direct.Configuration
import ru.yandex.direct.data.ApiSampleData
import ru.yandex.direct.domain.daterange.DateRange
import ru.yandex.direct.domain.statistics.Metrics
import ru.yandex.direct.interactor.clients.CurrentClientInteractor
import ru.yandex.direct.repository.statistics.StatisticsRemoteRepository
import ru.yandex.direct.utils.StubReportParser

class SummaryReportEnvironment {
    val scheduler = TestScheduler()

    val remoteRepo = mock<StatisticsRemoteRepository> {
        on { fetch(any(), any()) } doReturn StubReportParser.empty()
    }

    var configuration = mock<Configuration> {
        on { statisticsDateRange } doReturn DateRange.DEFAULT
        on { isReportVatEnabled } doReturn true
        on { enabledMetrics } doReturn listOf(Metrics.IMPRESSIONS, Metrics.CLICKS)
    }

    var clientInteractor = mock<CurrentClientInteractor> {
        on { currency } doReturn ApiSampleData.currency[0]
    }

    var interactor = StatisticsInteractor(mock(), mock(), remoteRepo, mock(), scheduler,
            scheduler, scheduler, clientInteractor, StatisticsSettingsInteractor(configuration, scheduler))
}