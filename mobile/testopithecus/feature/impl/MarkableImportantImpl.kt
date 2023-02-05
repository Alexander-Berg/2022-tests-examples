package com.yandex.mail.testopithecus.feature.impl

import androidx.test.uiautomator.UiDevice
import com.yandex.mail.steps.ActionList
import com.yandex.mail.testopithecus.steps.ShortSwipeBaseActionsImpl
import com.yandex.xplat.testopithecus.MarkableImportant

class MarkableImportantImpl(device: UiDevice) : MarkableImportant {

    private val shortSwipeBaseActions = ShortSwipeBaseActionsImpl(device)

    override fun markAsImportant(order: Int) {
        shortSwipeBaseActions.shortSwipeMenuAction(order, ActionList.MARK_AS_IMPORTANT)
    }

    override fun markAsUnimportant(order: Int) {
        shortSwipeBaseActions.shortSwipeMenuAction(order, ActionList.UNMARK_AS_IMPORTANT)
    }
}
