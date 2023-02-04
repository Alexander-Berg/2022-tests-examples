package ru.auto.api.managers.callback

import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.ApiOfferModel.Offer
import ru.auto.api.ApiOfferModel.Section
import ru.auto.api.BaseSpec
import ru.auto.api.CarsModel.CarInfo
import ru.auto.api.CatalogModel.Mark
import ru.auto.api.CatalogModel.Model
import ru.auto.api.MotoModel.MotoInfo
import ru.auto.api.TrucksModel.TruckInfo
import ru.auto.api.model.bunker.telepony.CallbackWithExtendedInfo
import ru.auto.api.extdata.DataService
import ru.yandex.vertis.mockito.MockitoSupport
import org.scalacheck.Gen
import ru.auto.api.model.ModelGenerators.DealerUserRefGen
import org.scalatest.PrivateMethodTester

class PhoneCallbackMessageBuilderSpec extends BaseSpec with MockitoSupport with PrivateMethodTester {

  abstract class Fixture {
    val extendedInfoClientId = DealerUserRefGen.next
    val callbackWithExtendedInfo = new CallbackWithExtendedInfo(Set(extendedInfoClientId))

    val defaultInfoClientId = DealerUserRefGen
    // Нужен идентификатор отличный от предыдущего
      .filter(_ != extendedInfoClientId)
      .next
    val defaultMessage = "Сообщение по умолчанию"
    val mark = Gen.identifier.next
    val model = Gen.identifier.next

    val dataService = mock[DataService]
    when(dataService.callbackWithExtendedInfo).thenReturn(callbackWithExtendedInfo)

    val messageBuilder = new PhoneCallbackMessageBuilder(
      dataService,
      defaultMessage
    )
  }

  private val messageWithExtendedInfoMethod = PrivateMethod[String](Symbol("messageWithExtendedInfo"))

  def messageWithExtendedInfo(offer: Offer, tradeIn: Boolean) =
    PhoneCallbackMessageBuilder.invokePrivate(messageWithExtendedInfoMethod(offer, tradeIn))

  "PhoneCallbackMessageBuilder#buildCallbackMessage" should {
    def offerBase(mark: String) =
      Offer.newBuilder
        .setSection(Section.NEW)
        .setCategory(Category.CARS)
        .setCarInfo(CarInfo.newBuilder().setMarkInfo(Mark.newBuilder().setRuName(mark)))

    "produce an extended message for one of the specified dealers (trade-in)" in new Fixture() {
      val offer = offerBase(mark).setUserRef(extendedInfoClientId.toPlain).build()
      val result = messageBuilder.buildCallbackMessage(offer, tradeIn = true)
      result shouldBe Some(s"Звонок с авто ру по вопросу покупки в трейд-ин новый $mark")
    }

    "produce an extended message for one of the specified dealers (no trade-in)" in new Fixture() {
      val offer = offerBase(mark).setUserRef(extendedInfoClientId.toPlain).build()
      val result = messageBuilder.buildCallbackMessage(offer, tradeIn = false)
      result shouldBe Some(s"Звонок с авто ру по вопросу покупки новый $mark")
    }

    "produce the default message for other dealers (trade-in)" in new Fixture() {
      val offer = offerBase(mark).setUserRef(defaultInfoClientId.toPlain).build()
      val result = messageBuilder.buildCallbackMessage(offer, tradeIn = true)
      result shouldBe Some(defaultMessage)
    }

    "produce None for other dealers (no trade-in)" in new Fixture() {
      val offer = offerBase(mark).setUserRef(defaultInfoClientId.toPlain).build()
      val result = messageBuilder.buildCallbackMessage(offer, tradeIn = false)
      result shouldBe None
    }

    "produce the default message when the dealer cannot be determined (trade-in)" in new Fixture() {
      val offer = offerBase(mark).build()
      val result = messageBuilder.buildCallbackMessage(offer, tradeIn = true)
      result shouldBe Some(defaultMessage)
    }

    "produce None when the dealer cannot be determined (no trade-in)" in new Fixture() {
      val offer = offerBase(mark).build()
      val result = messageBuilder.buildCallbackMessage(offer, tradeIn = false)
      result shouldBe None
    }
  }

  "PhoneCallbackMessageBuilder.messageWithExtendedInfo" should {
    "produce expected result for cars" in new Fixture() {
      val offer = Offer
        .newBuilder()
        .setSection(Section.NEW)
        .setCategory(Category.CARS)
        .setCarInfo(
          CarInfo
            .newBuilder()
            .setMarkInfo(Mark.newBuilder().setRuName(mark))
            .setModelInfo(Model.newBuilder().setRuName(model))
        )
        .setColorHex("000000")
        .build()

      val result = messageWithExtendedInfo(offer, tradeIn = true)
      result shouldBe s"Звонок с авто ру по вопросу покупки в трейд-ин новый $mark $model черный"
    }

    "produce expected result for trucks" in new Fixture() {
      val offer = Offer
        .newBuilder()
        .setSection(Section.USED)
        .setCategory(Category.TRUCKS)
        .setTruckInfo(
          TruckInfo
            .newBuilder()
            .setMarkInfo(Mark.newBuilder().setRuName(mark))
            .setModelInfo(Model.newBuilder().setRuName(model))
        )
        .setColorHex("ffffff")
        .build()

      val result = messageWithExtendedInfo(offer, tradeIn = false)
      result shouldBe s"Звонок с авто ру по вопросу покупки подержанный $mark $model белый"
    }

    "produce expected result for motorcycles" in new Fixture() {
      val offer = Offer
        .newBuilder()
        .setSection(Section.NEW)
        .setCategory(Category.MOTO)
        .setMotoInfo(
          MotoInfo
            .newBuilder()
            .setMarkInfo(Mark.newBuilder().setRuName(mark))
            .setModelInfo(Model.newBuilder().setRuName(model))
        )
        .setColorHex("000000")
        .build()

      val result = messageWithExtendedInfo(offer, tradeIn = false)
      result shouldBe s"Звонок с авто ру по вопросу покупки новый $mark $model черный"
    }

    "handle missing mark and model names (cars)" in new Fixture() {
      val offer = Offer
        .newBuilder()
        .setCategory(Category.CARS)
        .setCarInfo(
          CarInfo
            .newBuilder()
            .setMarkInfo(Mark.newBuilder())
            .setModelInfo(Model.newBuilder())
        )
        .build()

      val result = messageWithExtendedInfo(offer, tradeIn = false)
      result shouldBe "Звонок с авто ру по вопросу покупки"
    }

    "handle missing mark and model (cars)" in new Fixture() {
      val offer = Offer
        .newBuilder()
        .setCategory(Category.CARS)
        .setCarInfo(
          CarInfo
            .newBuilder()
        )
        .build()

      val result = messageWithExtendedInfo(offer, tradeIn = false)
      result shouldBe "Звонок с авто ру по вопросу покупки"
    }

    "handle missing info (cars)" in new Fixture() {
      val offer = Offer
        .newBuilder()
        .setCategory(Category.CARS)
        .setCarInfo(
          CarInfo
            .newBuilder()
        )
        .build()

      val result = messageWithExtendedInfo(offer, tradeIn = false)
      result shouldBe "Звонок с авто ру по вопросу покупки"
    }

    "handle missing mark and model names (trucks)" in new Fixture() {
      val offer = Offer
        .newBuilder()
        .setCategory(Category.TRUCKS)
        .setTruckInfo(
          TruckInfo
            .newBuilder()
            .setMarkInfo(Mark.newBuilder())
            .setModelInfo(Model.newBuilder())
        )
        .build()

      val result = messageWithExtendedInfo(offer, tradeIn = false)
      result shouldBe "Звонок с авто ру по вопросу покупки"
    }

    "handle missing mark and model (trucks)" in new Fixture() {
      val offer = Offer
        .newBuilder()
        .setCategory(Category.TRUCKS)
        .setTruckInfo(
          TruckInfo
            .newBuilder()
        )
        .build()

      val result = messageWithExtendedInfo(offer, tradeIn = false)
      result shouldBe "Звонок с авто ру по вопросу покупки"
    }

    "handle missing info (trucks)" in new Fixture() {
      val offer = Offer
        .newBuilder()
        .setCategory(Category.TRUCKS)
        .setTruckInfo(
          TruckInfo
            .newBuilder()
        )
        .build()

      val result = messageWithExtendedInfo(offer, tradeIn = false)
      result shouldBe "Звонок с авто ру по вопросу покупки"
    }

    "handle missing mark and model names (motorcycles)" in new Fixture() {
      val offer = Offer
        .newBuilder()
        .setCategory(Category.MOTO)
        .setMotoInfo(
          MotoInfo
            .newBuilder()
            .setMarkInfo(Mark.newBuilder())
            .setModelInfo(Model.newBuilder())
        )
        .build()

      val result = messageWithExtendedInfo(offer, tradeIn = false)
      result shouldBe "Звонок с авто ру по вопросу покупки"
    }

    "handle missing mark and model (motorcycles)" in new Fixture() {
      val offer = Offer
        .newBuilder()
        .setCategory(Category.MOTO)
        .setMotoInfo(
          MotoInfo
            .newBuilder()
        )
        .build()

      val result = messageWithExtendedInfo(offer, tradeIn = false)
      result shouldBe "Звонок с авто ру по вопросу покупки"
    }

    "handle missing info (motorcycles)" in new Fixture() {
      val offer = Offer
        .newBuilder()
        .setCategory(Category.MOTO)
        .setMotoInfo(
          MotoInfo
            .newBuilder()
        )
        .build()

      val result = messageWithExtendedInfo(offer, tradeIn = false)
      result shouldBe "Звонок с авто ру по вопросу покупки"
    }
  }
}
