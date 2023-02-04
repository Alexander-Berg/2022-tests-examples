package ru.auto.ara.network.api.converter.search.request

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.arbitrary.set
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.enum
import ru.auto.ara.data.models.FormState
import ru.auto.ara.filter.mapper.CarParamsExtractor
import ru.auto.ara.ui.helpers.form.util.CarsSearchRequestToFormStateConverter
import ru.auto.ara.util.shouldBeSameSetAs
import ru.auto.ara.util.toNonEmptyList
import ru.auto.data.model.filter.CarParams
import ru.auto.data.model.network.common.NWTransmission
import ru.auto.data.model.network.scala.catalog.NWSteeringWheel
import ru.auto.data.model.network.scala.common.NWCarGearType
import ru.auto.data.model.network.scala.search.NWBodyTypeGroup
import ru.auto.data.model.network.scala.search.NWCarsSearchRequestParameters
import ru.auto.data.model.network.scala.search.NWEngineGroup
import ru.auto.data.model.network.scala.search.converter.CarParamsConverter
import ru.auto.data.util.AUTO_CATEGORY_OLD_ID

class CarsSearchRequestConverterTest : BehaviorSpec() {
    init {
        given("CarsSearchRequestToFormStateConverter and PublicApiAutoRequestExtractor") {
            `when`("they convert CarsSearchRequestParameters to FormState and back") {
                then("`transmission` fields should be the same") {
                    checkAll(Arb.set(Exhaustive.enum(), 0..NWTransmission.values().size)) { transmission: Set<NWTransmission> ->
                        val carsRequest = NWCarsSearchRequestParameters(
                            transmission = transmission.filter { it != NWTransmission.AUTO }.toNonEmptyList()
                        )
                        val backConverted = convertBackAndForth(carsRequest)
                        backConverted.transmission shouldBeSameSetAs carsRequest.transmission
                    }
                }
                then("`engine_group` fields should be the same") {
                    checkAll(Arb.set(Exhaustive.enum(), 0..NWEngineGroup.values().size))  { engine: Set<NWEngineGroup> ->
                        val carsRequest = NWCarsSearchRequestParameters(engine_group = engine.toNonEmptyList())
                        val backConverted = convertBackAndForth(carsRequest)
                        backConverted.engine_group shouldBeSameSetAs carsRequest.engine_group
                    }
                }
                then("`gear_type` fields should be the same") {
                    checkAll(Arb.set(Exhaustive.enum(), 0..NWCarGearType.values().size))  { gearType: Set<NWCarGearType> ->
                        val carsRequest = NWCarsSearchRequestParameters(gear_type = gearType.toNonEmptyList())
                        val backConverted = convertBackAndForth(carsRequest)
                        backConverted.gear_type shouldBeSameSetAs carsRequest.gear_type
                    }
                }
                then("`steering_wheel` fields should be the same") {
                    checkAll(Exhaustive.enum()) { steeringWheel: NWSteeringWheel ->
                        val carsRequest = NWCarsSearchRequestParameters(steering_wheel = steeringWheel)
                        val backConverted = convertBackAndForth(carsRequest)
                        backConverted.steering_wheel shouldBe carsRequest.steering_wheel
                    }
                }
                then("`body_type_group` fields should be the same") {
                    checkAll(Arb.set(Exhaustive.enum(), 0..NWBodyTypeGroup.values().size))  { bodyType: Set<NWBodyTypeGroup> ->
                        val carsRequest = NWCarsSearchRequestParameters(
                            body_type_group = bodyType.filterNot { it == NWBodyTypeGroup.ANY_BODY }.toNonEmptyList()
                        )
                        val backConverted = convertBackAndForth(carsRequest)
                        backConverted.body_type_group shouldBeSameSetAs carsRequest.body_type_group
                    }
                }
            }
        }
    }

    private fun convertBackAndForth(carsRequest: NWCarsSearchRequestParameters): NWCarsSearchRequestParameters {
        val carParams: CarParams = CarParamsConverter.fromNetwork(carsRequest)
        val formState: FormState = CarsSearchRequestToFormStateConverter.convert(carParams)
        val formParams: List<Pair<String, String>> = FormstateFilterPairsExtractor.invoke(formState, AUTO_CATEGORY_OLD_ID)
        val convertedParams: CarParams = CarParamsExtractor.getCarParams(formParams)
        return CarParamsConverter.toNetwork(convertedParams)
    }
}
