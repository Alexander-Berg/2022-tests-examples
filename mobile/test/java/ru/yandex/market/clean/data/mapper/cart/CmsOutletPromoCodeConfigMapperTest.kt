package ru.yandex.market.clean.data.mapper.cart

import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.base.network.common.address.HttpAddress
import ru.yandex.market.base.network.common.address.HttpAddressParser
import ru.yandex.market.clean.data.mapper.cms.CmsColorParser
import ru.yandex.market.clean.data.mapper.cms.CmsOutletPromoCodeConfigMapper
import ru.yandex.market.clean.data.model.dto.DiscountTypeDto
import ru.yandex.market.clean.data.model.dto.cms.CmsImageDto
import ru.yandex.market.clean.data.model.dto.cms.CmsPromoCodeBannerConfigDto
import ru.yandex.market.clean.domain.model.cms.CmsOutletPromoCodeConfig
import ru.yandex.market.data.media.image.mapper.ImageReferenceMapper
import ru.yandex.market.domain.media.model.ImageReference

@RunWith(Parameterized::class)
class CmsOutletPromoCodeConfigMapperTest(
    private val input: CmsPromoCodeBannerConfigDto,
    private val output: CmsOutletPromoCodeConfig?,
) {

    private val colorParser: CmsColorParser = mock()
    private val httpAddressParser: HttpAddressParser = mock()
    private val imageReferenceMapper: ImageReferenceMapper = mock()

    init {
        whenever(colorParser.parse(any())).doReturn(DEFAULT_COLOR)
        whenever(httpAddressParser.parse(anyString())).doReturn(DEFAULT_LINK)
        whenever(imageReferenceMapper.mapImage(anyString(), anyBoolean())).doReturn(DEFAULT_IMAGE)
    }

    private val mapper = CmsOutletPromoCodeConfigMapper(
        colorParser = colorParser,
        httpAddressParser = httpAddressParser,
        imageReferenceMapper = imageReferenceMapper,
    )

    @Test
    fun `test mapping`() {
        Assertions.assertThat(mapper.map(input)).isEqualTo(output)
    }

    companion object {

        private const val PROMO_CODE = "promocode"
        private const val DESCRIPTION = "desc"
        private const val LINK = "link"
        private const val DEFAULT_COLOR = 0xFF00FF
        private val DEFAULT_LINK = HttpAddress.empty()
        private val DEFAULT_IMAGE = ImageReference.empty()

        private val DEFAULT_DTO = CmsPromoCodeBannerConfigDto(
            discountValue = 5.toBigDecimal(),
            discountType = DiscountTypeDto.PERCENT,
            backgroundColor = "#ff00ff",
            backgroundImage = null,
            promoCode = PROMO_CODE,
            description = DESCRIPTION,
            linkText = LINK,
            link = "https://pronhub.cc"
        )

        private val DEFAULT_RESULT = CmsOutletPromoCodeConfig(
            discountValue = 5.toBigDecimal(),
            discountType = CmsOutletPromoCodeConfig.DiscountType.PERCENT,
            backgroundColor = DEFAULT_COLOR,
            backgroundImage = null,
            promoCode = PROMO_CODE,
            description = DESCRIPTION,
            linkText = LINK,
            link = DEFAULT_LINK,
        )

        @Parameterized.Parameters(name = "{index}: {0} -> {1}")
        @JvmStatic
        fun data(): Iterable<Array<*>> = listOf(
            arrayOf(
                DEFAULT_DTO, DEFAULT_RESULT
            ),
            arrayOf(
                DEFAULT_DTO.copy(discountValue = null), null
            ),
            arrayOf(
                DEFAULT_DTO.copy(discountType = null), null
            ),
            arrayOf(
                DEFAULT_DTO.copy(promoCode = null), null
            ),
            arrayOf(
                DEFAULT_DTO.copy(discountType = DiscountTypeDto.ABSOLUTE),
                DEFAULT_RESULT.copy(discountType = CmsOutletPromoCodeConfig.DiscountType.ABSOLUTE)
            ),
            arrayOf(
                DEFAULT_DTO.copy(backgroundImage = CmsImageDto.testInstance()),
                DEFAULT_RESULT.copy(backgroundImage = DEFAULT_IMAGE)
            ),
        )
    }
}