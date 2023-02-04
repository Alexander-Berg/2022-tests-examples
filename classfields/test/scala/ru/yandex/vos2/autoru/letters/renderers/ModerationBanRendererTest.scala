package ru.yandex.vos2.autoru.letters.renderers

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, OptionValues}
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport.{mock, when}
import ru.yandex.vertis.moderation.proto.Model.DetailedReason.Details
import ru.yandex.vertis.moderation.proto.Model.DetailedReason.Details.Duplicate
import ru.yandex.vertis.moderation.proto.Model.{DetailedReason, Reason}
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer.DuplicateOfferInfo
import ru.yandex.vos2.OfferModel.OfferFlag
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.letters.ModerationBanDuplicate
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.autoru.model.extdata.BanReasons.BanDescription

import scala.jdk.CollectionConverters._

/**
  * Created by andrey on 8/29/17.
  */
@RunWith(classOf[JUnitRunner])
class ModerationBanRendererTest extends AnyFunSuite with InitTestDbs with OptionValues with BeforeAndAfter {
  initDbs()
  implicit val trace = Traced.empty

  val banReasons = components.banReasons
  val isSpamalotFeature: Feature[Boolean] = mock[Feature[Boolean]]
  when(isSpamalotFeature.value).thenReturn(true)

  val moderationBanRenderer = new ModerationBanRenderer(
    components.carsCatalog,
    components.trucksCatalog,
    components.motoCatalog,
    banReasons,
    components.featuresManager.DuplicateBanNotification,
    isSpamalotFeature
  )

  val testOffer = TestUtils.createOffer().setOfferID("123-abc").build()
  components.offerVosDao.saveMigrated(Seq(testOffer))(Traced.empty)

  before {
    components.featureRegistry.updateFeature(components.featuresManager.DuplicateBanNotification.name, true)
  }

  after {
    components.featureRegistry.updateFeature(components.featuresManager.DuplicateBanNotification.name, false)
  }

  test("handle reasons from notification") {
    val offerBuilder = TestUtils.createOffer()
    offerBuilder.addReasonsBan("wrong_mark").addReasonsBan("wrong_model")
    val extraArgs = Seq("wrong_mark", "wrong_model")
    val moderationBan = moderationBanRenderer.render(offerBuilder.build(), extraArgs)
    val banReasonDescription = (moderationBan.mail.value.payload \ "ban_reason_description").as[String]
    val banMap: Map[Reason, BanDescription] = banReasons.getByKeys(offerBuilder.getReasonsBanList.asScala.toSeq)
    val wrongMark = banMap(Reason.WRONG_MARK)
    val wrongModel = banMap(Reason.WRONG_MODEL)
    assert(banReasonDescription.contains(wrongMark.title))
    assert(banReasonDescription.contains(wrongMark.text))
    assert(banReasonDescription.contains(wrongModel.title))
    assert(banReasonDescription.contains(wrongModel.text))
  }

  test("do_not_exist is the only reason") {
    val offerBuilder = TestUtils.createOffer()
    offerBuilder.addReasonsBan("do_not_exist").addReasonsBan("wrong_model")
    val moderationBan = moderationBanRenderer.render(offerBuilder.build(), Seq("do_not_exist", "wrong_model"))
    val banReasonDescription = (moderationBan.mail.value.payload \ "ban_reason_description").as[String]
    val banMap: Map[Reason, BanDescription] = banReasons.getByKeys(offerBuilder.getReasonsBanList.asScala.toSeq)
    val doNotExist = banMap(Reason.DO_NOT_EXIST)
    val wrongModel = banMap(Reason.WRONG_MODEL)
    assert(banReasonDescription.contains(doNotExist.text))
    assert(!banReasonDescription.contains(wrongModel.text))
  }

  //scalastyle:off line.size.limit

  test("three reasons") {
    val b = TestUtils.createOffer()
    b.getOfferAutoruBuilder.getCarInfoBuilder.setMark("AUDI").setModel("A7").setTechParamId(6457128)
    b.addReasonsBan(Reason.LOW_PRICE.name().toLowerCase())
      .addReasonsBan(Reason.PHONE_IN_DESC.name().toLowerCase())
      .addReasonsBan(Reason.WRONG_AUTO_RULES.name().toLowerCase)
    val moderationBan = moderationBanRenderer.render(b.build(), Seq("low_price", "phone_in_desc", "wrong_auto_rules"))
    assert(
      moderationBan.sms.value.smsText == "Мы проверили ваше объявление Audi A7, нашли ошибки и удалили его. Подробности — в личном кабинете."
    )
    assert(moderationBan.push.isEmpty)
    assert(moderationBan.mail.value.name == "10636")
    assert((moderationBan.mail.value.payload \ "sale_id").as[String] == b.getOfferID)
    assert(
      (moderationBan.mail.value.payload \ "blocked_offer_link")
        .as[String] == s"https://auto.ru/cars/used/sale/${b.getOfferID}"
    )
    assert(
      (moderationBan.mail.value.payload \ "ban_reason_description")
        .as[String] == "<p><strong>Заниженная цена</strong> Стоимость автомобиля / мотоцикла коммерческого транспорта или спецтехники, указанная в объявлении, ниже той цены, за которую вы фактически его продаете. Пожалуйста, определитесь с точной суммой, без учёта возможных скидок и проверьте выбранную валюту. Важно: цену надо прописывать не в тексте объявления и не в дополнительной информации, а именно в графе «Цена».</p> <p><strong>Телефон в объявлении</strong> Проверьте, чтобы номер вашего телефона стоял в нужной графе анкеты — см. пункт «Личные данные и место осмотра». Дублировать контактную информацию в других строчках или давать чужие телефоны для связи в тексте самого объявления не стоит.</p> <p><strong>Пробег, владельцы, цвет</strong> Проверьте, чтобы все факты в вашей анкете были точными и соответствовали действительности. В частности, обратите внимание на пункты: пробег, количество владельцев, цвет автомобиля / мотоцикла. И помните, что дублировать эту информацию в тексте самого объявления не стоит.</p>"
    )
    assert(
      (moderationBan.mail.value.payload \ "rules")
        .as[String] == "<a href='https://auto.ru/pages/terms_of_service/#rule-10.5.1'>10.5.1</a>, <a href='https://auto.ru/pages/terms_of_service/#rule-10.6.1'>10.6.1</a>, <a href='https://auto.ru/pages/terms_of_service/#rule-10.7.2'>10.7.2</a>, <a href='https://auto.ru/pages/terms_of_service/#rule-11.12'>11.12</a>"
    )
    assert((moderationBan.mail.value.payload \ "mark_model").as[String] == "Audi A7")
    assert(moderationBan.chatSupport.isEmpty)
  }

  test("private message text on ban") {
    val b = TestUtils.createOffer()
    b.getOfferAutoruBuilder.getCarInfoBuilder.setMark("AUDI").setModel("A7").setTechParamId(6457128)
    b.addReasonsBan(Reason.DO_NOT_EXIST.name().toLowerCase())
    b.addFlag(OfferFlag.OF_BANNED)
    val moderationBan = moderationBanRenderer.render(b.build(), Seq("do_not_exist"))
    assert(moderationBan.chatSupport.isEmpty)
  }

  test("private message text on recall") {
    val b = TestUtils.createOffer()
    b.getOfferAutoruBuilder.getCarInfoBuilder.setMark("AUDI").setModel("A7").setTechParamId(6457128)
    b.addReasonsBan(Reason.LOW_PRICE.name().toLowerCase())
      .addReasonsBan(Reason.PHONE_IN_DESC.name().toLowerCase())
      .addReasonsBan(Reason.WRONG_AUTO_RULES.name().toLowerCase)
    b.addFlag(OfferFlag.OF_BANNED)
    val moderationBan = moderationBanRenderer.render(b.build(), Seq("low_price", "phone_in_desc", "wrong_auto_rules"))

    assert(moderationBan.chatSupport.isEmpty)
  }

  test("duplicate ban reason") {
    val offerBuilder = TestUtils.createOffer()
    offerBuilder.setOfferID("123-abc")
    offerBuilder.addReasonsBan("duplicate").addReasonsBan("duplicate")
    offerBuilder.addDetailedBanReasons(
      DetailedReason
        .newBuilder()
        .setReason(Reason.DUPLICATE)
        .setDetails(Details.newBuilder().setDuplicate(Duplicate.newBuilder().setOriginalId("456-def")))
    )

    offerBuilder.getOfferAutoruBuilder.setDuplicateOfferInfo(
      DuplicateOfferInfo
        .newBuilder()
        .setOfferId("456-def")
        .setCategory(Category.CARS)
        .setSection(Section.USED)
    )

    val moderationBan = moderationBanRenderer
      .render(offerBuilder.build(), Seq("duplicate"))
      .asInstanceOf[ModerationBanDuplicate]

    assert(moderationBan.mail.get.name === "moderation_block_ad_duplicate")
    assert(moderationBan.offerId === "123-abc")
    assert(moderationBan.originalOfferLink === "https://auto.ru/cars/used/sale/456-def")
  }
}
