package com.yandex.vanga.app.operations

import android.app.Activity
import com.yandex.vanga.app.misc.getRatingManagerInternal
import com.yandex.vanga.app.utils.showToast

class TestVisitBatchInsertRunnable(val activity: Activity) : Runnable {
    override fun run() {
        getRatingManagerInternal()
                .testVisitBatchInsert(activity) { exception ->
                    activity.runOnUiThread {
                        activity.showToast(exception?.let { "Test failed due: $it" } ?: "Ok")
                    }
                }
    }
}
