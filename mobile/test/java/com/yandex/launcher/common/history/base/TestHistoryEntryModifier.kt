package com.yandex.launcher.common.history.base

open class TestHistoryEntryModifier : HistoryEntryModifier<TestHistoryEntry> {
    override fun modify(entry: TestHistoryEntry) {

    }

    override fun setEntryWasCreated(wasCreated: Boolean) { /* not used*/ }
}