package ru.yandex.vos2.autoru.letters.renderers

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.OptionValues
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.letters.ModerationUnban
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.model.UserRef
import ru.yandex.vos2.services.phone.ToPhoneDelivery
import ru.yandex.vos2.services.sender.ToEmailDelivery

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class ModerationUnbanRendererTest extends AnyFunSuite with InitTestDbs with OptionValues {
  val banReasons = components.banReasons
  implicit val trace = Traced.empty

  val moderationUnbanRenderer =
    new ModerationUnbanRenderer(components.carsCatalog, components.trucksCatalog, components.motoCatalog, banReasons)

  test("unban notification") {
    val phone = "79291112233"
    val email = "example@example.com"
    val offerBuilder = TestUtils.createOffer()
    offerBuilder.getOfferAutoruBuilder.getCarInfoBuilder
      .setMark("HYUNDAI")
      .setModel("IX35")
      .setSuperGenId(2305474)
      .setConfigurationId(6143425)
      .setTechParamId(6143500)
    offerBuilder.getUserBuilder.getUserContactsBuilder
      .setEmail(email)
      .addPhonesBuilder()
      .setNumber(phone)
    val moderationUnban = moderationUnbanRenderer.render(offerBuilder.build())
    assert(
      moderationUnban == ModerationUnban(
        UserRef.from(offerBuilder.getUserRef),
        mark = "Hyundai",
        model = "ix35",
        smsText = Some("Покупатели снова видят ваше объявление о продаже МАРКА / МОДЕЛЬ на сайте."),
        chatText = Some("Покупатели снова видят ваше объявление о продаже МАРКА / МОДЕЛЬ на сайте."),
        senderTemplate = "moderation.unblock_success",
        paramDelivery = ToEmailDelivery(email),
        paramSms = Some(ToPhoneDelivery(phone))
      )
    )
  }
}
