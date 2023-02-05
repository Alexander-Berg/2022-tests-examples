package com.yandex.mail.testopithecus.feature.impl

import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.uiautomator.UiDevice
import com.yandex.mail.R
import com.yandex.mail.testopithecus.pages.MessageViewPage
import com.yandex.mail.testopithecus.steps.DEFAULT_TIMEOUT
import com.yandex.mail.testopithecus.steps.SHORT_TIMEOUT
import com.yandex.mail.testopithecus.steps.scrollToObjectIfNeeded
import com.yandex.mail.testopithecus.steps.shouldNotSee
import com.yandex.mail.testopithecus.steps.shouldSee
import com.yandex.xplat.testopithecus.DefaultFolderName
import com.yandex.xplat.testopithecus.FolderName
import com.yandex.xplat.testopithecus.Validation
import java.util.concurrent.TimeUnit

class ValidationImpl(private val device: UiDevice) : Validation {
    private val messageViewPage: MessageViewPage = MessageViewPage()

    override fun validatePossibleActionsAction(folderName: FolderName) {
        messageViewPage.goToActionMenu()
        shouldSee(
            DEFAULT_TIMEOUT,
            TimeUnit.MILLISECONDS,
            ViewMatchers.withText(R.string.reply_action),
            ViewMatchers.withText(R.string.reply_to_all),
            ViewMatchers.withText(R.string.forward),
            ViewMatchers.withText(R.string.delete),
            ViewMatchers.withText(R.string.mark_unread),
            ViewMatchers.withText(R.string.important),
            ViewMatchers.withText(R.string.move_to_folder)
        )
        if (folderName == DefaultFolderName.spam) {
            shouldSee(SHORT_TIMEOUT, TimeUnit.MILLISECONDS, ViewMatchers.withText(R.string.mark_not_spam))
        } else {
            shouldNotSee(ViewMatchers.withText(R.string.mark_as_spam), SHORT_TIMEOUT, TimeUnit.MILLISECONDS)
        }
        scrollToObjectIfNeeded("Show translator")
        shouldSee(
            DEFAULT_TIMEOUT,
            TimeUnit.MILLISECONDS,
            ViewMatchers.withText(R.string.mark_with_label),
            ViewMatchers.withText(R.string.show_translator)
        )
        if (folderName == DefaultFolderName.archive) {
            shouldNotSee(ViewMatchers.withText(R.string.archive), SHORT_TIMEOUT, TimeUnit.MILLISECONDS)
        } else {
            shouldSee(SHORT_TIMEOUT, TimeUnit.MILLISECONDS, ViewMatchers.withText(R.string.archive))
        }
    }
}
