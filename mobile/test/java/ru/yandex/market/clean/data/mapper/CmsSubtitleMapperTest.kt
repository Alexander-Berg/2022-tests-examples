package ru.yandex.market.clean.data.mapper

import android.graphics.Color
import org.junit.Assert
import org.mockito.kotlin.mock
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.data.mapper.cms.CmsSubtitleMapper
import ru.yandex.market.clean.data.mapper.cms.CmsWidgetMapper
import ru.yandex.market.clean.data.mapper.cms.CmsWidgetWrapperPropsMapper
import ru.yandex.market.clean.data.model.dto.cms.CmsNodeWrapperTitle
import ru.yandex.market.clean.data.model.dto.cms.cmsNodePropertyDtoTestInstance
import ru.yandex.market.clean.data.model.dto.cms.cmsNodeWrapperPropsDtoTestInstance
import ru.yandex.market.clean.domain.model.OfferSpecificationInternal
import ru.yandex.market.clean.domain.model.cms.CmsFont
import ru.yandex.market.clean.domain.model.cms.CmsWidgetType
import ru.yandex.market.clean.domain.model.cms.GarsonType
import ru.yandex.market.clean.domain.model.cms.garson.PrimeSearchCmsWidgetGarson
import ru.yandex.market.common.android.ResourcesManager

class CmsSubtitleMapperTest {

    private val paramsMock = mock<PrimeSearchCmsWidgetGarson.Params>()
    private val propertiesMock = mock<OfferSpecificationInternal>()
    private val resourcesManager = mock<ResourcesManager>()
    private val cmsNodeWrapperPropsMapper = mock<CmsWidgetWrapperPropsMapper>()
    private val cmsSubtitleMapper = CmsSubtitleMapper(
        resourcesManager = resourcesManager,
        cmsNodeWrapperPropsMapper = cmsNodeWrapperPropsMapper,
    )

    init {
        whenever(resourcesManager.getFormattedString(any(), any())).doReturn(ACTIVE_SUBSTANCE)
        whenever(resourcesManager.getString(any())).doReturn(SUBSTANCE)
        whenever(resourcesManager.getColor(any())).doReturn(Color.BLACK)
    }

    @Test
    fun `map subtitle from wrapper props is correct`() {
        val subtitle = cmsSubtitleMapper.map(
            cmsNodeWrapperPropsDtoTestInstance(
                title = CmsNodeWrapperTitle.testInstance().copy(
                    subtitle = CmsNodeWrapperTitle.testInstance().copy(text = TITLE)
                )
            )
        )

        Assert.assertEquals(subtitle?.name, TITLE)
    }

    @Test
    fun `map subtitle from empty wrapper props is null`() {
        val subtitle = cmsSubtitleMapper.map(
            cmsNodeWrapperPropsDtoTestInstance(
                title = CmsNodeWrapperTitle.testInstance().copy(
                    subtitle = CmsNodeWrapperTitle.testInstance()
                )
            )
        )

        Assert.assertNull(subtitle)
    }

    @Test
    fun `use default font if none provided`() {
        val subtitle = cmsSubtitleMapper.map(TITLE, null)
        Assert.assertEquals(subtitle?.name, TITLE)
        Assert.assertEquals(subtitle?.font, CmsFont.normalCmsFont())
    }

    @Test
    fun `use active substance for pharma when active substance provided`() {
        whenever(paramsMock.hasVidalParam()).doReturn(true)
        whenever(propertiesMock.isMedicine()).doReturn(true)
        val subtitle = cmsSubtitleMapper.map(
            type = CmsWidgetType.SCROLLBOX,
            properties = cmsNodePropertyDtoTestInstance(),
            garsons = listOf(
                PrimeSearchCmsWidgetGarson(
                    count = 0,
                    vendorId = null,
                    atcCode = null,
                    specificationSet = emptyList(),
                    params = paramsMock,
                    type = GarsonType.PRIME_SEARCH,
                )
            ),
            extraParams = CmsWidgetMapper.ExtraParams(
                activeSubstance = ACTIVE_SUBSTANCE,
                internalOfferProperties = propertiesMock,
            )
        )

        Assert.assertEquals(ACTIVE_SUBSTANCE, subtitle?.name)
    }

    @Test
    fun `use hardcoded string for pharma when no active substance provided`() {
        whenever(paramsMock.hasVidalParam()).doReturn(true)
        whenever(propertiesMock.isMedicine()).doReturn(true)
        val subtitle = cmsSubtitleMapper.map(
            type = CmsWidgetType.SCROLLBOX,
            properties = cmsNodePropertyDtoTestInstance(),
            garsons = listOf(
                PrimeSearchCmsWidgetGarson(
                    count = 0,
                    vendorId = null,
                    atcCode = null,
                    specificationSet = emptyList(),
                    params = paramsMock,
                    type = GarsonType.PRIME_SEARCH,
                )
            ),
            extraParams = CmsWidgetMapper.ExtraParams(
                internalOfferProperties = propertiesMock,
            )
        )

        Assert.assertEquals(SUBSTANCE, subtitle?.name)
    }

    @Test
    fun `use simple subtitle when not pharma`() {
        whenever(propertiesMock.isMedicine()).doReturn(false)
        val nodeProperty = cmsNodePropertyDtoTestInstance()
        val subtitle = cmsSubtitleMapper.map(
            type = CmsWidgetType.SCROLLBOX,
            properties = nodeProperty,
            garsons = listOf(
                PrimeSearchCmsWidgetGarson(
                    count = 0,
                    vendorId = null,
                    atcCode = null,
                    specificationSet = emptyList(),
                    params = paramsMock,
                    type = GarsonType.PRIME_SEARCH,
                )
            ),
            extraParams = CmsWidgetMapper.ExtraParams()
        )

        Assert.assertEquals(nodeProperty.subtitle?.text, subtitle?.name)
    }

    companion object {
        private const val TITLE = "some random title"
        private const val ACTIVE_SUBSTANCE = "Действующее вещество neotrazim"
        private const val SUBSTANCE = "По действующему веществу"
    }
}