package ru.auto.ara.network.api.converter.search.request

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.set
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.enum
import io.kotest.property.exhaustive.map
import ru.auto.ara.data.models.FormState
import ru.auto.ara.filter.mapper.MotoParamsExtractor
import ru.auto.ara.ui.helpers.form.util.MotoSearchRequestToFormStateConverter
import ru.auto.ara.util.shouldBeSameSetAs
import ru.auto.ara.util.toNonEmptyList
import ru.auto.data.model.filter.MotoParams
import ru.auto.data.model.network.scala.search.NWAtvType
import ru.auto.data.model.network.scala.search.NWCylinderOrder
import ru.auto.data.model.network.scala.search.NWCylinders
import ru.auto.data.model.network.scala.search.NWMotoCategory
import ru.auto.data.model.network.scala.search.NWMotoEngine
import ru.auto.data.model.network.scala.search.NWMotoGearType
import ru.auto.data.model.network.scala.search.NWMotoSearchRequestParameters
import ru.auto.data.model.network.scala.search.NWMotoTransmission
import ru.auto.data.model.network.scala.search.NWMotoType
import ru.auto.data.model.network.scala.search.NWSnowmobileType
import ru.auto.data.model.network.scala.search.NWStroke
import ru.auto.data.model.network.scala.search.converter.MotoParamsConverter
import ru.auto.data.util.ATVS_SUB_CATEGORY_OLD_ID
import ru.auto.data.util.MOTO_CATEGORY_OLD_ID
import ru.auto.data.util.SNOWMOBILES_SUB_CATEGORY_OLD_ID

class MotoSearchRequestConverterTest : BehaviorSpec() {
    init {

        given("MotoSearchRequestToFormStateConverter and PublicApiMotoRequestExtractor") {
            `when`("they convert MotoSearchRequestParameters to FormState and back") {
                then("`transmission` fields should be the same") {
                    checkAll(Arb.set(
                        Exhaustive.enum(),
                        0..NWMotoTransmission.values().size
                    )) { transmission: Set<NWMotoTransmission> ->
                        val motoRequest = NWMotoSearchRequestParameters(transmission = transmission.toNonEmptyList())
                        val backConverted = convertBackAndForth(motoRequest)
                        backConverted.transmission shouldBeSameSetAs motoRequest.transmission
                    }
                }
                then("`engine_type` fields should be the same") {
                    checkAll(Arb.set(Exhaustive.enum(), 0..NWMotoEngine.values().size)) { engine: Set<NWMotoEngine> ->
                        val motoRequest = NWMotoSearchRequestParameters(engine_type = engine.toNonEmptyList())
                        val backConverted = convertBackAndForth(motoRequest)
                        backConverted.engine_type shouldBeSameSetAs motoRequest.engine_type
                    }
                }
                then("`cylinders` fields should be the same") {
                    checkAll(Arb.set(Exhaustive.enum(), 0..NWCylinders.values().size)) { cylinders: Set<NWCylinders> ->
                        val motoRequest = NWMotoSearchRequestParameters(cylinders = cylinders.toNonEmptyList())
                        val backConverted = convertBackAndForth(motoRequest)
                        backConverted.cylinders shouldBeSameSetAs motoRequest.cylinders
                    }
                }
                then("`cylinders_type` fields should be the same") {
                    checkAll(Arb.set(Exhaustive.enum(), 0..NWCylinderOrder.values().size)) { cylinders: Set<NWCylinderOrder> ->
                        val motoRequest = NWMotoSearchRequestParameters(cylinders_type = cylinders.toNonEmptyList())
                        val backConverted = convertBackAndForth(motoRequest)
                        backConverted.cylinders_type shouldBeSameSetAs motoRequest.cylinders_type
                    }
                }
                then("`gear_type` fields should be the same") {
                    checkAll(Arb.set(Exhaustive.enum(), 0..NWMotoGearType.values().size)) { gearType: Set<NWMotoGearType> ->
                        val motoRequest = NWMotoSearchRequestParameters(gear_type = gearType.toNonEmptyList())
                        val backConverted = convertBackAndForth(motoRequest)
                        backConverted.gear_type shouldBeSameSetAs motoRequest.gear_type
                    }
                }
                then("`moto_category` fields should be the same") {
                    checkAll(Exhaustive.enum()) { category: NWMotoCategory ->
                        val motoRequest = NWMotoSearchRequestParameters(moto_category = category)
                        val backConverted = convertBackAndForth(motoRequest)
                        backConverted.moto_category shouldBe motoRequest.moto_category
                    }
                }
                then("`moto_type` fields should be the same") {
                    checkAll(Arb.set(Exhaustive.enum(), 0..NWMotoType.values().size)) { motoType: Set<NWMotoType> ->
                        val motoRequest = NWMotoSearchRequestParameters(moto_type = motoType.toNonEmptyList())
                        val backConverted = convertBackAndForth(motoRequest)
                        backConverted.moto_type shouldBeSameSetAs motoRequest.moto_type
                    }
                }
                then("`atv_type` fields should be the same") {
                    checkAll(Arb.set(Exhaustive.enum(), 0..NWAtvType.values().size)) { atv: Set<NWAtvType> ->
                        val motoRequest = NWMotoSearchRequestParameters(atv_type = atv.toNonEmptyList())
                        val backConverted = convertBackAndForth(motoRequest, subcategory = ATVS_SUB_CATEGORY_OLD_ID)
                        backConverted.atv_type shouldBeSameSetAs motoRequest.atv_type
                    }
                }
                then("`snowmobile_type` fields should be the same") {
                    checkAll(Arb.set(Exhaustive.enum(), 0..NWSnowmobileType.values().size)) { snowmobile: Set<NWSnowmobileType> ->
                        val motoRequest = NWMotoSearchRequestParameters(snowmobile_type = snowmobile.toNonEmptyList())
                        val backConverted = convertBackAndForth(motoRequest, subcategory = SNOWMOBILES_SUB_CATEGORY_OLD_ID)
                        backConverted.snowmobile_type shouldBeSameSetAs motoRequest.snowmobile_type
                    }
                }
                then("`strokes` fields should be the same") {
                    checkAll(Exhaustive.enum<NWStroke>().map(::listOf).toArb().orNull(), Exhaustive.enum()
                    ) { strokes: List<NWStroke>?, category: NWMotoCategory ->
                        val motoRequest = NWMotoSearchRequestParameters(
                            moto_category = category,
                            strokes = strokes
                        )
                        val backConverted = convertBackAndForth(motoRequest)
                        backConverted.strokes shouldBeSameSetAs motoRequest.strokes
                    }
                }
            }
        }
    }

    private fun convertBackAndForth(motoRequest: NWMotoSearchRequestParameters,
                                    subcategory: String = MOTO_CATEGORY_OLD_ID): NWMotoSearchRequestParameters {
        val motoParams: MotoParams = MotoParamsConverter.fromNetwork(motoRequest)
        val formState: FormState = MotoSearchRequestToFormStateConverter.convert(motoParams)
        val formParams: List<Pair<String, String>> = FormstateFilterPairsExtractor(formState, subcategory)
        val convertedParams: MotoParams = MotoParamsExtractor.getMotoParams(formParams)
        return MotoParamsConverter.toNetwork(convertedParams)
    }
}
