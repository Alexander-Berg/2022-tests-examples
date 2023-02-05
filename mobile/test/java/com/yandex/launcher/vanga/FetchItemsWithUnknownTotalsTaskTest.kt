package com.yandex.launcher.vanga

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isEmpty
import com.yandex.launcher.BaseRobolectricTest
import org.junit.Test
import java.lang.ref.WeakReference


class FetchItemsWithUnknownTotalsTaskTest : BaseRobolectricTest() {

    val task = FetchItemsWithUnknownTotalsTask(WeakReference<UpdateVangaRatingJob.Worker>(null))

    @Test
    fun `get partial update params, result package name equals to key's package`() {
        val packagesParam = task.getPartialUpdateParams(listOf("{mypack.com/com.my_act}")).packagesParam
        assertThat(packagesParam.first(), equalTo("mypack.com/com.my_act"))
    }

    @Test
    fun `get partial update params, add to keys 3 items with duplicated package names, result package params, has length 3`() {
        val params = task.getPartialUpdateParams(
            listOf("{mypack.com/com.my_act}", "{mypack.com/com.my_act1}", "{mypack.com/com.my_act2}")
        )
        assertThat(params.requiredKeys.size, equalTo(3))
    }

    @Test
    fun `get partial update params, add to list 2 invalid and 3 valid keys, result has same keys`() {

        val keys = listOf(
            "{validA/myactivity}",
            "invalidA",
            "{validB/myactivity}",
            "{validC/myactivity}",
            "invalidB"
        )

        val keysAndPackageNames = task.getPartialUpdateParams(keys)

        assertThat(keysAndPackageNames.requiredKeys.sorted(), equalTo(keys.sorted()))
    }

    @Test
    fun `get partial update params, add to keys 2 invalid and 3 valid keys, result has 3 proper package params`() {

        val keys = listOf(
            "{validA/myactivity}",
            "invalidA",
            "{validB/myactivity}",
            "{validC/myactivity}",
            "invalidB"
        )

        val params = task.getPartialUpdateParams(keys)

        assertThat(
            params.packagesParam.sorted(),
            equalTo(listOf("validA/myactivity", "validB/myactivity", "validC/myactivity").sorted())
        )
    }

    @Test
    fun `get partial update params, add to keys 6 invalid keys, result package params is empty`() {
        val keys = listOf(
            "",
            "{}",
            "{/}",
            "mypack.com/com.my_act}",
            "{mypack.com\\com.my_act}",
            "{mypack.com\\\\}"
        )

        assertThat(task.getPartialUpdateParams(keys).packagesParam, isEmpty)
    }

    @Test
    fun `get package param, produce correct format`() {
        val param = task.getPackageParam("{mypack.com/com.my_act}")
        assertThat(param, equalTo("mypack.com/com.my_act"))
    }

    @Test
    fun `get package param, return null on invalid key`() {
        val param = task.getPackageParam("{mypack.comcom.my_act}")
        assertThat(param, absent())
    }
}