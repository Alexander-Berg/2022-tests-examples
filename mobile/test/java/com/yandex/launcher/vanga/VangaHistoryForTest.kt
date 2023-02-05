package com.yandex.launcher.vanga

import android.content.Context
import com.yandex.vanga.InvalidKeysSeparator
import com.yandex.vanga.NO_LIMIT
import com.yandex.vanga.RatingManager

class VangaHistoryForTest(
    context: Context,
    val ratingManagerP: RatingManager,
    vangaHistoryDelegate: VangaHistoryDelegate
) : VangaHistory(context, "awesome vanga test", NO_LIMIT, vangaHistoryDelegate) {

    override fun load() {
        updateCacheLockedInternal(getRatingManager().getSortedEntries(context))
    }

    override fun getRatingManager() = ratingManagerP

    override fun launch(key: String) {
        updateCacheLockedInternal(getRatingManager().updateVisitsAndRating(context, key))
    }

    override fun addToRating(keyList: List<String>) {
        updateCacheLockedInternal(getRatingManager().addInstalledAppToRating(context, keyList, MAX_VANGA_OBJECT_IN_HISTORY))
    }
}

class StubVangaHistoryDelegate : VangaHistoryDelegate {
    override fun getVangaInvalidKeysSeparator(): InvalidKeysSeparator? {
        //do nothing
        return null
    }

    override fun scheduleVangaRatingJob(keys: List<String>) {
        //do nothing
    }
}