package com.yandex.mail.testopithecus.feature.impl

import androidx.test.uiautomator.UiDevice
import com.yandex.mail.testopithecus.steps.LongSwipeBaseActionsImpl
import com.yandex.mail.testopithecus.steps.declineDialog
import com.yandex.mail.testopithecus.steps.isDialogShown
import com.yandex.xplat.testopithecus.LongSwipe

class LongSwipeImpl(private val device: UiDevice) : LongSwipe {
    private val swipeBaseActions = LongSwipeBaseActionsImpl(device)
    private val shortSwipeActions = ShortSwipeMenuImpl(device)

    override fun deleteMessageByLongSwipe(order: Int, confirmDeletionIfNeeded: Boolean) {
        swipeBaseActions.longSwipeLeft(order)
        if (device.isDialogShown()) {
            if (confirmDeletionIfNeeded) {
                shortSwipeActions.acceptAndWaitForUndoToDisappear()
            } else {
                device.declineDialog()
            }
        }
    }

    override fun archiveMessageByLongSwipe(order: Int) {
        swipeBaseActions.longSwipeLeft(order)
    }
}
