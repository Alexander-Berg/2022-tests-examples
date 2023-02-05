// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.domain.statistics

import java.util.*

open class BaseReportRowTest {
    internal val mTimestamp = Date()

    private val mReportUuid = UUID.randomUUID()

    private fun row() = ReportRow(mReportUuid)

    internal fun row(timestamp: Date, section: String?, sectionExtra: String?, sectionId: Long): ReportRow {
        val row = row()
        row.timestamp = timestamp
        row.sectionCriteria = section
        row.sectionCriteriaExtra = sectionExtra
        row.sectionCriteriaId = sectionId
        return row
    }
}