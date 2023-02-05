package com.yandex.launcher.common.history.base

import android.content.Context
import java.util.ArrayList

open class TestHistoryImplementation(
    context: Context,
    maxNumberOfObjects: Int
) : History<TestHistoryEntry, TestHistoryEntryModifier>(context, "test-history", maxNumberOfObjects) {
    override fun load(callback: LoadCallback?) {

    }

    override fun onAppInstalled(key: String) {
        // not used
    }
    
    override fun onAppInstalled(keys: List<String>) {
        // not used
    }

    override fun onAppRemoved(packageName: String) {
        // not used
    }

    override fun flush() {
        // not used
    }

    override fun dump(tag: String) {
        // not used
    }

    override fun launch(key: String?,modifier: TestHistoryEntryModifier?,callback: LoadCallback?) {
        // not used
    }

    override fun getLast(max: Int): ArrayList<String> = throw UnsupportedOperationException("Not implemented")

    override fun load() {
        // not used
    }

    override fun onBeforeAdd(entry: TestHistoryEntry?) {
        // not used
    }

    override fun onAfterAdd(entry: TestHistoryEntry) {
        // not used
    }

}