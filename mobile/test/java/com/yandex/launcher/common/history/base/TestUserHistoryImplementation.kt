package com.yandex.launcher.common.history.base

import android.content.Context
import com.yandex.launcher.common.history.UserHistoryEntry
import com.yandex.launcher.common.history.UserHistoryEntryModifier
import com.yandex.launcher.common.util.JsonDiskStorage

class TestUserHistoryImplementation(
    context: Context,
    maxNumberOfObjects: Int,
    maxDays: Int,
    strategy: HistoryStrategy<UserHistoryEntry, UserHistoryEntryModifier>,
    storage: JsonDiskStorage<SavedData>?
) : com.yandex.launcher.common.history.UserHistory(context, "test-history", maxNumberOfObjects, maxDays, strategy, storage) {
    override fun onAppInstalled(keys: List<String>) {
        // not used
    }

    override fun onAppRemoved(packageName: String) {
        // not used
    }

    override fun dump(tag: String) {
        // not used
    }

    override fun calculateRating(entry: UserHistoryEntry) {

    }

    override fun load() {
        // not used
    }

}