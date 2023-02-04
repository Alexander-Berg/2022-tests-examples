package ru.auto.ara.filter.screen.user

import com.yandex.mobile.vertical.dynamicscreens.model.field.BaseFieldWithValue
import io.qameta.allure.kotlin.junit4.AllureRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.auto.ara.consts.Filters
import ru.auto.ara.consts.Filters.GLOBAL_CATEGORY_FIELD
import ru.auto.ara.consts.Filters.MARK_FIELD
import ru.auto.ara.consts.Filters.MODEL_FIELD
import ru.auto.ara.consts.Filters.SERVICES_FIELD
import ru.auto.ara.consts.Filters.STATUS_FIELD
import ru.auto.ara.consts.Filters.VIN_RESOLUTION_FIELD
import ru.auto.ara.data.entities.form.Option
import ru.auto.ara.draft.field.SegmentDynamicField
import ru.auto.ara.filter.fields.GlobalCategoryField
import ru.auto.ara.filter.fields.MarkField
import ru.auto.ara.filter.fields.ModelField
import ru.auto.ara.filter.fields.PriceInputField
import ru.auto.ara.filter.fields.SelectField
import ru.auto.ara.filter.screen.FilterScreen
import ru.auto.ara.filter.screen.user.CampaignFactory.getMotoSubcategories
import ru.auto.ara.filter.screen.user.CampaignFactory.getTruckSubcategories
import ru.auto.ara.network.response.BasicItem
import ru.auto.ara.network.response.GetListItem
import ru.auto.ara.util.SerializablePair
import ru.auto.ara.util.android.OptionsProvider
import ru.auto.ara.util.android.StringsProvider
import ru.auto.data.model.Campaign
import ru.auto.data.model.DealerOffersFilter
import ru.auto.data.model.data.offer.ACTIVE
import ru.auto.data.model.data.offer.ALL
import ru.auto.data.model.data.offer.CAR
import ru.auto.data.model.data.offer.MOTO
import ru.auto.data.model.data.offer.TRUCKS
import ru.auto.data.model.data.offer.USED
import ru.auto.data.util.EXCLUDE_VIN_OFFERS_HISTORY
import ru.auto.data.util.VAS_AUTOUP_APPLY
import ru.auto.data.util.VAS_DEALER_AUTOSTRATEGY_FIRST_PAGE
import ru.auto.data.util.VIN_RESOLUTION_ERROR_TAG
import ru.auto.data.util.VIN_RESOLUTION_INVALID_TAG

/**
 * @author aleien on 17.07.18.
 */
@RunWith(AllureRunner::class) class DealerFilterScreenConverterTest {
    private val strings: StringsProvider = mock()
    private val options: OptionsProvider<Option> = mock()

    @Before
    fun setup() {
        whenever(strings.get(any())).thenReturn("Label")
        whenever(options.get(any())).thenReturn(listOf(Option("a", "b")))
    }

    @Test
    fun `default filter screen should have 'all' category`() {
        val screen = buildScreen(CampaignFactory.buildAll(), ALL)
        assertThat(screen.getValueFieldById<String?>(GLOBAL_CATEGORY_FIELD).value).isEqualToIgnoringCase(ALL)
    }

    @Test
    fun `car screen should have car category`() {
        val screen = buildScreen(CampaignFactory.buildCarMoto())
        screen.getFieldByIdAs<GlobalCategoryField>(GLOBAL_CATEGORY_FIELD).value = CAR

        val filter = DealerOffersFilterScreenConverter.fromScreen(screen)
        assertThat(filter.category).isEqualTo(CAR)
    }

    @Test
    fun `car screen should not have moto subcategory`() {
        val screen = buildScreen(CampaignFactory.buildCarMoto())
        screen.getFieldByIdAs<GlobalCategoryField>(GLOBAL_CATEGORY_FIELD).value = CAR

        val filter = DealerOffersFilterScreenConverter.fromScreen(screen)
        assertThat(filter.motoCategory).isNull()
    }

    @Test
    fun `car screen should not have truck subcategory`() {
        val screen = buildScreen(CampaignFactory.buildCarMoto())
        val filter = DealerOffersFilterScreenConverter.fromScreen(screen)
        assertThat(filter.truckCategory).isNull()
    }

    @Test
    fun `moto screen should have moto category`() {
        val screen = buildScreen(CampaignFactory.buildCarMoto())
        screen.getFieldByIdAs<GlobalCategoryField>(GLOBAL_CATEGORY_FIELD).value = MOTO

        val filter = DealerOffersFilterScreenConverter.fromScreen(screen)
        assertThat(filter.category).isEqualTo(MOTO)
    }

    @Test
    fun `trucks screen should have trucks category`() {
        val screen = buildScreen(CampaignFactory.buildCarMoto())
        screen.getFieldByIdAs<GlobalCategoryField>(GLOBAL_CATEGORY_FIELD).value = TRUCKS

        val filter = DealerOffersFilterScreenConverter.fromScreen(screen)
        assertThat(filter.category).isEqualTo(TRUCKS)
    }

    @Test
    fun `default moto screen should have null subcategory`() {
        val screen = buildScreen(CampaignFactory.buildCarMotoComm())
        screen.getFieldByIdAs<GlobalCategoryField>(GLOBAL_CATEGORY_FIELD).value = MOTO

        val filter = DealerOffersFilterScreenConverter.fromScreen(screen)
        assertThat(filter.motoCategory).isNull()
    }

    @Test
    fun `default trucks screen should have null subcategory`() {
        val screen = buildScreen(CampaignFactory.buildCarMotoComm())
        screen.getFieldByIdAs<GlobalCategoryField>(GLOBAL_CATEGORY_FIELD).value = TRUCKS

        val filter = DealerOffersFilterScreenConverter.fromScreen(screen)
        assertThat(filter.truckCategory).isNull()
    }

    @Test
    fun `if there are several state field values first one should be selected as default`() {
        val screen = buildScreen(CampaignFactory.buildCarMotoComm())
        screen.getFieldByIdAs<GlobalCategoryField>(GLOBAL_CATEGORY_FIELD).value = CAR

        val stateValues = screen.getFieldByIdAs<SegmentDynamicField>(Filters.STATE_FIELD).itemsByKeys.toList()

        val filter = DealerOffersFilterScreenConverter.fromScreen(screen)
        assertThat(filter.state).isEqualTo(stateValues.firstOrNull()?.first?.takeIf { it != ALL })
    }

    @Test
    fun `if there are several state field values in moto, but only used in cars, moto screen should use 'all' as default value`() {
        val screen = buildScreen(CampaignFactory.buildCarMotoComm())
        screen.getFieldByIdAs<GlobalCategoryField>(GLOBAL_CATEGORY_FIELD).value = MOTO

        val filter = DealerOffersFilterScreenConverter.fromScreen(screen)
        assertThat(filter.state).isNull()
    }

    @Test
    fun `'all' state should convert to null`() {
        val screen = buildScreen(CampaignFactory.buildCarMotoComm())
        screen.getFieldByIdAs<GlobalCategoryField>(GLOBAL_CATEGORY_FIELD).value = MOTO

        screen.getFieldByIdAs<SegmentDynamicField>(Filters.STATE_FIELD).value = ALL

        val filter = DealerOffersFilterScreenConverter.fromScreen(screen)
        assertThat(filter.state).isNull()
    }

    @Test
    fun `if status field is empty it should convert to null`() {
        val screen = buildScreen(CampaignFactory.buildCarMotoComm())
        screen.getFieldByIdAs<GlobalCategoryField>(GLOBAL_CATEGORY_FIELD).value = CAR

        val filter = DealerOffersFilterScreenConverter.fromScreen(screen)
        assertThat(filter.status).isNull()
    }

    @Test
    fun `if status field is not empty it should convert as it's key`() {
        val screen = buildScreen(CampaignFactory.buildCarMotoComm())
        screen.getFieldByIdAs<GlobalCategoryField>(GLOBAL_CATEGORY_FIELD).value = CAR

        val option = Option(ACTIVE, "Активные")
        screen.getFieldByIdAs<SelectField>(STATUS_FIELD).value = option

        val filter = DealerOffersFilterScreenConverter.fromScreen(screen)
        assertThat(filter.status).isEqualTo(option.key)
    }

    @Test
    fun `if mark field is not empty it should convert as mark id`() {
        val screen = buildScreen(CampaignFactory.buildCarMotoComm())
        screen.getFieldByIdAs<GlobalCategoryField>(GLOBAL_CATEGORY_FIELD).value = CAR

        val mark = BasicItem().apply {
            newId = "markcode"
            id = "markid"
            name = "markname"
        }
        screen.getFieldByIdAs<MarkField>(MARK_FIELD).value = mark

        val filter = DealerOffersFilterScreenConverter.fromScreen(screen)
        assertThat(filter.markCode).isEqualTo(mark.newId)
    }

    @Test
    fun `if model field is not empty it should convert as model id`() {
        val screen = buildScreen(CampaignFactory.buildCarMotoComm())
        screen.getFieldByIdAs<GlobalCategoryField>(GLOBAL_CATEGORY_FIELD).value = CAR

        val model = GetListItem().apply {
            newId = "modelcode"
            id = "modelid"
            nameplate = "modelname"
        }
        screen.getFieldByIdAs<ModelField>(MODEL_FIELD).value = model

        val filter = DealerOffersFilterScreenConverter.fromScreen(screen)
        assertThat(filter.modelCode).isEqualTo(model.newId)
    }

    @Test
    fun `if there are mark and model selected, getMarkModel() result should be separated with #`() {
        val mark = "mark"
        val model = "model"
        val userFilter = DealerOffersFilter(markCode = mark, modelCode = model)
        assertThat(userFilter.getMarkModel()).isEqualTo("$mark#$model")
    }

    @Test
    fun `if only mark selected getMarkModel() should return only mark`() {
        val mark = "mark"
        val userFilter = DealerOffersFilter(markCode = mark)
        assertThat(userFilter.getMarkModel()).isEqualTo(mark)
    }

    @Test
    fun `if no mark selected getMarkModel() should return null`() {
        val userFilter = DealerOffersFilter()
        assertThat(userFilter.getMarkModel()).isNull()
    }

    @Test
    fun `if there is service selected, it should convert as service id`() {
        val screen = buildScreen(CampaignFactory.buildCarMotoComm())
        val service = "all_sale_badge"

        val value = Option(service, "С бейджами")
        screen.getFieldByIdAs<SelectField>(SERVICES_FIELD).value = value

        val filter = DealerOffersFilterScreenConverter.fromScreen(screen)
        assertThat(filter.service).isEqualTo(service)
    }

    @Test
    fun `if no service selected, it should convert as null`() {
        val screen = buildScreen(CampaignFactory.buildCarMotoComm())
        val filter = DealerOffersFilterScreenConverter.fromScreen(screen)
        assertThat(filter.service).isNull()
    }

    @Test
    fun `if auto_apply_service selected, it should convert as tag`() {
        val screen = buildScreen(CampaignFactory.buildCarMotoComm())
        val service = VAS_AUTOUP_APPLY

        val value = Option(service, "С автоподнятием")
        screen.getFieldByIdAs<SelectField>(SERVICES_FIELD).value = value

        val filter = DealerOffersFilterScreenConverter.fromScreen(screen)
        assertThat(filter.tags).containsOnly(service)
    }

    @Test
    fun `if auto_apply_service selected, it should not convert as service id`() {
        val screen = buildScreen(CampaignFactory.buildCarMotoComm())
        val service = VAS_AUTOUP_APPLY

        val value = Option(service, "С автоподнятием")
        screen.getFieldByIdAs<SelectField>(SERVICES_FIELD).value = value

        val filter = DealerOffersFilterScreenConverter.fromScreen(screen)
        assertThat(filter.service).isNull()
    }

    @Test
    fun `if autostrategy_first_page selected, it should convert as tag`() {
        val screen = buildScreen(CampaignFactory.buildCarMotoComm())
        val service = VAS_DEALER_AUTOSTRATEGY_FIRST_PAGE

        val value = Option(service, "С автостратегией")
        screen.getFieldByIdAs<SelectField>(SERVICES_FIELD).value = value

        val filter = DealerOffersFilterScreenConverter.fromScreen(screen)
        assertThat(filter.tags).containsOnly(service)
    }

    @Test
    fun `if exclude_history selected, it should convert as exclude_tag`() {
        val screen = buildScreen(CampaignFactory.buildCarMotoComm())
        val excludeHistory = EXCLUDE_VIN_OFFERS_HISTORY

        val value = Option(excludeHistory, "Без истории")
        screen.getFieldByIdAs<SelectField>(VIN_RESOLUTION_FIELD).value = value

        val filter = DealerOffersFilterScreenConverter.fromScreen(screen)
        assertThat(filter.excludeTags).containsOnly("vin_offers_history")
        assertThat(filter.tags).isEmpty()
    }

    @Test
    fun `if vin_resolution_error selected it should convert as several tags`() {
        val screen = buildScreen(CampaignFactory.buildCarMotoComm())
        val vinError = VIN_RESOLUTION_ERROR_TAG

        val value = Option(vinError, "Красные отчеты")
        screen.getFieldByIdAs<SelectField>(VIN_RESOLUTION_FIELD).value = value

        val filter = DealerOffersFilterScreenConverter.fromScreen(screen)
        assertThat(filter.tags).containsExactlyInAnyOrder(VIN_RESOLUTION_ERROR_TAG, VIN_RESOLUTION_INVALID_TAG)
        assertThat(filter.excludeTags).isEmpty()
    }

    @Test
    fun `if vin tags are selected and service tag is selected, they all should convert to tags`() {
        val screen = buildScreen(CampaignFactory.buildCarMotoComm())
        val vinError = VIN_RESOLUTION_ERROR_TAG

        val value = Option(vinError, "Красные отчеты")
        screen.getFieldByIdAs<SelectField>(VIN_RESOLUTION_FIELD).value = value

        val service = VAS_DEALER_AUTOSTRATEGY_FIRST_PAGE

        val serviceValue = Option(service, "С автостратегией")
        screen.getFieldByIdAs<SelectField>(SERVICES_FIELD).value = serviceValue

        val filter = DealerOffersFilterScreenConverter.fromScreen(screen)
        assertThat(filter.tags).containsExactlyInAnyOrder(VIN_RESOLUTION_ERROR_TAG, VIN_RESOLUTION_INVALID_TAG, service)
        assertThat(filter.excludeTags).isEmpty()
    }

    @Test
    fun `if selected category is "all" getParamsCount should return 0`() {
        val screen = buildScreen(CampaignFactory.buildCarMotoComm(), ALL)

        assertThat(screen.nonDefaultFieldsNumber).isEqualTo(0)
    }

    @Test
    fun `if only category selected getParamsCount should return 1`() {
        val screen = buildScreen(CampaignFactory.buildCarMotoComm())

        assertThat(screen.nonDefaultFieldsNumber).isEqualTo(1)
    }

    @Test
    fun `if category, service and vin selected getParamsCount should return 3`() {
        val screen = buildScreen(CampaignFactory.buildCarMotoComm())

        val vinError = VIN_RESOLUTION_ERROR_TAG

        val value = Option(vinError, "Красные отчеты")
        screen.getFieldByIdAs<SelectField>(VIN_RESOLUTION_FIELD).value = value

        val service = VAS_DEALER_AUTOSTRATEGY_FIRST_PAGE

        val serviceValue = Option(service, "С автостратегией")
        screen.getFieldByIdAs<SelectField>(SERVICES_FIELD).value = serviceValue

        assertThat(screen.nonDefaultFieldsNumber).isEqualTo(3)
    }

    @Test
    fun `if only category and state selected getParamsCount should return 2`() {
        val screen = buildScreen(CampaignFactory.buildCarMotoComm())

        screen.getFieldByIdAs<SegmentDynamicField>(Filters.STATE_FIELD).value = USED

        val filter = DealerOffersFilterScreenConverter.fromScreen(screen)
        println(filter)
        assertThat(screen.nonDefaultFieldsNumber).isEqualTo(2)
    }

    @Test
    fun `if only price from selected, price to should be null`() {
        val screen = buildScreen(CampaignFactory.buildCarMotoComm())

        screen.getFieldByIdAs<PriceInputField>(Filters.PRICE_FIELD).value = SerializablePair.create(1.toDouble(), 0.toDouble())

        val filter = DealerOffersFilterScreenConverter.fromScreen(screen)
        assertThat(filter.priceTo).isNull()
    }

    @Test
    fun `if only price from selected, price from should be converted as int`() {
        val screen = buildScreen(CampaignFactory.buildCarMotoComm())

        val priceFrom = 1000
        screen.getFieldByIdAs<PriceInputField>(Filters.PRICE_FIELD).value =
                SerializablePair.create(priceFrom.toDouble(), 0.toDouble())

        val filter = DealerOffersFilterScreenConverter.fromScreen(screen)
        assertThat(filter.priceFrom).isEqualTo(priceFrom)
    }


    @Test
    fun `if only price to selected, priceFrom should be null`() {
        val screen = buildScreen(CampaignFactory.buildCarMotoComm())

        screen.getFieldByIdAs<PriceInputField>(Filters.PRICE_FIELD).value = SerializablePair.create(0.toDouble(), 1.toDouble())

        val filter = DealerOffersFilterScreenConverter.fromScreen(screen)
        assertThat(filter.priceFrom).isNull()
    }

    @Test
    fun `if only price to selected, priceTo should be converted as int`() {
        val screen = buildScreen(CampaignFactory.buildCarMotoComm())

        val priceTo = 1000
        screen.getFieldByIdAs<PriceInputField>(Filters.PRICE_FIELD).value =
                SerializablePair.create(0.toDouble(), priceTo.toDouble())

        val filter = DealerOffersFilterScreenConverter.fromScreen(screen)
        assertThat(filter.priceTo).isEqualTo(priceTo)
    }

    @Test
    fun `if both price from and price to are selected, they both should be converted`() {
        val screen = buildScreen(CampaignFactory.buildCarMotoComm())

        val priceFrom = 10_000
        val priceTo = 100_000_000
        screen.getFieldByIdAs<PriceInputField>(Filters.PRICE_FIELD).value = SerializablePair.create(
            priceFrom.toDouble(),
            priceTo.toDouble()
        )

        val filter = DealerOffersFilterScreenConverter.fromScreen(screen)
        assertThat(filter.priceTo).isEqualTo(priceTo)
        assertThat(filter.priceFrom).isEqualTo(priceFrom)
    }

    @Test
    fun `if priceFrom and priceTo selected getParamsCount should return 1`() {
        val screen = buildScreen(CampaignFactory.buildCarMotoComm())

        screen.getFieldByIdAs<PriceInputField>(Filters.PRICE_FIELD).value = SerializablePair.create(
            1.toDouble(),
            1000000.toDouble()
        )
    }


    private fun buildScreen(campaigns: List<Campaign>, category: String? = null): DealerFilterScreen =
        DealerFilterScreen.Builder(
            category ?: campaigns[0].category,
            strings, options,
            campaigns,
            getMotoSubcategories(), getTruckSubcategories()
        ).build() as DealerFilterScreen

    @Suppress("UNCHECKED_CAST")
    private fun <T : BaseFieldWithValue<*>> FilterScreen.getFieldByIdAs(id: String): T = getFieldById(id) as T

}
