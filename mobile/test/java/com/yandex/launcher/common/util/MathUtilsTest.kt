package com.yandex.launcher.common.util

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.yandex.launcher.BaseRobolectricTest
import org.junit.Test

class MathUtilsTest: BaseRobolectricTest() {

    @Test
    fun `numbers are equal`() {
        assertThat(MathUtils.isEqual(0.003f, 0.003f), equalTo(true))
        assertThat(MathUtils.isEqual(0.03f, 0.03f), equalTo(true))
        assertThat(MathUtils.isEqual(0.3f, 0.3f), equalTo(true))
        assertThat(MathUtils.isEqual(1.003f, 1.003f), equalTo(true))
        assertThat(MathUtils.isEqual(1.03f, 1.03f), equalTo(true))
        assertThat(MathUtils.isEqual(1.3f, 1.3f), equalTo(true))
    }

    @Test
    fun `numbers are not equal`() {
        assertThat(MathUtils.isEqual(0.003f, 0.002f), equalTo(false))
        assertThat(MathUtils.isEqual(0.03f, 0.02f), equalTo(false))
        assertThat(MathUtils.isEqual(0.3f, 0.2f), equalTo(false))
        assertThat(MathUtils.isEqual(1.003f, 1.002f), equalTo(false))
        assertThat(MathUtils.isEqual(1.03f, 1.02f), equalTo(false))
        assertThat(MathUtils.isEqual(1.3f, 1.2f), equalTo(false))

        assertThat(MathUtils.isEqual(0.003f, 0.002999f), equalTo(false))
        assertThat(MathUtils.isEqual(0.03f, 0.02999f), equalTo(false))
        assertThat(MathUtils.isEqual(0.3f, 0.2999f), equalTo(false))
        assertThat(MathUtils.isEqual(1.003f, 1.002999f), equalTo(false))
        assertThat(MathUtils.isEqual(1.03f, 1.02999f), equalTo(false))
        assertThat(MathUtils.isEqual(1.3f, 1.2999f), equalTo(false))
    }

    @Test
    fun `get 50% of 100, 50 returned`() {
        assertThat(MathUtils.relative2Px(50f, 100f), equalTo(50f))
    }

    @Test
    fun `get 1% of 100, 1 returned`() {
        assertThat(MathUtils.relative2Px(1f, 100f), equalTo(1f))
    }

    @Test
    fun `get 99% of 100, 99 returned`() {
        assertThat(MathUtils.relative2Px(99f, 100f), equalTo(99f))
    }


    @Test
    fun `intersection point of 2 lines is (1, 1)`() {
        val input = arrayOf(
            arrayOf(
                intArrayOf(0, 0),
                intArrayOf(2, 2)
            ),
            arrayOf(
                intArrayOf(2, 0),
                intArrayOf(0, 2)
            )
        )

        val intersectionPoint = MathUtils.getIntersectionPoint(input)

        assertThat(intersectionPoint[0], equalTo(1))
        assertThat(intersectionPoint[1], equalTo(1))
    }

    @Test
    fun `intersection point of 2 lines is (2, 0)`() {
        val input = arrayOf(
            arrayOf(
                intArrayOf(2, 0),
                intArrayOf(2, 2)
            ),
            arrayOf(
                intArrayOf(2, 0),
                intArrayOf(0, 2)
            )
        )

        val intersectionPoint = MathUtils.getIntersectionPoint(input)

        assertThat(intersectionPoint[0], equalTo(2))
        assertThat(intersectionPoint[1], equalTo(0))
    }
}
