package ru.auto.ara.network.api.converter.search.request

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.orNull
import io.kotest.property.checkAll
import ru.auto.data.model.network.scala.draft.NWRegionInfo
import ru.auto.data.model.network.scala.search.NWCatalogFilter
import ru.auto.data.model.network.scala.search.NWEntryView
import ru.auto.data.model.network.scala.search.NWGenView
import ru.auto.data.model.network.scala.search.NWMarkModelNameplateView
import ru.auto.data.model.network.scala.search.NWSearchRequestParams
import ru.auto.data.model.network.scala.search.NWSearchVehicleCategory
import ru.auto.data.model.network.scala.search.NWSearchView
import ru.auto.data.util.AUTO_CATEGORY_OLD_ID
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 *
 * @author jagger on 28.04.18.
 */
class SavedSearchToFormStateConverterTest : BehaviorSpec() {

    init {
        given("SearchBackAndForthConverter it converts SearchRequestParams to FormState and back") {
            `when`("there are no conflicting models") {
                then("they should be the same") {

                    val catalogFilter1 = NWCatalogFilter(
                        mark = "BMW",
                        model = "X1",
                        nameplate = 1L
                    )
                    val catalogFilter2 = NWCatalogFilter(
                        mark = "BMW",
                        model = "X3",
                        generation = 2008L
                    )
                    val view = NWSearchView(
                        mark_model_nameplate_gen_views = listOf(
                            NWMarkModelNameplateView(
                                mark = NWEntryView("BMW", ""),
                                model = NWEntryView("X1", ""),
                                nameplate = NWEntryView("1", "")
                            ),
                            NWMarkModelNameplateView(
                                mark = NWEntryView("BMW", ""),
                                model = NWEntryView("X3", ""),
                                super_gen = NWGenView("2008", "")
                            )
                        ),
                        vendor_views = listOf(),
                        regions = listOf(),
                        applied_filter_count = null,
                        applied_filter_fields = listOf(),
                        salon = null
                    )
                    val initialParams = NWSearchRequestParams(
                        catalog_filter = listOf(catalogFilter1, catalogFilter2)
                    )
                    val convertedParams = SearchBackAndForthConverter.invoke(
                        category = NWSearchVehicleCategory.CARS,
                        oldCategory = AUTO_CATEGORY_OLD_ID,
                        searchRequest = initialParams,
                        view = view
                    )
                    convertedParams.catalog_filter.shouldContainExactlyInAnyOrder(catalogFilter1, catalogFilter2)
                }
            }

            `when`("there are conflicting model") {
                then("they can be not same") {

                    val catalogFilter1 = NWCatalogFilter(
                        mark = "BMW",
                        model = "X1",
                        nameplate = 1L,
                        generation = 2009L
                    )
                    val catalogFilter2 = NWCatalogFilter(
                        mark = "BMW",
                        model = "X1",
                        generation = 2008L
                    )
                    val view = NWSearchView(
                        mark_model_nameplate_gen_views = listOf(
                            NWMarkModelNameplateView(
                                mark = NWEntryView("BMW", ""),
                                model = NWEntryView("X1", ""),
                                nameplate = NWEntryView("1", ""),
                                super_gen = NWGenView("2009", "")
                            ),
                            NWMarkModelNameplateView(
                                mark = NWEntryView("BMW", ""),
                                model = NWEntryView("X1", ""),
                                super_gen = NWGenView("2008", "")
                            )
                        ),
                        vendor_views = listOf(),
                        regions = listOf(),
                        applied_filter_count = null,
                        applied_filter_fields = listOf(),
                        salon = null
                    )
                    val initialParams = NWSearchRequestParams(
                        catalog_filter = listOf(catalogFilter1, catalogFilter2)
                    )
                    val convertedParams = SearchBackAndForthConverter.invoke(
                        category = NWSearchVehicleCategory.CARS,
                        oldCategory = AUTO_CATEGORY_OLD_ID,
                        searchRequest = initialParams,
                        view = view
                    )
                    convertedParams.catalog_filter.shouldContainExactlyInAnyOrder(catalogFilter1, catalogFilter2)
                }
            }

            `when`("there are mark and vendor") {
                then("they should be the same") {

                    val catalogFilter1 = NWCatalogFilter(
                        mark = "Audi"
                    )
                    val catalogFilter2 = NWCatalogFilter(
                        vendor = "Japan"
                    )
                    val view = NWSearchView(
                        mark_model_nameplate_gen_views = listOf(
                            NWMarkModelNameplateView(
                                mark = NWEntryView("Audi", "")
                            )
                        ),
                        vendor_views = listOf(
                            NWEntryView("Japan", "")
                        ), regions = listOf(), applied_filter_count = null, applied_filter_fields = listOf(), salon = null
                    )
                    val initialParams = NWSearchRequestParams(
                        catalog_filter = listOf(catalogFilter1, catalogFilter2)
                    )
                    val convertedParams = SearchBackAndForthConverter.invoke(
                        category = NWSearchVehicleCategory.CARS,
                        oldCategory = AUTO_CATEGORY_OLD_ID,
                        searchRequest = initialParams,
                        view = view
                    )
                    convertedParams.catalog_filter.shouldContainExactlyInAnyOrder(catalogFilter1, catalogFilter2)
                }
            }

            `when`("there are mark and same mark with model") {
                then("only mark will left") {

                    val catalogFilter1 = NWCatalogFilter(
                        mark = "BMW"
                    )
                    val catalogFilter2 = NWCatalogFilter(
                        mark = "BMW",
                        model = "X1"
                    )
                    val view = NWSearchView(
                        mark_model_nameplate_gen_views = listOf(
                            NWMarkModelNameplateView(
                                mark = NWEntryView("BMW", "")
                            ),
                            NWMarkModelNameplateView(
                                mark = NWEntryView("BMW", ""),
                                model = NWEntryView("X1", "")
                            )
                        ),
                        vendor_views = listOf(),
                        regions = listOf(),
                        applied_filter_count = null,
                        applied_filter_fields = listOf(),
                        salon = null
                    )
                    val initialParams = NWSearchRequestParams(
                        catalog_filter = listOf(catalogFilter1, catalogFilter2)
                    )
                    val convertedParams = SearchBackAndForthConverter.invoke(
                        category = NWSearchVehicleCategory.CARS,
                        oldCategory = AUTO_CATEGORY_OLD_ID,
                        searchRequest = initialParams,
                        view = view
                    )
                    convertedParams.catalog_filter.shouldContainExactly(catalogFilter1)
                }
            }

            `when`("there are regions") {
                then("`rid values should be the same`") {
                    checkAll(Arb.int().orNull()) { rid: Int? ->
                        NWSearchView(
                            regions = listOf(
                                NWRegionInfo(
                                    id = rid?.toLong(),
                                    name = "",
                                    prepositional = null,
                                    preposition = null,
                                    latitude = null,
                                    longitude = null,
                                    supports_geo_radius = false,
                                    genitive = null,
                                    dative = null,
                                    accusative = null
                                )
                            )
                        ).convertBackAndForth { requestParams ->
                            if (rid == null) {
                                assertNull(requestParams.rid)
                            } else {
                                assertNotNull(requestParams.rid) { list ->
                                    list shouldContain rid
                                }
                            }
                        }
                    }
                }
            }

        }
    }
}

private fun NWSearchView.convertBackAndForth(block: (NWSearchRequestParams) -> Unit) {
    val requestParams = SearchBackAndForthConverter.invoke(
        category = NWSearchVehicleCategory.CARS,
        oldCategory = AUTO_CATEGORY_OLD_ID,
        searchRequest = NWSearchRequestParams(),
        view = this
    )
    block(requestParams)
}
