package ru.yandex.market.uikit.helper

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SkeletonsCountHelperTest {

    private val helper = SkeletonsCountHelper(columnsCount = 2, visibleSkeletonRows = 1)

    @Test
    fun `Return specified skeleton rows by default`() {
        assertThat(helper.skeletonsToShow).isEqualTo(2)
    }

    @Test
    fun `Fills skeletons to full row`() {
        helper += 1
        assertThat(helper.skeletonsToShow).isEqualTo(3)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Throws exceptions when columns count is zero`() {
        SkeletonsCountHelper(columnsCount = 0, visibleSkeletonRows = 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Throws exceptions when visible skeleton rows is zero`() {
        SkeletonsCountHelper(columnsCount = 1, visibleSkeletonRows = 0)
    }
}