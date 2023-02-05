package ru.yandex.market.clean.data.source.cms

import androidx.annotation.StringRes
import org.junit.Assert
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.beru.android.R
import ru.yandex.market.clean.domain.model.cms.CmsFont
import ru.yandex.market.clean.domain.model.cms.CmsWidgetSubtitle
import ru.yandex.market.clean.domain.model.cms.CmsWidgetTitle
import ru.yandex.market.clean.domain.model.cms.SoftUpdateMergedWidgetCmsItem
import ru.yandex.market.clean.domain.model.cms.garson.MergedWidgetParams
import ru.yandex.market.clean.domain.model.cms.garson.SoftUpdateGarson
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.feature.updater.InstallStatus

@RunWith(Enclosed::class)
class CmsSoftUpdateMergedWidgetRepositoryTest {

    class TextTextsFromGarson {

        private val garson = SoftUpdateGarson(
            BUTTON_TITLE,
            IMAGE_URL
        )
        private val mergedWidgetTitleSubtitle = MergedWidgetParams(
            CmsWidgetTitle.testBuilder().name(ITEM_TITLE).build(),
            CmsWidgetSubtitle(ITEM_SUBTITLE, CmsFont.normalCmsFont(), null),
            false
        )
        private val installStatus = InstallStatus.Unknown

        private val repository = CmsSoftUpdateMergedWidgetRepository(mock())

        @Test
        fun `get texts from garsons`() {
            val item = repository.getSoftUpdateItem(
                installStatus = installStatus,
                garson = garson,
                mergedWidgetParams = mergedWidgetTitleSubtitle
            )
            Assert.assertEquals(
                SoftUpdateMergedWidgetCmsItem(
                    title = ITEM_TITLE,
                    subtitle = ITEM_SUBTITLE,
                    buttonTitle = BUTTON_TITLE,
                    imageUrl = IMAGE_URL
                ), item
            )
        }

        companion object {

            private const val ITEM_TITLE = "item title"
            private const val ITEM_SUBTITLE = "item subtitle"
            private const val BUTTON_TITLE = "button title"
            private const val IMAGE_URL = "image url"
        }
    }

    @RunWith(Parameterized::class)
    class InstallButtonTextFromStatus(
        private val installStatus: InstallStatus,
        @StringRes private val expectedButtonTextRes: Int
    ) {

        private val resourcesDataStore = mock<ResourcesManager> {
            on { getString(R.string.item_soft_update_downloaded) } doReturn R.string.item_soft_update_downloaded.toString()
            on { getString(R.string.item_soft_update_downloading) } doReturn R.string.item_soft_update_downloading.toString()
            on { getString(R.string.item_soft_update_failed) } doReturn R.string.item_soft_update_failed.toString()
            on { getString(R.string.item_soft_update_installed) } doReturn R.string.item_soft_update_installed.toString()
            on { getString(R.string.item_soft_update_installing) } doReturn R.string.item_soft_update_installing.toString()
            on { getString(R.string.item_soft_update_pending) } doReturn R.string.item_soft_update_pending.toString()
        }
        private val repository = CmsSoftUpdateMergedWidgetRepository(resourcesDataStore)

        @Test
        fun `get button text from resource by app install status`() {
            val item = repository.getSoftUpdateItem(
                installStatus = installStatus,
                garson = SoftUpdateGarson("", ""),
                mergedWidgetParams = null
            )
            Assert.assertEquals(
                SoftUpdateMergedWidgetCmsItem(
                    title = null,
                    subtitle = null,
                    buttonTitle = expectedButtonTextRes.toString(),
                    imageUrl = ""
                ),
                item
            )
        }

        companion object {

            @Parameterized.Parameters(name = "{index}: {0} -> {1}")
            @JvmStatic
            fun data(): Iterable<Array<*>> = listOf(
                arrayOf(
                    InstallStatus.Downloaded,
                    R.string.item_soft_update_downloaded
                ),
                arrayOf(
                    InstallStatus.Downloading,
                    R.string.item_soft_update_downloading
                ),
                arrayOf(
                    InstallStatus.Failed("empty message"),
                    R.string.item_soft_update_failed
                ),
                arrayOf(
                    InstallStatus.Installed,
                    R.string.item_soft_update_installed
                ),
                arrayOf(
                    InstallStatus.Installing,
                    R.string.item_soft_update_installing
                ),
                arrayOf(
                    InstallStatus.Pending,
                    R.string.item_soft_update_pending
                ),
            )
        }
    }

}