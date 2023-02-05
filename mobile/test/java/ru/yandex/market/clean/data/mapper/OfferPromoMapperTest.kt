package ru.yandex.market.clean.data.mapper

import com.annimon.stream.Exceptional
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.base.network.common.address.HttpAddress
import ru.yandex.market.base.network.common.address.HttpAddressParser
import ru.yandex.market.data.money.dto.priceDtoTestInstance
import ru.yandex.market.clean.data.fapi.dto.frontApiDiscountDtoTestInstance
import ru.yandex.market.clean.data.fapi.dto.mergedItemsInfoDtoTestInstance
import ru.yandex.market.clean.data.fapi.dto.mergedOfferPromoDtoTestInstance
import ru.yandex.market.clean.data.model.dto.DiscountTypeDto
import ru.yandex.market.clean.data.model.dto.capiOfferPromoDtoTestInstance
import ru.yandex.market.clean.domain.model.PromoCodeDiscountType
import ru.yandex.market.clean.domain.model.giftOfferTestInstance
import ru.yandex.market.common.featureconfigs.provider.FeatureConfigsProvider
import ru.yandex.market.data.cashback.mapper.order.CashbackPromoTagMapper
import ru.yandex.market.domain.money.model.Money
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.clean.data.mapper.money.MoneyMapper
import ru.yandex.market.common.datetimeparser.DateTimeParser
import ru.yandex.market.data.promo.mapper.OfferPromoTypeMapper
import ru.yandex.market.data.money.dto.PriceDto
import java.math.BigDecimal
import java.util.Date
import java.util.concurrent.ThreadLocalRandom

class OfferPromoMapperTest {

    private val moneyMapper = mock<MoneyMapper>()
    private val httpAddressParser = mock<HttpAddressParser>()
    private val dateTimeParser = mock<DateTimeParser>()
    private val featureConfigsProvider = mock<FeatureConfigsProvider>()
    private val resourcesDataStore = mock<ResourcesManager>()
    private val cashbackPromoTagMapper = mock<CashbackPromoTagMapper>()

    private val configuration = OfferPromoTypeMapper.Configuration(
        minimumBundleSize = 2,
        maxBlueSetAdditionalItemsCount = 2,
        promoTypeGifts = "bg",
        promoTypeGiftAdditionalItem = "gbg",
        promoTypeCheapestAsGift = "cg",
        promoTypeFlashSales = "fs",
        promoTypeBlueSet = "bs",
        promoTypeBlueSetAdditionalItem = "bsai",
        promoTypePriceDrop = "pd",
        promoTypeDirectDiscount = "dd",
        promoTypeSecretSale = "ss",
        cashback = "cb",
        promoTypePromoCode = "pc",
        promoTypeSpreadDiscountCount = "ptsdc",
        promoTypeSpreadDiscountReceipt = "ptsdr",
        promoCashbackCollection = "cbc",
        promoPaymentSystemCashbackCollection = "promoPaymentSystemCashbackCollection",
        promoCashbackYaCardCollection = "promoCashbackYaCardCollection",
        promoTypeParentPromo = "promoTypeParentPromo",
    )

    val mapper = OfferPromoMapper(
        httpAddressParser = httpAddressParser,
        configuration = configuration,
        moneyMapper = moneyMapper,
        dateTimeParser = dateTimeParser,
        featureConfigsProvider = featureConfigsProvider,
        resourcesManager = resourcesDataStore,
        cashbackPromoTagMapper = cashbackPromoTagMapper,
    )

    init {
        val address = HttpAddress.builder()
            .scheme("https")
            .host("beru.ru")
            .addPathSegment("pay")
            .addQueryParameter("param", "value")
            .build()
        whenever(moneyMapper.map(any<PriceDto>())).thenReturn(Exceptional.of { Money.createRub(10) })
        whenever(httpAddressParser.parse(any<String>())).thenReturn(address)
    }

    @Test
    fun `Mapping blue-set with too much additional items should return null`() {
        assertNull(
            mapper.map(
                capiOfferPromoDtoTestInstance(type = configuration.promoTypeBlueSet),
                listOf(
                    giftOfferTestInstance(),
                    giftOfferTestInstance(),
                    giftOfferTestInstance()
                ),
                null,
            )
        )
    }

    @Test
    fun `Mapping blue-set with normal additional items count should succeed`() {
        assertNotNull(
            mapper.map(
                capiOfferPromoDtoTestInstance(type = configuration.promoTypeBlueSet),
                listOf(giftOfferTestInstance()),
                null,
            )
        )
        assertNotNull(
            mapper.map(
                capiOfferPromoDtoTestInstance(type = configuration.promoTypeBlueSet),
                listOf(giftOfferTestInstance(), giftOfferTestInstance()),
                null,
            )
        )
    }

    @Test
    fun `Trying to map cheapest-as-gift promo with insufficient bundle size should return null`() {
        assertNull(
            mapper.map(
                capiOfferPromoDtoTestInstance(type = configuration.promoTypeCheapestAsGift, count = 1),
                listOf(giftOfferTestInstance()),
                null,
            )
        )
    }

    @Test
    fun `Discount is taken from itemsInfo discount for promo code`() {
        val inputOfferPromo = mergedOfferPromoDtoTestInstance(
            type = configuration.promoTypePromoCode,
            itemsInfo = mergedItemsInfoDtoTestInstance(
                discountType = DiscountTypeDto.PERCENT,
                discountPromoPrice = frontApiDiscountDtoTestInstance(
                    percent = BigDecimal.valueOf(ThreadLocalRandom.current().nextLong(1, 30)),
                    absolute = priceDtoTestInstance(
                        value = BigDecimal.valueOf(ThreadLocalRandom.current().nextLong(30, 100))
                    ),
                )
            )
        )

        whenever(dateTimeParser.parseGmt(any())).thenReturn(Date(0L))
        whenever(resourcesDataStore.getString(any())).thenReturn("Правила акции")

        val info = mapper.mapInfo(
            listOf(
                inputOfferPromo to emptyList()
            ),
            emptyList(),
            emptyList(),
            null,
            emptyList()
        )
        assertNotNull(info)
        val inputPercentDiscount = inputOfferPromo.itemsInfo?.discountPromoPrice?.percent
        assertNotNull(inputPercentDiscount)
        assertEquals(inputPercentDiscount, info.promoCodePromo?.prices?.discountValue)
        assertEquals(PromoCodeDiscountType.PERCENT, info.promoCodePromo?.prices?.discountType)
    }
}
