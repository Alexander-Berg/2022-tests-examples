package ru.auto.ara.core

import android.app.Activity
import android.content.Intent
import ru.auto.ara.router.Router
import ru.auto.ara.router.RouterCommand

class TestEmptyActivityCommand(private val activityResult: ActivityResult): RouterCommand {

    data class ActivityResult(
        val requestCode: Int = -1,
        val resultCode: Int = Activity.RESULT_OK,
        val data: Intent? = null
    )

    override fun perform(router: Router, activity: Activity) {
        val intent = TestEmptyActivity.getIntent(activity, activityResult.resultCode, activityResult.data)
        activity.startActivityForResult(intent, activityResult.requestCode)
    }

}
