package ru.yandex.vos2.autoru.letters.renderers

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.OptionValues
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.model.TestUtils

/**
  * Created by sievmi on 20.02.18
  */
@RunWith(classOf[JUnitRunner])
class ModerationUpdateRendererTest extends AnyFunSuite with InitTestDbs with OptionValues {
  implicit val trace: Traced = Traced.empty

  val moderationUpdateRenderer =
    new ModerationUpdateRenderer(
      components.carsCatalog,
      components.trucksCatalog,
      components.motoCatalog,
      components.moderationUpdateFields
    )

  test("test renderer") {
    val offerBuilder = TestUtils.createOffer()
    offerBuilder.getOfferAutoruBuilder.getCarInfoBuilder
      .setMark("HYUNDAI")
      .setModel("IX35")
      .setSuperGenId(2305474)
      .setConfigurationId(6143425)
      .setTechParamId(6143500)

    val moderationUpdate = moderationUpdateRenderer.render(offerBuilder.build(), Seq("mark", "model", "currency"))
    val changedFields = (moderationUpdate.mail.value.payload \ "changed_fields").as[String]
    assert(changedFields === "<ul><li>марку</li><li>модель</li><li>валюту</li></ul>")

    assert(moderationUpdate.mail.value.name === "moderation.edited_by_moderator")
    assert(
      moderationUpdate.sms.value.smsText === "Мы нашли ошибки в вашем объявлении " +
        "о продаже Hyundai ix35 и исправили их. Удачной сделки!"
    )

    assert(
      moderationUpdate.chatSupport.value.text === "Мы исправили марку, модель и валюту в вашем объявлении " +
        "о продаже Hyundai ix35. Удачной сделки!"
    )
  }

  test("moto test renderer") {
    val offerBuilder = TestUtils.createOffer()
    offerBuilder.getOfferAutoruBuilder.getMotoInfoBuilder
      .setMark("SUZUKI")
      .setModel("BANDIT")

    val moderationUpdate = moderationUpdateRenderer.render(offerBuilder.build(), Seq("moto_type", "moto_category"))
    val changedFields = (moderationUpdate.mail.value.payload \ "changed_fields").as[String]

    assert(changedFields === "<ul><li>тип мототехники</li><li>категорию мототехники</li></ul>")
  }

  test("zero fields changed") {
    val offerBuilder = TestUtils.createOffer()

    val moderationUpdate = moderationUpdateRenderer.render(offerBuilder.build(), Seq.empty)
    val changedFields = (moderationUpdate.mail.value.payload \ "changed_fields").as[String]
    assert(changedFields === "")
  }

  test("changed single field") {
    val offerBuilder = TestUtils.createOffer()
    val moderationUpdate = moderationUpdateRenderer.render(offerBuilder.build(), Seq("price_info"))
    val changedFields = (moderationUpdate.mail.value.payload \ "changed_fields").as[String]
    assert(changedFields === "<ul><li>цену</li></ul>")
  }
}
