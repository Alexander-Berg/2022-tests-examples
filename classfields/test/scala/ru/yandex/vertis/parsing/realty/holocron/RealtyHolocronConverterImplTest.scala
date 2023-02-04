package ru.yandex.vertis.parsing.realty.holocron

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.vertis.holocron.common.HoloOffer
import ru.yandex.realty.proto.Area
import ru.yandex.vertis.parsing.CommonModel.Source
import ru.yandex.vertis.parsing.common.Site
import ru.yandex.vertis.parsing.components.executioncontext.SameThreadExecutionContextSupport
import ru.yandex.vertis.parsing.features.SimpleFeatures
import ru.yandex.vertis.parsing.holocron.HolocronConverter
import ru.yandex.vertis.parsing.holocron.validation.{HolocronValidator, HolocronValidatorImpl}
import ru.yandex.vertis.parsing.realty.bunkerconfig.BunkerConfig
import ru.yandex.vertis.parsing.realty.dao.offers.ParsedRealtyRow
import ru.yandex.vertis.parsing.realty.features.ParsingRealtyFeatures
import ru.yandex.vertis.parsing.realty.parsers.CommonRealtyParser
import ru.yandex.vertis.parsing.realty.parsers.smartagent.avito.SmartAgentAvitoRealtyParser
import ru.yandex.vertis.parsing.util.http.tracing.TracedUtils
import ru.yandex.vertis.protobuf.Holocron
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.validation.MessageValidator
import ru.yandex.vertis.validation.model.MissingRequiredField
import ru.yandex.vertis.validation.validators.{CompositeMessageValidator, RequiredMessageValidator, SpecialFieldValidator}

@RunWith(classOf[JUnitRunner])
class RealtyHolocronConverterImplTest extends FunSuite { outer =>

  private val validators = Seq(
    new SpecialFieldValidator(Seq(Holocron.isClassified, Holocron.isSource)),
    new RequiredMessageValidator(Seq())
  )

  private val messageValidator: MessageValidator = new CompositeMessageValidator(validators)

  private val holocronValidator: HolocronValidator = new HolocronValidatorImpl(messageValidator)

  implicit private val trace: Traced = TracedUtils.empty

  test("areas") {
    val holoOfferBuilder = HoloOffer.newBuilder()
    holoOfferBuilder.getRealtyOfferBuilder.setTotalArea(Area.getDefaultInstance)
    holoOfferBuilder.getRealtyOfferBuilder.setKitchenArea(Area.getDefaultInstance)
    holoOfferBuilder.getRealtyOfferBuilder.setLivingArea(Area.getDefaultInstance)
    val holoOffer = holoOfferBuilder.build()
    val validationResult = holocronValidator.validate(Site.Avito, Source.SMART_AGENT_FRESH, holoOffer)
    println(validationResult)
    assert(validationResult.isInvalid)
    val reasons = validationResult.asInvalid.invalidReasons
    assert(reasons.contains(MissingRequiredField("realty.Area.value")))
    assert(reasons.contains(MissingRequiredField("realty.Area.unit")))
  }
}
