package ru.auto.feature.search_filter

import io.qameta.allure.kotlin.Allure.step
import io.qameta.allure.kotlin.junit4.AllureRunner
import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.data.model.BasicRegion
import ru.auto.data.model.VehicleCategory
import ru.auto.data.model.data.offer.CarInfo
import ru.auto.data.model.data.offer.Documents
import ru.auto.data.model.data.offer.MarkInfo
import ru.auto.data.model.data.offer.ModelInfo
import ru.auto.data.model.data.offer.Offer
import ru.auto.data.model.data.offer.PriceInfo
import ru.auto.data.model.data.offer.SellerType
import ru.auto.data.model.filter.BodyTypeGroup
import ru.auto.data.model.filter.CarParams
import ru.auto.data.model.filter.CarSearch
import ru.auto.data.model.filter.CatalogFilter
import ru.auto.data.model.filter.CommonVehicleParams
import ru.auto.data.util.ALLOWED_FOR_CARTINDER
import ru.auto.data.util.REGION_ID_MOSCOW_REGION
import ru.auto.data.util.filterIsInstanceAndPredicate
import ru.auto.data.util.getCurrentYear
import ru.auto.data.util.toListOrEmpty
import ru.auto.feature.search_filter.factory.cartinder.CartinderCarsSearchFilterStateFactory
import ru.auto.feature.search_filter.factory.cartinder.CartinderCarsSearchRequestFactory
import ru.auto.feature.search_filter.feature.SearchFilter
import ru.auto.feature.search_filter.feature.cartinder.CartinderSearchFilter
import ru.auto.feature.search_filter.field.Field
import ru.auto.feature.search_filter.field.MultiChoiceField
import ru.auto.feature.search_filter.field.cartinder.BODY
import ru.auto.feature.search_filter.field.cartinder.CARTINDER_FILTERS_MIN_YEAR
import ru.auto.feature.search_filter.field.cartinder.CARTINDER_FILTERS_PRICE_CEIL_COEF
import ru.auto.feature.search_filter.field.cartinder.CARTINDER_FILTERS_PRICE_FLOOR_COEF
import ru.auto.feature.search_filter.field.cartinder.CartinderMultiChoiceFieldMatcher
import ru.auto.feature.search_filter.field.cartinder.MARK
import ru.auto.feature.search_filter.field.cartinder.MILEAGE
import ru.auto.feature.search_filter.field.cartinder.MODEL
import ru.auto.feature.search_filter.field.cartinder.MULTI_MARK
import ru.auto.feature.search_filter.field.cartinder.MULTI_MARK_BUTTON_ADD_MORE
import ru.auto.feature.search_filter.field.cartinder.PRICE
import ru.auto.feature.search_filter.field.cartinder.RADIUS
import ru.auto.feature.search_filter.field.cartinder.REGIONS
import ru.auto.feature.search_filter.field.cartinder.RangeFieldMatcher
import ru.auto.feature.search_filter.field.cartinder.TextFieldMatcher
import ru.auto.feature.search_filter.field.cartinder.YEAR
import ru.auto.feature.search_filter.field.cartinder.YEARS_DIFF
import ru.auto.test.tea.TeaTestFeature
import ru.auto.testextension.matchesNotNull
import ru.auto.testextension.matchesNotNullWithRepresentation

private const val MAX_MULTI_MARK = 5

@RunWith(AllureRunner::class)
class CartinderSearchFilterFeatureTest {

    @Test
    fun `should be in empty fields state`() {

        val feature = createFeature()

        step("check screen content") {
            assertThat(feature).satisfies {
                assertThat(it.currentState.fields).matchesNotNull { size == 9 }
                it.assertField<MultiChoiceField>(MULTI_MARK) { matchesNotNull { value.getAllFields().isEmpty() } }
                it.assertField<Field.SelectField>(MULTI_MARK_BUTTON_ADD_MORE) { matchesNotNull { isHidden.not() } }
            }
        }
    }

    @Test
    fun `should add multi choice`() {

        val feature = createFeature()

        step("add new mark") {
            feature.accept(
                SearchFilter.Msg.OnFieldsUpdated(
                    fields = listOf(Field.TextField(id = MARK, values = listOf(getTestTextValue("0")))),
                    multiChoiceFieldId = MULTI_MARK,
                )
            )
        }

        step("should contain multimark not empty") {
            feature.assertField<MultiChoiceField>(MULTI_MARK) {
                matchesNotNull { getActiveMultiMarkFieldValue(this, MARK) == getTestTextValue("0") }
            }
        }
    }

    @Test
    fun `should not call load count`() {

        val feature = createFeature()

        step("add new mark") {
            feature.accept(
                SearchFilter.Msg.OnFieldsUpdated(
                    fields = listOf(Field.TextField(id = MARK, values = listOf(getTestTextValue("0")))),
                    multiChoiceFieldId = MULTI_MARK,
                )
            )
        }

        step("should not contain Eff.LoadCount") {
            assertThat(feature.latestEffects).matches { effs ->
                effs.any { eff -> eff is SearchFilter.Eff.LoadCount }.not()
            }
        }
    }

    @Test
    fun `should hide button add_more when multi choice count max`() {

        val feature = createFeature()

        step("add more multi choices") {
            for (i in 0..9) {
                feature.accept(
                    SearchFilter.Msg.OnFieldsUpdated(
                        fields = listOf(Field.TextField(id = MARK, values = listOf(getTestTextValue(i.toString())))),
                        multiChoiceFieldId = MULTI_MARK,
                    )
                )
            }
        }

        step("multichoice size should be $MAX_MULTI_MARK and button \"add more\" should be hidden") {
            feature.assertField<MultiChoiceField>(MULTI_MARK) {
                matchesNotNull { value.getAllFields().size == MAX_MULTI_MARK }
            }

            feature.assertField<Field.SelectField>(MULTI_MARK_BUTTON_ADD_MORE) {
                matchesNotNullWithRepresentation(
                    predicateDescription = "isHidden == true",
                    representationMessageBuilder = { "isHidden == ${it.isHidden}" }
                ) { isHidden }
            }
        }
    }

    @Test
    fun `should create search params for default`() {

        val search = searchRequestFactory.createSearch(createFeature().currentState.getFields())
        step("search should be same to") {
            assertThat(search.search).isEqualTo(
                getCarSearch(
                    priceTo = sampleOffer.priceInfo?.price?.let { it * CARTINDER_FILTERS_PRICE_CEIL_COEF }?.toLong(),
                    priceFrom = sampleOffer.priceInfo?.price?.let { it * CARTINDER_FILTERS_PRICE_FLOOR_COEF }?.toLong(),
                    yearFrom = ((sampleOffer.documents?.year ?: 0) - YEARS_DIFF).toInt(),
                    yearTo = ((sampleOffer.documents?.year ?: 0) + YEARS_DIFF).toInt(),
                    searchTags = listOf(ALLOWED_FOR_CARTINDER)
                )
            )
        }
    }

    @Test
    fun `should create search params for all cleared`() {

        val feature = createFeature()

        step("clear all fields") {
            feature.accept(SearchFilter.Msg.ClearFilter)
        }

        step("mark mode multi choice should be empty all fields values should be empty") {

        }

        val search = searchRequestFactory.createSearch(feature.currentState.getFields())
        step("search should be same to") {
            assertThat(search.search).isEqualTo(
                getCarSearch(
                    yearFrom = CARTINDER_FILTERS_MIN_YEAR.toInt(),
                    yearTo = getCurrentYear(),
                    searchTags = listOf(ALLOWED_FOR_CARTINDER)
                )
            )
        }
    }

    @Test
    fun `should create search params for user input`() {

        val feature = createFeature()
        val priceTo = 300_000L
        val priceFrom = 20_000L
        val yearTo = 2000L
        val yearFrom = 2010L
        val kmAgeTo = 100_000L
        val radius = 500L

        step("update mark model field") {
            feature.accept(
                SearchFilter.Msg.OnFieldsUpdated(
                    fields = listOf(
                        Field.TextField(id = MARK, values = listOf(getTestTextValue("Audi"))),
                        Field.TextField(id = MODEL, values = listOf(
                            getTestTextValue("1"),
                            getTestTextValue("2"),
                            getTestTextValue("3"),
                        )),
                    ),
                    multiChoiceFieldId = MULTI_MARK,
                )
            )
        }

        step("update body type field") {
            feature.accept(
                SearchFilter.Msg.OnFieldsUpdated(
                    fields = listOf(Field.TextField(
                        id = BODY,
                        values = getTestTextValue(BodyTypeGroup.SEDAN.id ?: "", BodyTypeGroup.SEDAN.name).toListOrEmpty()
                    ))
                )
            )
        }

        step("update year from - to") {
            feature.accept(
                SearchFilter.Msg.OnRangeFieldUpdated(fieldId = PRICE, from = priceFrom, to = priceTo)
            )
        }

        step("update year from - to") {
            feature.accept(
                SearchFilter.Msg.OnRangeFieldUpdated(fieldId = YEAR, from = yearFrom, to = yearTo)
            )
        }

        step("update km age") {
            feature.accept(
                SearchFilter.Msg.OnRangeFieldUpdated(MILEAGE, from = null, to = kmAgeTo)
            )
        }

        step("update distance") {
            feature.accept(
                SearchFilter.Msg.OnRangeFieldUpdated(fieldId = RADIUS, from = null, to = radius)
            )
        }

        step("update region") {
            feature.accept(
                SearchFilter.Msg.OnFieldsUpdated(
                    fields = listOf(
                        Field.TextField(REGIONS, getTestTextValue(REGION_ID_MOSCOW_REGION, "Moscow").toListOrEmpty())
                    )
                )
            )
        }

        val search = searchRequestFactory.createSearch(feature.currentState.getFields())
        step("search should be same to") {
            assertThat(search.search).isEqualTo(
                getCarSearch(
                    priceFrom = priceFrom,
                    priceTo = priceTo,
                    yearFrom = yearFrom.toInt(),
                    yearTo = yearTo.toInt(),
                    searchTags = listOf(ALLOWED_FOR_CARTINDER),
                    markToModels = mapOf("Audi" to listOf("1", "2", "3")),
                    kmAgeTo = kmAgeTo.toInt(),
                    bodyTypeGroup = listOf(BodyTypeGroup.SEDAN),
                    regions = listOf(BasicRegion(REGION_ID_MOSCOW_REGION, "Moscow")),
                    geoRadius = radius.toInt(),
                    geoRadiusSupport = true
                )
            )
        }
    }

    @Test
    fun `should open filter for search params and get proper search on finish`() {
        val initialSearch = getCarSearch(
            priceTo = sampleOffer.priceInfo?.price?.let { it * CARTINDER_FILTERS_PRICE_CEIL_COEF }?.toLong(),
            priceFrom = sampleOffer.priceInfo?.price?.let { it * CARTINDER_FILTERS_PRICE_FLOOR_COEF }?.toLong(),
            yearFrom = ((sampleOffer.documents?.year ?: 0) - YEARS_DIFF).toInt(),
            yearTo = ((sampleOffer.documents?.year ?: 0) + YEARS_DIFF).toInt(),
            searchTags = listOf(ALLOWED_FOR_CARTINDER),
            markToModels = mapOf("Audi" to listOf("1", "2", "3")),
            kmAgeTo = 100_000
        )
        val feature = createFeature(initialSearch)

        feature.assertField<Field.RangeField>(MILEAGE) {
            matchesNotNull { it.to == 100_000L }
        }

        feature.assertField<MultiChoiceField>(MULTI_MARK) {
            matchesNotNullWithRepresentation(
                "multi choice size should be == 1",
                { multiChoiceField -> "multi choice size ${multiChoiceField.value.getAllFields().size}" }
            ) { value.getAllFields().size == 1 }

            matchesNotNull {
                value.getAllFields().values.singleOrNull()?.let {
                    it.getField<Field.TextField>(MARK)?.values == listOf(getTestTextValue("Audi"))
                } == true
            }

            matchesNotNull {
                value.getAllFields().values.singleOrNull()?.let {
                    it.getField<Field.TextField>(MODEL)?.values == listOf(
                        getTestTextValue("1"),
                        getTestTextValue("2"),
                        getTestTextValue("3"),
                    )
                } == true
            }
        }
        val resultSearch = searchRequestFactory.createSearch(feature.currentState.getFields())

        step("search should be same to") {
            assertThat(resultSearch.search).isEqualTo(initialSearch)
        }
    }

    @Test
    fun `should open filter for search params and get proper search on finish after user input`() {
        val initialSearch = getCarSearch(
            priceTo = sampleOffer.priceInfo?.price?.let { it * CARTINDER_FILTERS_PRICE_CEIL_COEF }?.toLong(),
            priceFrom = sampleOffer.priceInfo?.price?.let { it * CARTINDER_FILTERS_PRICE_FLOOR_COEF }?.toLong(),
            yearFrom = ((sampleOffer.documents?.year ?: 0) - YEARS_DIFF).toInt(),
            yearTo = ((sampleOffer.documents?.year ?: 0) + YEARS_DIFF).toInt(),
            searchTags = listOf(ALLOWED_FOR_CARTINDER),
            markToModels = mapOf("Audi" to listOf("1", "2", "3")),
            kmAgeTo = 100_000
        )
        val feature = createFeature(initialSearch)

        step("input one more mark with multiply models") {
            feature.accept(
                SearchFilter.Msg.OnFieldsUpdated(
                    fields = listOf(
                        Field.TextField(id = MARK, values = listOf(getTestTextValue("BMW"))),
                        Field.TextField(id = MODEL, values = listOf(
                            getTestTextValue("1"),
                            getTestTextValue("2"),
                        ))
                    ),
                    multiChoiceFieldId = MULTI_MARK,
                )
            )
        }

        val resultSearch = searchRequestFactory.createSearch(feature.currentState.getFields())

        step("search should be same") {
            assertThat(resultSearch.search).isEqualTo(
                initialSearch.copy(
                    commonParams = initialSearch.commonParams.copy(
                        catalogFilters = mapOf(
                            "Audi" to listOf("1", "2", "3"),
                            "BMW" to listOf("1", "2"),
                        ).toCatalogFilters()
                    )
                )
            )
        }
    }

    private inline fun <reified T : Field> List<Field>.getField(id: String): T? =
        filterIsInstanceAndPredicate<T> { it.id == id }.singleOrNull()

    private inline fun <reified T : Field> TeaTestFeature<SearchFilter.Msg, SearchFilter.State, SearchFilter.Eff>.assertField(
        id: String,
        crossinline requirements: AbstractAssert<*, T?>.(T) -> Unit,
    ) = currentState.fields.getField<T>(id)?.let { field ->
        val abstractAssert = assertThat(field)
        abstractAssert.satisfies { abstractAssert.requirements(it) }
    }

    private inline fun <reified T : Field> Map<String, Field>.getField(id: String): T? = values.toList().getField(id)

    private fun getActiveMultiMarkFieldValue(field: MultiChoiceField, id: String) =
        field.value.getFieldsForActiveMultiChoice().getField<Field.TextField>(id)?.values?.singleOrNull()

    private val sampleOffer = Offer(
        category = VehicleCategory.CARS,
        id = "0",
        sellerType = SellerType.PRIVATE,
        carInfo = CarInfo(
            markInfo = MarkInfo(
                id = "Audi",
                code = "Audi",
                name = "Audi",
            ),
            modelInfo = ModelInfo(
                id = "1000",
                code = "1000",
                name = "1000"
            )
        ),
        priceInfo = PriceInfo(100_000),
        documents = Documents(year = 2000)
    )

    private val searchRequestFactory = CartinderCarsSearchRequestFactory()

    private fun getCarSearch(
        geoRadius: Int? = null,
        geoRadiusSupport: Boolean = geoRadius != null,
        yearFrom: Int? = null,
        yearTo: Int? = null,
        priceFrom: Long? = null,
        priceTo: Long? = null,
        kmAgeTo: Int? = null,
        bodyTypeGroup: List<BodyTypeGroup> = emptyList(),
        markToModels: Map<String, List<String>> = emptyMap(),
        regions: List<BasicRegion> = emptyList(),
        searchTags: List<String> = emptyList(),
    ) = CarSearch(
        CarParams(
            bodyTypeGroup = bodyTypeGroup
        ),
        CommonVehicleParams(
            catalogFilters = markToModels.toCatalogFilters(),
            yearFrom = yearFrom,
            yearTo = yearTo,
            priceFrom = priceFrom,
            priceTo = priceTo,
            kmAgeTo = kmAgeTo,
            geoRadius = geoRadius,
            geoRadiusSupport = geoRadiusSupport,
            regions = regions,
            searchTag = searchTags
        )
    )

    private fun Map<String, List<String>>.toCatalogFilters() = map { (mark, models) ->
        models.map { model -> CatalogFilter(mark = mark, model = model) }
    }.flatten()

    private fun createFeature(
        search: CarSearch? = null,
    ) = TeaTestFeature(
        initialState = CartinderCarsSearchFilterStateFactory(
            matchers = listOf(
                TextFieldMatcher(search = search, offer = sampleOffer){ Field.TextField.Value("") },
                RangeFieldMatcher(search = search, offer = sampleOffer),
                CartinderMultiChoiceFieldMatcher(search = search,
                    maxMultiChoiceCount = MAX_MULTI_MARK)
            )
        ).buildState(),
        reducer = CartinderSearchFilter::reduce
    )

    private fun getTestTextValue(id: String, label: String = id) = Field.TextField.Value(id, label)
}
