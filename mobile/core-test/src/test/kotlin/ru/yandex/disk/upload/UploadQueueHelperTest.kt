package ru.yandex.disk.upload

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class UploadQueueHelperTest  {

    @Test
    fun `should include all parent`() {
        val parentDirs = UploadQueueHelper.getParentDirs("/disk/A/B/C", false)
        assertThat(parentDirs, contains("/disk/A/B", "/disk/A"))
    }

    @Test
    fun `should include all parent including self`() {
        val parentDirs = UploadQueueHelper.getParentDirs("/disk/A/B/C", true)
        assertThat(parentDirs, contains("/disk/A/B/C", "/disk/A/B", "/disk/A"))
    }

}