package ru.yandex.market.clean.presentation.feature.cms.formatter

import org.junit.Assert
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.clean.domain.model.cms.CmsProduct
import ru.yandex.market.clean.presentation.feature.cms.item.offer.formatter.FinancialProductFormatter
import ru.yandex.market.clean.presentation.formatter.PhotoFormatter
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.domain.media.model.ImageReference

class PhotoFormatterTest {

    private val financialProductFormatter = mock<FinancialProductFormatter>()
    private val resourcesManager = mock<ResourcesManager>() {
        on { getColor(any()) } doReturn 0
        on { getString(any()) } doReturn "РЕСЕЙЛ"
    }
    private val photoFormatter = PhotoFormatter(financialProductFormatter, resourcesManager)

    @Test
    fun `check skuImage used as first image in vo when useSkuImage is set true`() {
        val imageReference = ImageReference.fromUrl(
            IMAGE_URL,
            false
        )
        val cmsProduct = CmsProduct.testBuilder()
            .image(imageReference)
            .build()
        val vo = photoFormatter.format(
            product = cmsProduct,
            garsons = emptyList(),
            isHypeGoodBadgeExpEnabled = false,
            isVisualType = false,
            snippetConfigBackgroundImage = null,
            useSkuImage = true
        )
        Assert.assertEquals(imageReference, vo.photos.first())
    }

    @Test
    fun `check skuImage not used as first image in vo when useSkuImage is set false`() {
        val imageReference = ImageReference.fromUrl(
            IMAGE_URL,
            false
        )
        val cmsProduct = CmsProduct.testBuilder()
            .image(imageReference)
            .build()
        val vo = photoFormatter.format(
            product = cmsProduct,
            garsons = emptyList(),
            isHypeGoodBadgeExpEnabled = false,
            isVisualType = false,
            snippetConfigBackgroundImage = null,
            useSkuImage = false
        )
        Assert.assertNotEquals(imageReference, vo.photos.first())
    }

    companion object {
        private const val IMAGE_URL =
            "https://avatars.mds.yandex.net/get-mpic/5245589/img_id5944042607284309248.jpeg/x248_trim"
    }
}
