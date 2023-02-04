package ru.auto.ara.core.mocks_and_stubbs

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.yandex.updater.lib.NotificationController
import com.yandex.updater.lib.Updater
import com.yandex.updater.lib.UpdaterListener
import com.yandex.updater.lib.UpdaterRequest
import com.yandex.updater.lib.network.UpdateResult
import com.yandex.updater.lib.network.UpdateStatus
import ru.auto.ara.core.utils.waitSomething
import ru.auto.data.util.AUTO_RU_GOOGLE_PLAY_MARKET_URL
import java.util.concurrent.TimeUnit

@Suppress("NotImplementedDeclaration", "ExpressionBodySyntax")
class TestYandexUpdater(
    private val isUpdateAvailable: Boolean,
    private val progress: Int = 0,
    private val uploadingCompleted: Boolean = false
): Updater {
    override fun addUpdateListener(listener: UpdaterListener) {
        if (uploadingCompleted) {
            // emulate loading
            waitSomething(100L, TimeUnit.MILLISECONDS)
            listener.onUpdatesAvailable()
        } else {
            listener.onLoadingProgressChanged(progress)
        }
    }

    override fun cancelPeriodicUpdates() {
        // do nothing
    }

    override fun checkForUpdates(request: UpdaterRequest) {
        // do nothing
    }

    override fun checkForUpdatesSync(appId: String, params: Map<String, String>) {
        // do nothing
    }

    override fun getNotificationController(): NotificationController {
        // do nothing
        throw NotImplementedError()
    }

    override fun hasLoadedArtifact(appId: String?): Boolean {
        // do nothing
        return false
    }

    override fun hasUpdatesAsync(request: UpdaterRequest, receiver: (UpdateResult) -> Unit) {
        // do nothing
    }

    override fun hasUpdatesSync(request: UpdaterRequest): UpdateResult = UpdateResult(
        updateStatus = when {
            isUpdateAvailable -> UpdateStatus.UPDATE_AVAILABLE
            else -> UpdateStatus.NO_UPDATES
        }
    )

    override fun removeUpdateListener(listener: UpdaterListener) {
        // do nothing
    }

    override fun requestInstallUpdate(activity: Activity, requestCode: Int, appId: String?): Boolean {

        // some stub intent
        activity.startActivityForResult(
            Intent(Intent.ACTION_VIEW).setData(Uri.parse(AUTO_RU_GOOGLE_PLAY_MARKET_URL)),
            requestCode
        )
        return true
    }

    override fun schedulePeriodicUpdates(request: UpdaterRequest, periodMs: Long, initialDelayMs: Long) {
        // do nothing
    }

    override fun startNewVersionLoading(targetApkUrl: String, targetApkVersionCode: Long, targetAppId: String?) {
        // do nothing
    }
}
