package com.yandex.launcher.common.history.base

open class TestHistoryStrategy : HistoryStrategy<TestHistoryEntry, TestHistoryEntryModifier> {
    override fun newEntry(key: String): TestHistoryEntry = throw UnsupportedOperationException("Not implemented")
    override fun applyModification(entry: TestHistoryEntry, modifier: TestHistoryEntryModifier) {
    }
}