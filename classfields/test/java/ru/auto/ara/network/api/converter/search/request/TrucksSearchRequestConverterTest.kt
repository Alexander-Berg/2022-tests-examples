package ru.auto.ara.network.api.converter.search.request

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.set
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.enum
import io.kotest.property.exhaustive.exhaustive
import ru.auto.ara.data.models.FormState
import ru.auto.ara.filter.mapper.TruckParamsExtractor
import ru.auto.ara.ui.helpers.form.util.TrucksSearchRequestToFormStateConverter
import ru.auto.ara.util.orderedPairOfNatsOrNulls
import ru.auto.ara.util.plusOrMinus
import ru.auto.ara.util.shouldBeSameSetAs
import ru.auto.ara.util.toNonEmptyList
import ru.auto.data.model.filter.TruckParams
import ru.auto.data.model.network.scala.catalog.NWSteeringWheel
import ru.auto.data.model.network.scala.common.NWEngineType
import ru.auto.data.model.network.scala.search.NWBrakeType
import ru.auto.data.model.network.scala.search.NWBusType
import ru.auto.data.model.network.scala.search.NWCabinType
import ru.auto.data.model.network.scala.search.NWEuroClassType
import ru.auto.data.model.network.scala.search.NWLightTruckType
import ru.auto.data.model.network.scala.search.NWSaddleHeight
import ru.auto.data.model.network.scala.search.NWSuspensionCabinType
import ru.auto.data.model.network.scala.search.NWSuspensionChassisType
import ru.auto.data.model.network.scala.search.NWSuspensionType
import ru.auto.data.model.network.scala.search.NWTrailerType
import ru.auto.data.model.network.scala.search.NWTruckCategory
import ru.auto.data.model.network.scala.search.NWTruckGearType
import ru.auto.data.model.network.scala.search.NWTruckType
import ru.auto.data.model.network.scala.search.NWTrucksSearchRequestParameters
import ru.auto.data.model.network.scala.search.NWTrucksTransmission
import ru.auto.data.model.network.scala.search.NWWheelDrive
import ru.auto.data.model.network.scala.search.converter.TruckParamsConverter
import ru.auto.data.util.BUS_SUB_CATEGORY_OLD_ID
import ru.auto.data.util.COMMERCIAL_CATEGORY_OLD_ID
import ru.auto.data.util.CRANE_HYDRAULICS_SUB_CATEGORY_OLD_ID
import ru.auto.data.util.TRAILER_SUB_CATEGORY_OLD_ID
import ru.auto.data.util.TRUCK_SUB_CATEGORY_OLD_ID
import ru.auto.data.util.TRUCK_TRACTOR_SUB_CATEGORY_OLD_ID

class TrucksSearchRequestConverterTest : BehaviorSpec() {
    private val euroClasses = listOf(
            listOf(
                NWEuroClassType.EURO_0,
                NWEuroClassType.EURO_1,
                NWEuroClassType.EURO_2,
                NWEuroClassType.EURO_3,
                NWEuroClassType.EURO_4,
                NWEuroClassType.EURO_5,
                NWEuroClassType.EURO_GREEN
            ),
            listOf(
                NWEuroClassType.EURO_1,
                NWEuroClassType.EURO_2,
                NWEuroClassType.EURO_3,
                NWEuroClassType.EURO_4,
                NWEuroClassType.EURO_5,
                NWEuroClassType.EURO_GREEN
            ),
            listOf(
                NWEuroClassType.EURO_2,
                NWEuroClassType.EURO_3,
                NWEuroClassType.EURO_4,
                NWEuroClassType.EURO_5,
                NWEuroClassType.EURO_GREEN
            ),
            listOf(
                NWEuroClassType.EURO_3,
                NWEuroClassType.EURO_4,
                NWEuroClassType.EURO_5,
                NWEuroClassType.EURO_GREEN
            ),
            listOf(
                NWEuroClassType.EURO_4,
                NWEuroClassType.EURO_5,
                NWEuroClassType.EURO_GREEN
            ),
            listOf(
                NWEuroClassType.EURO_5,
                NWEuroClassType.EURO_GREEN
            ),
            listOf(NWEuroClassType.EURO_GREEN)
    )

    init {
        given("TrucksSearchRequestToFormStateConverter and PublicApiTrucksRequestExtractor") {
            `when`("they convert TrucksSearchRequestParameters to FormState and back") {
                then("`transmission` fields should be the same") {
                    checkAll(Arb.set(
                        Exhaustive.enum(),
                        0..NWTrucksTransmission.values().size
                    )) { transmission: Set<NWTrucksTransmission> ->
                        val trucksRequest = NWTrucksSearchRequestParameters(transmission = transmission.toNonEmptyList())
                        val backConverted = convertBackAndForth(trucksRequest)
                        backConverted.transmission shouldBeSameSetAs trucksRequest.transmission
                    }
                }
                then("`engine_type` fields should be the same") {
                    checkAll(Arb.set(Exhaustive.enum(), 0..NWEngineType.values().size)) { engine: Set<NWEngineType> ->
                        val trucksRequest = NWTrucksSearchRequestParameters(engine_type = engine.toNonEmptyList())
                        val backConverted = convertBackAndForth(trucksRequest)
                        backConverted.engine_type shouldBeSameSetAs trucksRequest.engine_type
                    }
                }
                then("`gear_type` fields should be the same") {
                    checkAll(Arb.set(Exhaustive.enum(), 0..NWTruckGearType.values().size)) { gearType: Set<NWTruckGearType> ->
                        val trucksRequest = NWTrucksSearchRequestParameters(gear_type = gearType.toNonEmptyList())
                        val backConverted = convertBackAndForth(trucksRequest)
                        backConverted.gear_type shouldBeSameSetAs trucksRequest.gear_type
                    }
                }
                then("`wheel_drive` fields should be the same") {
                    checkAll(Arb.set(Exhaustive.enum(), 0..NWWheelDrive.values().size)) { wheelDrive: Set<NWWheelDrive> ->
                        val trucksRequest = NWTrucksSearchRequestParameters(wheel_drive = wheelDrive.toNonEmptyList())
                        val backConverted = convertBackAndForth(trucksRequest, subcategory = TRUCK_SUB_CATEGORY_OLD_ID)
                        backConverted.wheel_drive shouldBeSameSetAs trucksRequest.wheel_drive
                    }
                }
                then("`steering_wheel` fields should be the same") {
                    checkAll(Exhaustive.enum<NWSteeringWheel>().toArb().orNull()) { steeringWheel: NWSteeringWheel? ->
                        val trucksRequest = NWTrucksSearchRequestParameters(steering_wheel = steeringWheel)
                        val backConverted = convertBackAndForth(trucksRequest)
                        backConverted.steering_wheel shouldBe trucksRequest.steering_wheel
                    }
                }
                then("`trucks_category` fields should be the same") {
                    checkAll(Exhaustive.enum()) { truckCategory: NWTruckCategory ->
                        val trucksRequest = NWTrucksSearchRequestParameters(trucks_category = truckCategory)
                        val backConverted = convertBackAndForth(trucksRequest, CRANE_HYDRAULICS_SUB_CATEGORY_OLD_ID)
                        backConverted.trucks_category shouldBe trucksRequest.trucks_category
                    }
                }
                then("`light_truck_type` fields should be the same") {
                    checkAll(Arb.set(Exhaustive.enum(), 0..NWLightTruckType.values().size)) { lightTruck: Set<NWLightTruckType> ->
                        val trucksRequest = NWTrucksSearchRequestParameters(light_truck_type = lightTruck.toNonEmptyList())
                        val backConverted = convertBackAndForth(trucksRequest)
                        backConverted.light_truck_type shouldBeSameSetAs trucksRequest.light_truck_type
                    }
                }
                then("`bus_type` fields should be the same") {
                    checkAll(Arb.set(Exhaustive.enum(), 0..NWBusType.values().size)) { busType: Set<NWBusType> ->
                        val trucksRequest = NWTrucksSearchRequestParameters(bus_type = busType.toNonEmptyList())
                        val backConverted = convertBackAndForth(trucksRequest, subcategory = BUS_SUB_CATEGORY_OLD_ID)
                        backConverted.bus_type shouldBeSameSetAs trucksRequest.bus_type
                    }
                }
                then("`seats_from` and `seats_to` fields should be the same") {
                    checkAll(orderedPairOfNatsOrNulls()) { (seatsFrom, seatsTo) ->
                        val searchRequest = NWTrucksSearchRequestParameters(seats_from = seatsFrom, seats_to = seatsTo)
                        val backConverted = convertBackAndForth(searchRequest)
                        backConverted.seats_from shouldBe searchRequest.seats_from
                        backConverted.seats_to shouldBe searchRequest.seats_to
                    }
                }
                then("`truck_type` fields should be the same") {
                    checkAll(Arb.set(Exhaustive.enum(), 0..NWTruckType.values().size)) { truckType: Set<NWTruckType> ->
                        val trucksRequest = NWTrucksSearchRequestParameters(
                            trucks_category = NWTruckCategory.TRUCK,
                            truck_type = truckType.toNonEmptyList()
                        )
                        val backConverted = convertBackAndForth(trucksRequest)
                        backConverted.truck_type shouldBeSameSetAs trucksRequest.truck_type
                    }
                }
                then("`euro_class` fields should be the same") {
                    checkAll(exhaustive(euroClasses).toArb().orNull()) { euro: List<NWEuroClassType>? ->
                        val trucksRequest = NWTrucksSearchRequestParameters(euro_class = euro)
                        val backConverted = convertBackAndForth(trucksRequest, subcategory = TRUCK_SUB_CATEGORY_OLD_ID)
                        backConverted.euro_class shouldBeSameSetAs trucksRequest.euro_class
                    }
                }
                then("`cabin_key` fields should be the same") {
                    checkAll(Arb.set(Exhaustive.enum(), 0..NWCabinType.values().size)) { cabin: Set<NWCabinType> ->
                        val trucksRequest = NWTrucksSearchRequestParameters(cabin_key = cabin.toNonEmptyList())
                        val backConverted = convertBackAndForth(trucksRequest, subcategory = TRUCK_SUB_CATEGORY_OLD_ID)
                        backConverted.cabin_key shouldBeSameSetAs trucksRequest.cabin_key
                    }
                }
                then("`suspension_chassis` fields should be the same") {
                    checkAll(Arb.set(
                        Exhaustive.enum(),
                        0..NWSuspensionChassisType.values().size
                    )) { suspension: Set<NWSuspensionChassisType> ->
                        val trucksRequest = NWTrucksSearchRequestParameters(suspension_chassis = suspension.toNonEmptyList())
                        val backConverted = convertBackAndForth(trucksRequest, subcategory = TRUCK_SUB_CATEGORY_OLD_ID)
                        backConverted.suspension_chassis shouldBeSameSetAs trucksRequest.suspension_chassis
                    }
                }
                then("`suspension_cabin` fields should be the same") {
                    checkAll(Arb.set(
                        Exhaustive.enum(),
                        0..NWSuspensionCabinType.values().size
                    )) { suspension: Set<NWSuspensionCabinType> ->
                        val trucksRequest = NWTrucksSearchRequestParameters(suspension_cabin = suspension.toNonEmptyList())
                        val backConverted = convertBackAndForth(trucksRequest, subcategory = TRUCK_SUB_CATEGORY_OLD_ID)
                        backConverted.suspension_cabin shouldBeSameSetAs trucksRequest.suspension_cabin
                    }
                }
                then("`suspension_type` fields should be the same") {
                    checkAll(Arb.set(Exhaustive.enum(), 0..NWSuspensionType.values().size)) { suspension: Set<NWSuspensionType> ->
                        val trucksRequest = NWTrucksSearchRequestParameters(suspension_type = suspension.toNonEmptyList())
                        val backConverted = convertBackAndForth(trucksRequest, subcategory = TRAILER_SUB_CATEGORY_OLD_ID)
                        backConverted.suspension_type shouldBeSameSetAs trucksRequest.suspension_type
                    }
                }
                then("`loading_from` and `loading_to` fields should be the same") {
                    checkAll(orderedPairOfNatsOrNulls()) { (loadingFrom, loadingTo) ->
                        val searchRequest = NWTrucksSearchRequestParameters(loading_from = loadingFrom, loading_to = loadingTo)
                        val backConverted = convertBackAndForth(searchRequest, subcategory = TRAILER_SUB_CATEGORY_OLD_ID)
                        //divide by thousand because in formState we keep loading in tonnes
                        // and map loading to killos in RangeField.buildLoadingTonnes
                        val expectedFrom = searchRequest.loading_from?.let { (it / 1000) plusOrMinus 1 }
                        val expectedTo = searchRequest.loading_to?.let { (it / 1000) plusOrMinus 1 }

                        backConverted.loading_from shouldBe expectedFrom
                        backConverted.loading_to shouldBe expectedTo
                    }
                }
                then("`saddle_height` fields should be the same") {
                    checkAll(Arb.set(Exhaustive.enum(), 0..NWSaddleHeight.values().size)) { saddleHeight: Set<NWSaddleHeight> ->
                        val trucksRequest = NWTrucksSearchRequestParameters(saddle_height = saddleHeight.toNonEmptyList())
                        val backConverted = convertBackAndForth(trucksRequest, subcategory = TRUCK_TRACTOR_SUB_CATEGORY_OLD_ID)
                        backConverted.saddle_height shouldBeSameSetAs trucksRequest.saddle_height
                    }
                }
                then("`trailer_type` fields should be the same") {
                    checkAll(Arb.set(Exhaustive.enum(), 0..NWTrailerType.values().size)) { trailer: Set<NWTrailerType> ->
                        val trucksRequest = NWTrucksSearchRequestParameters(trailer_type = trailer.toNonEmptyList())
                        val backConverted = convertBackAndForth(trucksRequest, subcategory = TRAILER_SUB_CATEGORY_OLD_ID)
                        backConverted.trailer_type shouldBeSameSetAs trucksRequest.trailer_type
                    }
                }
                then("`brake_type` fields should be the same") {
                    checkAll(Arb.set(Exhaustive.enum(), 0..NWBrakeType.values().size)) { brake: Set<NWBrakeType> ->
                        val trucksRequest = NWTrucksSearchRequestParameters(brake_type = brake.toNonEmptyList())
                        val backConverted = convertBackAndForth(trucksRequest, subcategory = TRAILER_SUB_CATEGORY_OLD_ID)
                        backConverted.brake_type shouldBeSameSetAs trucksRequest.brake_type
                    }
                }
                then("`axis_from` and `axis_to` fields should be the same") {
                    checkAll(orderedPairOfNatsOrNulls()) { (axisFrom, axisTo) ->
                        val searchRequest = NWTrucksSearchRequestParameters(axis_from = axisFrom, axis_to = axisTo)
                        val backConverted = convertBackAndForth(searchRequest, subcategory = TRAILER_SUB_CATEGORY_OLD_ID)
                        backConverted.axis_from shouldBe searchRequest.axis_from
                        backConverted.axis_to shouldBe searchRequest.axis_to
                    }
                }
            }
        }
    }

    private fun convertBackAndForth(trucksRequest: NWTrucksSearchRequestParameters,
                                    subcategory: String = COMMERCIAL_CATEGORY_OLD_ID): NWTrucksSearchRequestParameters {
        val truckParams: TruckParams = TruckParamsConverter.fromNetwork(trucksRequest)
        val formState: FormState = TrucksSearchRequestToFormStateConverter.convert(truckParams)
        val formParams: List<Pair<String, String>> = FormstateFilterPairsExtractor(formState, subcategory)
        val convertedParams: TruckParams = TruckParamsExtractor.getTruckParams(formParams)
        return TruckParamsConverter.toNetwork(convertedParams)
    }
}
