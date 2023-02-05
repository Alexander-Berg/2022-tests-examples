package com.yandex.launcher.common.util

import com.yandex.launcher.BaseRobolectricTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.Assert
import org.junit.Test

private const val MAIN_JOB_ID = 100
private const val FALLBACK_JOB_ID = 500

class JobUtilsTest : BaseRobolectricTest() {

    @Test
    fun `getAllowedJobId returns the main id, if current id is incorrect`() =
        assertThat(JobUtils.getAllowedJobId(MAIN_JOB_ID, FALLBACK_JOB_ID, -1), `is`(MAIN_JOB_ID))

    @Test
    fun `getAllowedJobId returns the main id, if current id doesn't match main id`() =
        assertThat(JobUtils.getAllowedJobId(MAIN_JOB_ID, FALLBACK_JOB_ID, 99), `is`(MAIN_JOB_ID))

    @Test
    fun `getAllowedJobId returns the main id, if current id matches fallback id`() =
        assertThat(JobUtils.getAllowedJobId(MAIN_JOB_ID, FALLBACK_JOB_ID, FALLBACK_JOB_ID), `is`(MAIN_JOB_ID))

    @Test
    fun `getAllowedJobId returns the fallback id, if current id matches main id`() =
        assertThat(JobUtils.getAllowedJobId(MAIN_JOB_ID, FALLBACK_JOB_ID, MAIN_JOB_ID), `is`(FALLBACK_JOB_ID))
}
