package ru.yandex.market.clean.presentation.feature.cms.item.gallery.singleaction.snippet.growingcashback

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.clean.domain.model.cms.CmsGrowingCashbackItem
import ru.yandex.market.clean.domain.model.cms.cmsGrowingCashbackItemTestInstance
import ru.yandex.market.common.android.ResourcesManagerImpl
import ru.yandex.market.domain.media.model.measuredImageReferenceTestInstance

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P], qualifiers = "ru")
class CmsGrowingCashbackFormatterTest(
    private val input: CmsGrowingCashbackItem,
    private val expectedOutput: CmsGrowingCashbackVo
) {

    private val formatter = CmsGrowingCashbackFormatter(
        ResourcesManagerImpl(ApplicationProvider.getApplicationContext<Context>().resources)
    )

    @Test
    fun testFormat() {
        val actual = formatter.format(input)

        assertThat(actual).isEqualTo(expectedOutput)
    }

    companion object {

        @ParameterizedRobolectricTestRunner.Parameters(name = "{index} : {0}")
        @JvmStatic
        fun data(): Iterable<Array<*>> = listOf(
            //0 - Акция активна
            arrayOf(
                cmsGrowingCashbackItemTestInstance(
                    ordersCount = 3,
                    maxReward = 100,
                    icon = measuredImageReferenceTestInstance("icon1"),
                    state = CmsGrowingCashbackItem.State.GET_REWARD
                ),
                cmsGrowingCashbackVoTestInstance(
                    title = "Получите до 100 баллов",
                    subtitle = "за первые 3 заказа в приложении",
                    primaryButton = cmsGrowingCashbackVo_ButtonTestInstance(
                        title = "Подробнее",
                        action = CmsGrowingCashbackVo.Action.ABOUT
                    ),
                    secondaryButton = null,
                    icon = measuredImageReferenceTestInstance("icon1")
                )
            ),
            //1 - Акция активна на 1 заказ
            arrayOf(
                cmsGrowingCashbackItemTestInstance(
                    ordersCount = 1,
                    maxReward = 1000,
                    icon = measuredImageReferenceTestInstance("icon2"),
                    state = CmsGrowingCashbackItem.State.GET_REWARD,
                ),
                cmsGrowingCashbackVoTestInstance(
                    title = "Получите до 1 000 баллов",
                    subtitle = "за 1 заказ в приложении",
                    primaryButton = cmsGrowingCashbackVo_ButtonTestInstance(
                        title = "Подробнее",
                        action = CmsGrowingCashbackVo.Action.ABOUT
                    ),
                    secondaryButton = null,
                    icon = measuredImageReferenceTestInstance("icon2")
                )
            ),
            //2 - Акция активна на 5 заказ
            arrayOf(
                cmsGrowingCashbackItemTestInstance(
                    ordersCount = 5,
                    maxReward = 2000,
                    state = CmsGrowingCashbackItem.State.GET_REWARD
                ),
                cmsGrowingCashbackVoTestInstance(
                    title = "Получите до 2 000 баллов",
                    subtitle = "за первые 5 заказов в приложении",
                    primaryButton = cmsGrowingCashbackVo_ButtonTestInstance(
                        title = "Подробнее",
                        action = CmsGrowingCashbackVo.Action.ABOUT
                    ),
                    secondaryButton = null
                )
            ),
            //3 - Нарушены условия акции
            arrayOf(
                cmsGrowingCashbackItemTestInstance(
                    ordersCount = 3,
                    state = CmsGrowingCashbackItem.State.ACTION_TERMS_VIOLATION
                ),
                cmsGrowingCashbackVoTestInstance(
                    title = "Получите до 42 баллов",
                    subtitle = "за первые 3 заказа в приложении",
                    primaryButton = cmsGrowingCashbackVo_ButtonTestInstance(
                        title = "Подробнее",
                        action = CmsGrowingCashbackVo.Action.ABOUT
                    ),
                    secondaryButton = cmsGrowingCashbackVo_ButtonTestInstance(
                        title = "Закрыть",
                        action = CmsGrowingCashbackVo.Action.CLOSE
                    )
                )
            ),
            //4 - Максимум баллов
            arrayOf(
                cmsGrowingCashbackItemTestInstance(
                    ordersCount = 3,
                    state = CmsGrowingCashbackItem.State.ACTION_COMPLETE
                ),
                cmsGrowingCashbackVoTestInstance(
                    title = "Получите до 42 баллов",
                    subtitle = "за первые 3 заказа в приложении",
                    primaryButton = cmsGrowingCashbackVo_ButtonTestInstance(
                        title = "Подробнее",
                        action = CmsGrowingCashbackVo.Action.ABOUT
                    ),
                    secondaryButton = cmsGrowingCashbackVo_ButtonTestInstance(
                        title = "Закрыть",
                        action = CmsGrowingCashbackVo.Action.CLOSE
                    )
                )
            )
        )
    }
}
