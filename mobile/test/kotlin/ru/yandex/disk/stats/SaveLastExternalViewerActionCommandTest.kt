package ru.yandex.disk.stats

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import com.google.common.collect.Lists.newArrayList
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.hamcrest.core.IsEqual.equalTo
import org.junit.Test
import org.robolectric.annotation.Config
import ru.yandex.disk.storage.MockSharedPreferences
import ru.yandex.disk.test.AndroidTestCase2

private const val ANDROID_COMPONENT_REPORT = "Resolved possible external open action activities { \nactivity_info\nUnknown\n}"
private const val NOT_ANDROID_COMPONENT_REPORT = "Last external open action resolved to {not_android/cls}"

@Config(manifest = Config.NONE)
class SaveLastExternalViewerActionCommandTest : AndroidTestCase2() {
    private val packageManager = mock<PackageManager> {
        on { queryIntentActivities(any(), any()) } doReturn newArrayList(
                ResolveInfo().apply { activityInfo = ActivityInfo().apply { name = "activity_info" } },
                null
        )
    }
    private val preferences = MockSharedPreferences()
    private val reporter = ExternalViewerActionReporter(preferences)
    private val command = SaveLastExternalViewerActionCommand(packageManager, reporter)

    @Test
    fun `should report android component`() {
        val intent = mock<Intent> {
            on { resolveActivity(any()) } doReturn ComponentName("android", "cls")
        }
        command.execute(SaveLastExternalViewerActionCommandRequest(intent))
        assertThat(preferences.getString("EXTERNAL_VIEWER_ACTION_REPORT", "def_value"),
                equalTo(ANDROID_COMPONENT_REPORT))
    }

    @Test
    fun `should report not android component`() {
        val intent = mock<Intent> {
            on { resolveActivity(any()) } doReturn ComponentName("not_android", "cls")
        }
        command.execute(SaveLastExternalViewerActionCommandRequest(intent))
        assertThat(preferences.getString("EXTERNAL_VIEWER_ACTION_REPORT", "def_value"),
                equalTo(NOT_ANDROID_COMPONENT_REPORT))
    }

    @Test
    fun `should return no info`() {
        val report = reporter.report
        assertThat(report, equalTo("No external open actions was logged"))
    }
}
