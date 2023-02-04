package ru.yandex.vos2.autoru.letters.renderers

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import play.api.libs.json.JsObject
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer.SourceInfo
import ru.yandex.vos2.AutoruModel.AutoruOffer.SourceInfo.Platform
import ru.yandex.vos2.BasicsModel.Photo
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.getNow
import ru.yandex.vos2.services.passport.PassportClient
import ru.yandex.vos2.services.phone.ToPhoneDelivery

/**
  * Created by amisyura on 17.03.17.
  */
@RunWith(classOf[JUnitRunner])
class OfferCreationRendererTest extends AnyFunSuite with InitTestDbs with MockitoSupport {
  initDbs()
  implicit val trace: Traced = Traced.empty

  val noPhotoThumbUrl = "https://i.auto.ru/all7/img/no-photo-thumb.png"

  val passportClient: PassportClient = mock[PassportClient]

  private def getRenderer = {
    new OfferCreationRenderer(
      components.carsCatalog,
      components.regionTree,
      components.mdsPhotoUtils,
      passportClient,
      components.featuresManager.TokenLoginSmsAfterCallCenter
    )
  }

  test("test") {
    pending
    val offer = getOfferById(1044159039L)

    val result = getRenderer.render(offer)

    val photo = Photo
      .newBuilder()
      .setIsMain(true)
      .setOrder(1)
      .setCreated(getNow)
      .setName("61842-3fd0690659afbee428d03abaa4715ed5")
      .build()

    val imageUrl = "https:" + components.mdsPhotoUtils.getSizes(photo)("thumb_m")
    val offerUrl = "https://auto.ru/cars/used/sale/1044159039-33be8"

    //check mail
    assert(result.mail.nonEmpty)
    assert(result.mail.get.payload.isInstanceOf[JsObject])
    assert((result.mail.get.payload \ "title").as[String] == "Peugeot Partner 2 Рестайлинг")
    assert((result.mail.get.payload \ "year").as[String] == "2012")
    assert((result.mail.get.payload \ "run").as[String] == "60000")
    assert((result.mail.get.payload \ "body_type").as[String] == "Компактвэн")
    assert((result.mail.get.payload \ "engine_type").as[String] == "Дизель")
    assert((result.mail.get.payload \ "engine_power").as[String] == "90")
    assert((result.mail.get.payload \ "transmission").as[String] == "Механическая")
    assert((result.mail.get.payload \ "gear_type").as[String] == "Передний")
    assert((result.mail.get.payload \ "price").as[String] == "1000000000")
    assert((result.mail.get.payload \ "color").as[String] == "Белый")
    assert((result.mail.get.payload \ "doors_count").as[String] == "5")
    assert((result.mail.get.payload \ "poi").as[String] == "Москва")
    assert((result.mail.get.payload \ "url").as[String] == offerUrl)
    assert((result.mail.get.payload \ "image").as[String] == imageUrl)

    //check sms
    val sms = result.sms

    assert(sms.nonEmpty)
    assert(sms.get.smsText == "Управляйте своим объявлением в приложении Авто.ру https://auto.ru/lc")
    assert(sms.get.smsParams match {
      case Some(ToPhoneDelivery("74957778895")) => true
      case _ => false
    })

    //check msg
    assert(result.chatSupport.isEmpty)
  }

  test("send sms when create offer without photos in android") {
    pending
    val offer = getOfferById(1044159039L).toBuilder
    offer.setUserRef("a_652137")
    when(passportClient.createPhotoAddUrl(?)(?)).thenReturn(Some("https://auto.ru/l/test_id"))
    for {
      geoId <- Seq(21930, 21931, 98932, 98933, 98934, 98935, 98936, 98937, 98938, 98939, 98940, 98941, 98942, 98943)
    } {
      offer.getOfferAutoruBuilder.getSellerBuilder.getPlaceBuilder.setGeobaseId(geoId)
      offer.getOfferAutoruBuilder.getSourceInfoBuilder.setPlatform(Platform.IOS)
      val result = getRenderer.render(offer.build())
      //check sms
      val sms = result.sms

      assert(sms.nonEmpty)
      assert(sms.get.smsText == "Загрузите фотографии в своё объявление на Авто.ру — https://auto.ru/l/test_id")
    }
  }

  test("is application") {
    val offer = {
      val builder = getOfferById(1044159039L).toBuilder
      builder.getOfferAutoruBuilder.getSourceInfoBuilder.setPlatform(SourceInfo.Platform.IOS)
      builder.build()
    }

    val result = getRenderer.render(offer)

    val sms = result.sms

    assert(sms.isEmpty)
  }

  test("empty photo") {
    val offer = getOfferById(1044159039L).toBuilder
    offer.getOfferAutoruBuilder
      .clearPhoto()
      .build()

    val result = getRenderer.render(offer.build())

    assert((result.mail.get.payload \ "image").as[String] == noPhotoThumbUrl)
  }

  test("isilon photo") {
    val photo = Photo
      .newBuilder()
      .setIsMain(true)
      .setOrder(1)
      .setCreated(getNow)
      .setName("2a980333")
      .build()

    val offer = getOfferById(1044159039L).toBuilder
    offer.getOfferAutoruBuilder
      .clearPhoto()
      .addPhoto(photo)
      .build()

    val result = getRenderer.render(offer.build())

    assert((result.mail.get.payload \ "image").as[String] == noPhotoThumbUrl)
  }

  test("exp sms") {
    pending
    components.featureRegistry.updateFeature(components.featuresManager.TokenLoginSmsAfterCallCenter.name, true)
    val builder = getOfferById(1044159039L).toBuilder
    builder.getOfferAutoruBuilder.getSourceInfoBuilder.setIsCallcenter(true)
    builder.setUserRef("a_652137")
    when(passportClient.createPhotoAddUrl(?)(?)).thenReturn(Some("https://auto.ru/l/test_id"))
    for {
      geoId <- Seq(21930, 21931, 98932, 98933, 98934, 98935, 98936, 98937, 98938, 98939, 98940, 98941, 98942, 98943)
    } {
      builder.getOfferAutoruBuilder.getSellerBuilder.getPlaceBuilder.setGeobaseId(geoId)
      val result = getRenderer.render(builder.build())

      val sms = result.sms

      assert(sms.nonEmpty)

      assert(sms.get.smsText == "Загрузите фотографии в своё объявление на Авто.ру — https://auto.ru/l/test_id")
    }
    components.featureRegistry.updateFeature(components.featuresManager.TokenLoginSmsAfterCallCenter.name, false)
  }

}
