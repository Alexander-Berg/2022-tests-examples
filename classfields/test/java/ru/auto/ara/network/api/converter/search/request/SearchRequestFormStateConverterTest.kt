package ru.auto.ara.network.api.converter.search.request

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.arbitrary.bool
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.set
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.enum
import io.kotest.property.exhaustive.exhaustive
import io.kotest.property.exhaustive.map
import io.kotest.property.exhaustive.of
import ru.auto.ara.consts.Filters.CERTIFICATE_MANUFACTURER
import ru.auto.ara.consts.Filters.ONLINE_VIEW_AVAILABLE_TAG
import ru.auto.ara.consts.Filters.PANORAMAS_TAG
import ru.auto.ara.consts.Filters.VIDEO_TAG
import ru.auto.ara.util.orderedPairOfChosenOrNulls
import ru.auto.ara.util.orderedPairOfNatsOrNulls
import ru.auto.ara.util.plusOrMinus
import ru.auto.ara.util.shouldBeSameSetAs
import ru.auto.ara.util.toNonEmptyList
import ru.auto.data.model.network.scala.offer.NWCreditGroup
import ru.auto.data.model.network.scala.search.NWCarsSearchRequestParameters
import ru.auto.data.model.network.scala.search.NWCustomsGroup
import ru.auto.data.model.network.scala.search.NWOwnersCountGroup
import ru.auto.data.model.network.scala.search.NWOwningTimeGroup
import ru.auto.data.model.network.scala.search.NWSearchRequestParams
import ru.auto.data.model.network.scala.search.NWSearchVehicleCategory
import ru.auto.data.model.network.scala.search.NWSearchView
import ru.auto.data.model.network.scala.search.NWSellerType
import ru.auto.data.model.network.scala.search.NWStateGroup
import ru.auto.data.model.network.scala.search.NWStock
import ru.auto.data.util.ALLOWED_FOR_CREDIT_TAG
import ru.auto.data.util.AUTO_CATEGORY_OLD_ID
import ru.auto.data.util.let2


class SearchRequestFormStateConverterTest : BehaviorSpec() {
    private val publicApiColors = listOf(
        "040001", "97948F", "CACECB", "FAFBFB", "EE1D19",
        "FFC0CB", "FF8649", "DEA522", "FFD600", "007F00", "22A0F8", "0000CC", "4A2197",
        "660099", "200204", "C49648"
    )

    private val topDays = listOf(1, 2, 3, 7, 2 * 7, 3 * 7, 31).map { it.toString() }

    private val searchTags = listOf(
        VIDEO_TAG,
        CERTIFICATE_MANUFACTURER,
        ALLOWED_FOR_CREDIT_TAG,
        PANORAMAS_TAG,
        ONLINE_VIEW_AVAILABLE_TAG
    )

    private val catalogEquipment = listOf(
        "fabric-seats,velvet-seats,leather,combo-interior",
        "aux",
        "multimedia",
        "cruise-control,auto-cruise",
        "multi-wheel",
        "abs",
        "alloy-wheel-disks",
        "roof-rails",
        "ptf",
        "wheel-configuration1",
        "entertainment-system-for-rear-seat-passengers",
        "passenger-seat-updown",
        "esp",
        "condition",
        "panorama-roof",
        "hatch",
        "seats-heat",
        "light-sensor",
        "bluetooth",
        "driver-seat-updown",
        "electro-mirrors",
        "wheel-power",
        "tinted-glass",
        "lock",
        "alarm",
        "rear-camera",
        "usb",
        "electro-window-front",
        "computer",
        "asr",
        "navigation",
        "xenon,led-lights",
        "parktronik",
        "mirrors-heat",
        "airbag-1",
        "wheel-heat",
        "rain-sensor",
        "light-interior,dark-interior"
    )

    private val MIN_YEAR = 1900
    private val MAX_YEAR = 2038
    private val MIN_PRICE = 1L
    private val MAX_PRICE = 9_999_999_999L
    private val POSSIBLE = "POSSIBLE"

    private val creditGroup: NWCreditGroup = NWCreditGroup(
        payment_from = 5_000,
        payment_to = 15_000,
        loan_term = 5,
        initial_fee = 10_000,
    )

    init {
        given("SearchRequestToFormStateConverter and PublicApiSearchRequestExtractor") {
            `when`("they convert SearchRequestParams to FormState and back") {
                then("`with_warranty` fields should be the same") {
                    checkAll(Arb.bool().orNull()) { warranty: Boolean? ->
                        val searchRequest = NWSearchRequestParams(with_warranty = warranty)
                        val backConverted = convertBackAndForth(searchRequest)

                        if (searchRequest.with_warranty == true) {
                            backConverted.with_warranty shouldBe true
                        } else {
                            backConverted.with_warranty shouldBe null
                        }
                    }
                }
                then("`has_image` fields should be the same") {
                    checkAll(Arb.bool().orNull()) { hasImage: Boolean? ->
                        val searchRequest = NWSearchRequestParams(has_image = hasImage)
                        val backConverted = convertBackAndForth(searchRequest)
                        if (searchRequest.has_image == null) {
                            backConverted.has_image shouldBe false
                        } else {
                            backConverted.has_image shouldBe searchRequest.has_image
                        }
                    }
                }
                then("`in_stock` fields should be the same") {
                    checkAll(Exhaustive.enum()) { inStock: NWStock ->
                        val searchRequest = NWSearchRequestParams(
                            cars_params = NWCarsSearchRequestParameters(),
                            state_group = NWStateGroup.NEW,
                            in_stock = inStock
                        )
                        val backConverted = convertBackAndForth(searchRequest)
                        backConverted.in_stock shouldBe searchRequest.in_stock
                    }
                }
                then("`state_group` fields should be the same") {
                    checkAll(Exhaustive.enum<NWStateGroup>().toArb().orNull()) { stateGroup: NWStateGroup? ->
                        val searchRequest = NWSearchRequestParams(state_group = stateGroup)
                        val backConverted = convertBackAndForth(searchRequest)

                        if (searchRequest.state_group == null) {
                            backConverted.state_group shouldBe NWStateGroup.ALL
                        } else {
                            backConverted.state_group shouldBe searchRequest.state_group
                        }
                    }
                }
                then("`color` fields should be the same") {
                    checkAll(Arb.set(exhaustive(publicApiColors), 0..publicApiColors.size).orNull()) { colors: Set<String>? ->
                        val searchRequest = NWSearchRequestParams(color = colors?.toNonEmptyList())
                        val backConverted = convertBackAndForth(searchRequest)
                        backConverted.color shouldBeSameSetAs searchRequest.color
                    }
                }
                then("`customs_state_group` fields should be the same") {
                    checkAll(Exhaustive.
                        enum<NWCustomsGroup>().toArb().orNull()
                    ) { customsGroup: NWCustomsGroup? ->
                        val searchRequest = NWSearchRequestParams(customs_state_group = customsGroup)
                        val backConverted = convertBackAndForth(searchRequest)

                        if (searchRequest.customs_state_group == null) {
                            backConverted.customs_state_group shouldBe NWCustomsGroup.DOESNT_MATTER
                        } else {
                            backConverted.customs_state_group shouldBe searchRequest.customs_state_group
                        }
                    }
                }
                then("`exchange_group` fields should be the same") {
                    checkAll(Exhaustive.of(POSSIBLE).toArb().orNull()) { exchangeGroup: String? ->
                        val searchRequest = NWSearchRequestParams(exchange_group = exchangeGroup)
                        val backConverted = convertBackAndForth(searchRequest)
                        backConverted.exchange_group shouldBe searchRequest.exchange_group
                    }
                }
                then("`top_days` fields should be the same") {
                    checkAll(exhaustive(topDays).toArb().orNull()) { topDay: String? ->
                        val searchRequest = NWSearchRequestParams(top_days = topDay)
                        val backConverted = convertBackAndForth(searchRequest)
                        backConverted.top_days shouldBe searchRequest.top_days
                    }
                }
                then("`year_from` and `year_to` fields should be the same") {
                    checkAll(orderedPairOfChosenOrNulls(MIN_YEAR, MAX_YEAR)) { (yearFrom, yearTo) ->
                        val searchRequest = NWSearchRequestParams(year_from = yearFrom, year_to = yearTo)
                        val backConverted = convertBackAndForth(searchRequest)
                        backConverted.year_from shouldBe searchRequest.year_from
                        backConverted.year_to shouldBe searchRequest.year_to
                    }
                }
                then("`price_from` and `price_to` fields should be the same") {
                    checkAll(orderedPairOfChosenOrNulls(MIN_PRICE, MAX_PRICE)) { (priceFrom, priceTo) ->
                        val searchRequest = NWSearchRequestParams(price_from = priceFrom, price_to = priceTo)
                        val backConverted = convertBackAndForth(searchRequest)
                        backConverted.price_from shouldBe searchRequest.price_from
                        backConverted.price_to shouldBe searchRequest.price_to
                    }
                }
                then("`km_age_from` and `km_age_to` fields should be the same") {
                    checkAll(orderedPairOfNatsOrNulls()) { (kmAgeFrom, kmAgeTo) ->
                        val searchRequest = NWSearchRequestParams(km_age_from = kmAgeFrom, km_age_to = kmAgeTo)
                        val backConverted = convertBackAndForth(searchRequest)
                        backConverted.km_age_from shouldBe searchRequest.km_age_from
                        backConverted.km_age_to shouldBe searchRequest.km_age_to
                    }
                }
                then("`power_from` and `power_to` fields should be the same") {
                    checkAll(orderedPairOfNatsOrNulls()) { (powerFrom, powerTo) ->
                        val searchRequest = NWSearchRequestParams(power_from = powerFrom, power_to = powerTo)
                        val backConverted = convertBackAndForth(searchRequest)
                        backConverted.power_from shouldBe searchRequest.power_from
                        backConverted.power_to shouldBe searchRequest.power_to
                    }
                }
                then("`acceleration_from` and `acceleration_to` fields should be the same") {
                    checkAll(orderedPairOfNatsOrNulls()) { (accelerationFrom, accelerationTo) ->
                        val searchRequest = NWSearchRequestParams(
                            acceleration_from = accelerationFrom,
                            acceleration_to = accelerationTo
                        )
                        val backConverted = convertBackAndForth(searchRequest)
                        backConverted.acceleration_from shouldBe searchRequest.acceleration_from
                        backConverted.acceleration_to shouldBe searchRequest.acceleration_to
                    }
                }
                then("`bucket_volume_from` and `bucket_volume_to` fields should be the same") {
                    checkAll(orderedPairOfChosenOrNulls(1, 10000)) { (displacementFrom, displacementTo) ->
                        val searchRequest = NWSearchRequestParams(
                            cars_params = NWCarsSearchRequestParameters(),
                            displacement_from = displacementFrom,
                            displacement_to = displacementTo
                        )
                        val backConverted = convertBackAndForth(searchRequest)
                        let2(searchRequest.displacement_from, backConverted.displacement_from) { (back, requested) ->
                            back should (requested plusOrMinus 50)
                        }
                            ?: run { backConverted.displacement_from shouldBe searchRequest.displacement_from }
                        let2(searchRequest.displacement_to, backConverted.displacement_to) { (back, requested) ->
                            back should (requested plusOrMinus 50)
                        }
                            ?: run { backConverted.displacement_to shouldBe searchRequest.displacement_to }

                    }
                }
                then("`seller_group` fields should be the same") {
                    checkAll(
                        Exhaustive.enum<NWSellerType>().map { listOf(it) }.toArb().orNull()
                    ) { sellerType: List<NWSellerType>? ->
                        val searchRequest = NWSearchRequestParams(seller_group = sellerType)
                        val backConverted = convertBackAndForth(searchRequest)

                        if (searchRequest.seller_group?.firstOrNull() == null) {
                            backConverted.seller_group shouldBe listOf(NWSellerType.ANY_SELLER)
                        } else {
                            backConverted.seller_group shouldBe searchRequest.seller_group
                        }
                    }
                }
                then("`owning_time_group` fields should be the same") {
                    checkAll(Exhaustive.
                        enum<NWOwningTimeGroup>().toArb().orNull()
                    ) { owningTimeGroup: NWOwningTimeGroup? ->
                        val searchRequest = NWSearchRequestParams(owning_time_group = owningTimeGroup)
                        val backConverted = convertBackAndForth(searchRequest)
                        backConverted.owning_time_group shouldBe searchRequest.owning_time_group
                    }
                }
                then("`owners_count_group` fields should be the same") {
                    checkAll(Exhaustive.
                        enum<NWOwnersCountGroup>().toArb().orNull()
                    ) { ownersCountGroup: NWOwnersCountGroup? ->
                        val searchRequest = NWSearchRequestParams(owners_count_group = ownersCountGroup)
                        val backConverted = convertBackAndForth(searchRequest)
                        backConverted.owners_count_group shouldBe searchRequest.owners_count_group
                    }
                }
                then("`pts_status` fields should be the same") {
                    checkAll(Exhaustive.of(1).toArb().orNull()) { pts: Int? ->
                        val searchRequest = NWSearchRequestParams(pts_status = pts)
                        val backConverted = convertBackAndForth(searchRequest)
                        backConverted.pts_status shouldBe searchRequest.pts_status
                    }
                }
                then("`search_tag` fields should be the same") {
                    checkAll(Arb.set(exhaustive(searchTags), 0..searchTags.size).orNull()) { searchTags: Set<String>? ->
                        val searchRequest = NWSearchRequestParams(search_tag = searchTags?.toNonEmptyList())
                        val backConverted = convertBackAndForth(searchRequest)
                        backConverted.search_tag?.flatMap { it.split(",") } shouldBeSameSetAs searchRequest.search_tag
                    }
                }
                then("`catalog_equipment` fields should be the same") {
                    checkAll(Arb.set(exhaustive(catalogEquipment), 0..catalogEquipment.size).orNull()) { extras: Set<String>? ->
                        val searchRequest = NWSearchRequestParams(catalog_equipment = extras?.toNonEmptyList())
                        val backConverted = convertBackAndForth(searchRequest)
                        backConverted.catalog_equipment shouldBeSameSetAs searchRequest.catalog_equipment
                    }
                }
                then("`credit_group` field should be the same") {
                    checkAll(Exhaustive.of(creditGroup).toArb().orNull()) {
                        val searchRequest = NWSearchRequestParams(
                            credit_group = creditGroup,
                            search_tag = listOf(ALLOWED_FOR_CREDIT_TAG)
                        )
                        val backConverted = convertBackAndForth(searchRequest)
                        backConverted.credit_group shouldBe creditGroup
                    }
                }
            }
        }
    }

    private fun convertBackAndForth(searchRequest: NWSearchRequestParams): NWSearchRequestParams {
        return SearchBackAndForthConverter.invoke(
            category = NWSearchVehicleCategory.CARS,
            oldCategory = AUTO_CATEGORY_OLD_ID,
            searchRequest = searchRequest,
            view = NWSearchView()
        )
    }
}
