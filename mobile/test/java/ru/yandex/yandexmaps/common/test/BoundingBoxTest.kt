package ru.yandex.yandexmaps.common.test

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.yandexmaps.multiplatform.core.geometry.BoundingBox
import ru.yandex.yandexmaps.multiplatform.core.geometry.Point
import ru.yandex.yandexmaps.multiplatform.core.geometry.getCenter

@RunWith(Parameterized::class)
class BoundingBoxTest(private val boundingBox: BoundingBox, private val center: Point) {

    @Test
    fun centerPointTest() {
        val centerResult = boundingBox.getCenter()
        assertThat(centerResult).isEqualToComparingFieldByField(center)
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "Center for BoundingBox: {0} is {1}")
        fun parameters() =
            listOf(
                arrayOf(BoundingBox(30.0, 30.0, 40.0, 40.0), Point(35.0, 35.0)),
                arrayOf(BoundingBox(-90.0, 30.0, -60.0, 40.0), Point(-75.0, 35.0)),
                arrayOf(BoundingBox(-60.0, 90.0, 60.0, -90.0), Point(0.0, 180.0)),
                arrayOf(BoundingBox(0.0, 10.0, 50.0, 0.0), Point(25.0, -175.0))
            )
    }
}
