// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.interactor.statistics

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.ArgumentCaptor
import ru.yandex.direct.loaders.impl.statistic.ReportTargetInfo
import java.util.*

class SummaryReportInteractorTest {
    @Test
    fun fetchSummaryReportForced_usesDifferentUuids() {
        SummaryReportEnvironment().apply {
            interactor.fetchSummaryReportForced(ReportTargetInfo.overall()).subscribe()
            scheduler.triggerActions()

            val fetchCount = 3;
            val captor = ArgumentCaptor.forClass(UUID::class.java)
            verify(remoteRepo, times(fetchCount)).fetch(captor.capture(), any())
            assertThat(captor.allValues.toSet().size).isEqualTo(fetchCount)
        }
    }
}