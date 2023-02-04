package ru.auto.ara.screenshotTests.garage.tools

import android.app.Activity
import ru.auto.ara.BaseActivity
import ru.auto.ara.core.utils.getCurrentActivity
import ru.auto.ara.router.Router
import ru.auto.ara.router.RouterCommand
import ru.auto.ara.router.command.ShowInlineGarageCardCommand
import ru.auto.core_ui.util.runOnUi

class TestShowInlineGarageCardOnUiCommand(private val garageCardId: String) : RouterCommand {

    fun perform() {
        val activity = getCurrentActivity() as BaseActivity
        val router = activity.router
        runOnUi { perform(router, activity) }
    }

    override fun perform(router: Router, activity: Activity) = ShowInlineGarageCardCommand(garageCardId).perform(router, activity)
}
