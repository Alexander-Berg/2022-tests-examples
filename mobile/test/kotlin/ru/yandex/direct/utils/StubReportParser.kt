// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.utils

import io.reactivex.Observable
import ru.yandex.direct.domain.statistics.ReportRow
import ru.yandex.direct.repository.statistics.ReportParser

class StubReportParser(val data: List<ReportRow>) : ReportParser {
    constructor(data: ReportRow) : this(listOf(data))

    override fun parse(): MutableList<ReportRow> = data.toMutableList()

    override fun parseAsync(): Observable<MutableList<ReportRow>> = Observable.just(parse())

    companion object {
        fun empty() = StubReportParser(emptyList())
    }
}