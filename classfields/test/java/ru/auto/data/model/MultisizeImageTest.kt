package ru.auto.data.model

import io.qameta.allure.kotlin.junit4.AllureParametrizedRunner
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.core_ui.util.image.MultisizeImage
import kotlin.test.assertEquals

/**
 *
 * @author jagger on 04.07.18.
 */

@RunWith(AllureParametrizedRunner::class)
class MultisizeImageTest(val image: MultisizeImage, val targetSize: Size, val expectUrl: String) {

    @Test
    fun `nearest_image_found`() {
        val uri = image.findNearest(targetSize)
        assertEquals(expectUrl, uri)
    }

    companion object {

        private val defaultImage = createImage(320, 456, 1200)

        private fun createImage(vararg widthList: Int): MultisizeImage {
            val sizes = widthList.map { Size(it, 0) to "$it"}.toMap()
            return MultisizeImage(sizes, false)
        }

        @JvmStatic
        @Parameterized.Parameters
        fun data() : Collection<Array<Any>> {
            return listOf(
                    arrayOf<Any>(defaultImage, Size(500, 0), "456"),
                    arrayOf<Any>(defaultImage, Size(400, 0), "456"),
                    arrayOf<Any>(defaultImage, Size(350, 0), "320"),
                    arrayOf<Any>(defaultImage, Size(1000, 0), "1200")
            )
        }
    }
}
