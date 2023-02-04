package ru.yandex.vos2.autoru.letters.renderers

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.OptionValues._
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.mockito.MockitoSupport._
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.OfferModel.OfferService
import ru.yandex.vos2.autoru.catalog.cars.CarsCatalog
import ru.yandex.vos2.autoru.catalog.moto.MotoCatalog
import ru.yandex.vos2.autoru.catalog.trucks.TrucksCatalog
import ru.yandex.vos2.autoru.model.extdata.BannedRevalidationDictionary
import ru.yandex.vos2.model.offer.OfferGenerator
import ru.auto.api.ApiOfferModel
import ru.yandex.vos2.autoru.catalog.cars.model.Mark
import ru.yandex.vos2.autoru.catalog.cars.model.Model

@RunWith(classOf[JUnitRunner])
class BannedRevalidationRendererTest extends AnyFunSuite with Matchers {
  implicit val trace = Traced.empty

  private val carsCatalog = mock[CarsCatalog]
  when(carsCatalog.getMarkByCode("bmw_mark")).thenReturn(Some(Mark("", 0, "", "BMW", 0)))
  when(carsCatalog.getModelByCode("bmw_mark", "x5_model")).thenReturn(Some(Model("", 0, "", "X5")))
  private val trucksCatalog = mock[TrucksCatalog]
  private val motoCatalog = mock[MotoCatalog]

  private val sampleOffer = (for {
    offerBase <- OfferGenerator.offerWithRequiredFields(OfferService.OFFER_AUTO)
    autoru <- OfferGenerator.autoruOfferWithRequiredFields()
  } yield {
    autoru.setCategory(ApiOfferModel.Category.CARS)
    autoru.getCarInfoBuilder().setMark("bmw_mark")
    autoru.getCarInfoBuilder().setModel("x5_model")
    offerBase.setOfferAutoru(autoru).build()
  }).sample.get

  test("Inject templates") {

    val dictionary = BannedRevalidationDictionary(
      textChatCompleteCheckFailed = Some("chat text МАРКА / МОДЕЛЬ"),
      textSmsCompleteCheckFailed = Some("sms text МАРКА / МОДЕЛЬ"),
      senderTemplateCompleteCheckFailed = Some("foo"),
      sendingActive = true
    )

    val renderer = new BannedRevalidationRenderer(carsCatalog, trucksCatalog, motoCatalog, dictionary)

    val result = renderer.render(sampleOffer)
    result.chatSupport.value.text shouldBe "chat text BMW X5"
    result.sms.value.smsText shouldBe "sms text BMW X5"
    result.mail.value.name shouldBe "foo"
  }

  test("Do nothing if sendingActive = false") {
    val dictionary = BannedRevalidationDictionary(
      textChatCompleteCheckFailed = Some("foo"),
      textSmsCompleteCheckFailed = Some("bar"),
      senderTemplateCompleteCheckFailed = Some("baz"),
      sendingActive = false
    )

    val renderer = new BannedRevalidationRenderer(carsCatalog, trucksCatalog, motoCatalog, dictionary)

    val result = renderer.render(sampleOffer)
    result.chatSupport shouldBe None
    result.sms shouldBe None
    result.mail shouldBe None
  }
}
