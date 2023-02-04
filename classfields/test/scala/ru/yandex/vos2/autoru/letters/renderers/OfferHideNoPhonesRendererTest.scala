package ru.yandex.vos2.autoru.letters.renderers

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.OptionValues
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.model.TestUtils

/**
  * Created auto.ru
  */
@RunWith(classOf[JUnitRunner])
class OfferHideNoPhonesRendererTest extends AnyFunSuite with InitTestDbs with OptionValues {
  implicit val trace = Traced.empty

  val offerHideNoPhonesRender = new OfferHideNoPhones(
    components.carsCatalog,
    components.trucksCatalog,
    components.motoCatalog
  )

  test("sme text") {
    val b = TestUtils.createOffer()
    b.getOfferAutoruBuilder.getCarInfoBuilder.setMark("AUDI").setModel("A7").setTechParamId(6457128)

    val render = offerHideNoPhonesRender.render(b.build())
    assert(
      render.sms.value.smsText == "Объявление Audi A7 снято: " +
        "номер телефона используется в другой учётной записи"
    )
  }
}
