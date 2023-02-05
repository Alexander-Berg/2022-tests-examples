package ru.yandex.market.clean.presentation.feature.cms.item.gallery.singleaction.snippet.plusbenefits

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.beru.android.R
import ru.yandex.market.clean.domain.model.cms.CmsPlusBenefitsItem
import ru.yandex.market.clean.presentation.feature.cms.model.PlusBenefitsWidgetCmsVo
import ru.yandex.market.common.android.ResourcesManager
import java.math.BigDecimal

@RunWith(Parameterized::class)
class CmsPlusBenefitsFormatterTest(
    private val input: CmsPlusBenefitsItem,
    private val expectedOutput: PlusBenefitsWidgetCmsVo
) {
    private val resourcesDataStore = mock<ResourcesManager> {
        on { getString(R.string.cms_widget_plus_benefits_with_plus_subtitle) } doReturn SUB_TITLE_FOR_YA_PLUS
        on { getString(R.string.cms_widget_plus_benefits_without_plus_subtitle) } doReturn SUB_TITLE_FOR_NOT_YA_PLUS
        on { getString(R.string.cms_widget_plus_benefits_with_plus_button) } doReturn BUTTON_FOR_YA_PLUS
        on { getString(R.string.cms_widget_plus_benefits_without_plus_button) } doReturn BUTTON_FOR_NOT_YA_PLUS
        on {
            getQuantityString(
                R.plurals.cms_widget_plus_benefits_title,
                input.cashbackBalance.toInt()
            )
        } doReturn BALANCE_FORMATTED_STRING + input.cashbackBalance.toString()
    }
    private val formatter = CmsPlusBenefitsFormatter(resourcesDataStore)

    @Test
    fun format() {
        val formatted = formatter.format(input)
        assertThat(expectedOutput).isEqualTo(formatted)
    }

    companion object {

        private const val SUB_TITLE_FOR_YA_PLUS = "заголоввок с плюсом"
        private const val SUB_TITLE_FOR_NOT_YA_PLUS = "заголоввок без плюса"
        private const val BUTTON_FOR_YA_PLUS = "кнопка с плюсом"
        private const val BUTTON_FOR_NOT_YA_PLUS = "кнопка без плюса"
        private const val BALANCE_FORMATTED_STRING = "количество баллов "

        @Parameterized.Parameters(name = "{index}: {0} -> {1}")
        @JvmStatic
        fun data(): Iterable<Array<*>> = listOf(
            arrayOf(
                CmsPlusBenefitsItem(
                    hasYandexPlus = true,
                    BigDecimal.TEN
                ),
                PlusBenefitsWidgetCmsVo(
                    title = BALANCE_FORMATTED_STRING + "10",
                    subtitle = SUB_TITLE_FOR_YA_PLUS,
                    buttonText = BUTTON_FOR_YA_PLUS
                )
            ),
            arrayOf(
                CmsPlusBenefitsItem(
                    hasYandexPlus = false,
                    BigDecimal.ZERO
                ),
                PlusBenefitsWidgetCmsVo(
                    title = BALANCE_FORMATTED_STRING + "0",
                    subtitle = SUB_TITLE_FOR_NOT_YA_PLUS,
                    buttonText = BUTTON_FOR_NOT_YA_PLUS
                )
            ),
        )
    }
}