package ru.yandex.market.clean.presentation.feature.cms.item.gallery.singleaction.snippet.softupdate

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.beru.android.R
import ru.yandex.market.clean.domain.model.cms.SoftUpdateMergedWidgetCmsItem
import ru.yandex.market.clean.presentation.feature.cms.model.SoftUpdateMergedWidgetCmsVo
import ru.yandex.market.common.android.ResourcesManager

@RunWith(Parameterized::class)
class CmsSoftUpdateFormatterTest(
    private val input: SoftUpdateMergedWidgetCmsItem,
    private val expectedOutput: SoftUpdateMergedWidgetCmsVo
) {

    private val resourcesDataStore = mock<ResourcesManager> {
        on { getString(R.string.item_soft_update_update_app) } doReturn R.string.item_soft_update_update_app.toString()
        on { getString(R.string.item_soft_update_give_ability) } doReturn R.string.item_soft_update_give_ability.toString()
        on { getString(R.string.item_soft_update_pending) } doReturn R.string.item_soft_update_pending.toString()
    }

    private val formatter = CmsSoftUpdateFormatter(resourcesDataStore)

    @Test
    fun format() {
        val formatted = formatter.format(input)
        MatcherAssert.assertThat(expectedOutput, equalTo(formatted))
    }

    companion object {

        @Parameterized.Parameters(name = "{index}: {0} -> {1}")
        @JvmStatic
        fun data(): Iterable<Array<*>> = listOf(
            arrayOf(
                SoftUpdateMergedWidgetCmsItem(
                    title = "title",
                    subtitle = "subtitle",
                    buttonTitle = "buttonTitle",
                    imageUrl = "imageUrl",
                ),
                SoftUpdateMergedWidgetCmsVo(
                    title = "title",
                    subtitle = "subtitle",
                    buttonTitle = "buttonTitle",
                    imageUrl = "imageUrl"
                )
            ),
            arrayOf(
                SoftUpdateMergedWidgetCmsItem(
                    title = "",
                    subtitle = "",
                    buttonTitle = "",
                    imageUrl = "imageUrl",
                ),
                SoftUpdateMergedWidgetCmsVo(
                    title = R.string.item_soft_update_update_app.toString(),
                    subtitle = R.string.item_soft_update_give_ability.toString(),
                    buttonTitle = R.string.item_soft_update_pending.toString(),
                    imageUrl = "imageUrl"
                )
            ),
            arrayOf(
                SoftUpdateMergedWidgetCmsItem(
                    title = null,
                    subtitle = null,
                    buttonTitle = "",
                    imageUrl = "imageUrl",
                ),
                SoftUpdateMergedWidgetCmsVo(
                    title = R.string.item_soft_update_update_app.toString(),
                    subtitle = R.string.item_soft_update_give_ability.toString(),
                    buttonTitle = R.string.item_soft_update_pending.toString(),
                    imageUrl = "imageUrl"
                )
            ),
        )
    }
}