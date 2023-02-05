package com.yandex.launcher.common.history.base

open class TestHistoryEntry(override val rating: Double, override val key: String) : HistoryEntry {
    override fun getDumpString(tag: String): String {
        throw UnsupportedOperationException("Not implemented")
    }
}