package ru.yandex.market.images

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.domain.media.model.avatarsImageReferenceTestInstance
import ru.yandex.market.images.avatars.AvatarsThumbnailRegistry
import ru.yandex.market.data.media.image.avatars.AvatarsUrlFormatter

/**
 * @property thumbnailNamespace - namespace для [ThumbnailAttributes], см [AvatarsThumbnailRegistry]
 * @property thumbnailId - ожидаемый суффикс в [AvatarsReferenceGlideLoaderDelegate.getUrl]
 * @property imageViewWidth - ширина ImageView в которую Glider будет загружать картинку
 * @property imageViewHeight - высота ImageView в которую Glider будет загружать картинку
 * @property densityFactors - интервал значении для densityFactor, при котором
 *  [AvatarsReferenceGlideLoaderDelegate.getUrl] будет содержать суффикс thumbnailId
 *  при заданных размерах imageViewWidth x imageViewHeight
 *
 *  Подсчет интервалов для входных данных производится вручную на бумажке для вьюхи размером 500x500.
 *  К сожалению пока удалось подсчитать интервалы только к половине thumbnail, а именно в основным к квадратным,
 *  т.к. подсчет интервалов для прямоугольных thumbnail не является тривиальным
 */
@RunWith(Parameterized::class)
class AvatarsReferenceGlideLoaderDelegateTest(
    private val thumbnailNamespace: String,
    private val thumbnailId: String,
    private val imageViewWidth: Int,
    private val imageViewHeight: Int,
    private val densityFactors: List<Double>,
) {
    private val avatarsThumbnailRegistry = AvatarsThumbnailRegistry()
    private val densityFactorCalculator = mock<DensityFactorCalculator>()

    private val avatarsUrlFormatter = AvatarsUrlFormatter()

    @Test
    fun `Test url formatting for avatars image references`() {
        val avatarsImageReference = avatarsImageReferenceTestInstance(namespace = thumbnailNamespace, isTrimmed = false)
        densityFactors.forEach { densityFactor ->
            whenever(densityFactorCalculator.getDensityFactor()).thenReturn(densityFactor)
            val delegate = AvatarsReferenceGlideLoaderDelegate(
                avatarsThumbnailRegistry = avatarsThumbnailRegistry,
                avatarsUrlFormatter = avatarsUrlFormatter,
                densityFactorCalculator = densityFactorCalculator,
                enableLog = false,
            )
            val url = delegate.getUrl(
                model = avatarsImageReference,
                width = imageViewWidth,
                height = imageViewHeight
            )
            assertThat(url).endsWith(thumbnailId)
        }
    }

    private companion object {

        const val VIEW_SIZE = 500

        @Parameterized.Parameters
        @JvmStatic
        fun data(): Iterable<Array<*>> = listOf(
            //mpic namespace

            //0
            arrayOf("mpic", "1hq", VIEW_SIZE, VIEW_SIZE, range(10.0, 15.0)),
            //1
            arrayOf("mpic", "2hq", VIEW_SIZE, VIEW_SIZE, range(5.0, 6.66)),
            //2
            arrayOf("mpic", "3hq", VIEW_SIZE, VIEW_SIZE, range(6.67, 10.0)),
            //3
            arrayOf("mpic", "4hq", VIEW_SIZE, VIEW_SIZE, range(3.34, 4.03)),
            //4
            arrayOf("mpic", "5hq", VIEW_SIZE, VIEW_SIZE, range(2.5, 3.33)),
            //5
            arrayOf("mpic", "6hq", VIEW_SIZE, VIEW_SIZE, range(2.0, 2.08)),
            //6
            arrayOf("mpic", "7hq", VIEW_SIZE, VIEW_SIZE, range(4.17, 5.0)),
            //7
            arrayOf("mpic", "8hq", VIEW_SIZE, VIEW_SIZE, range(2.09, 2.5)),
            //8
            arrayOf("mpic", "9hq", VIEW_SIZE, VIEW_SIZE, range(1.0, 1.5)),
            //9
            arrayOf("mpic", "orig", VIEW_SIZE, VIEW_SIZE, range(0.1, 1.0)),
            //10
            arrayOf("mpic", "x124_trim", VIEW_SIZE, VIEW_SIZE, range(4.04, 4.16)),

            //marketpic namespace

            //11
            arrayOf("marketpic", "50x50", VIEW_SIZE, VIEW_SIZE, range(10.0, 15.0)),
            //12
            arrayOf("marketpic", "75x75", VIEW_SIZE, VIEW_SIZE, range(6.7, 8.33)),
            //13
            arrayOf("marketpic", "100x100", VIEW_SIZE, VIEW_SIZE, range(5.0, 6.66)),
            //14
            arrayOf("marketpic", "150x150", VIEW_SIZE, VIEW_SIZE, range(3.34, 4.03)),
            //15
            arrayOf("marketpic", "200x200", VIEW_SIZE, VIEW_SIZE, range(2.5, 3.33)),
            //16
            arrayOf("marketpic", "300x300", VIEW_SIZE, VIEW_SIZE, range(1.8, 2.0)),
            //17
            arrayOf("marketpic", "x332_trim", VIEW_SIZE, VIEW_SIZE, range(1.51, 1.6)),
        )

        fun range(start: Double, end: Double, step: Double = 0.01): List<Double> {
            val list = mutableListOf<Double>()
            var value = start
            while (value < end) {
                list.add(value)
                value += step
            }
            return list
        }
    }
}
