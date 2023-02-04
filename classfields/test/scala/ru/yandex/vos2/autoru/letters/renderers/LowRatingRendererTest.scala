package ru.yandex.vos2.autoru.letters.renderers

import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport.{mock, when}
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer
import ru.yandex.vos2.AutoruModel.AutoruOffer.Notification.NotificationType
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.services.pushnoy.PushTemplateV1

/**
  * Created by sievmi on 26.01.18
  */
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class LowRatingRendererTest extends AnyFunSuite with InitTestDbs {
  implicit val trace = Traced.empty

  def addLowRatingNotification(builder: Offer.Builder, num: Int, pos: Int = 100): AutoruOffer.Builder = {
    builder.getOfferAutoruBuilder.addNotifications(
      AutoruOffer.Notification
        .newBuilder()
        .setType(NotificationType.LOW_RATING)
        .addExtraArgs(s"$pos")
        .setTimestampCreate(num)
    )
  }

  val isSpamalotFeature: Feature[Boolean] = mock[Feature[Boolean]]
  when(isSpamalotFeature.value).thenReturn(true)
  val renderer = new LowRatingRenderer(isSpamalotFeature)

  test("first push") {
    val offerBuilder = TestUtils.createOffer()
    addLowRatingNotification(offerBuilder, 1)
    val res = renderer.render(offerBuilder.build(), Seq("1"))
    res.push match {
      case push: Option[PushTemplateV1] =>
        assert(push.nonEmpty)
        assert(push.get.androidTitle === Some("📞 Мало звонков по объявлению?"))
        assert(
          push.get.androidBody === s"Ваше объявление 1-е в списке\nПоднимите его в поиске, чтобы покупатели его видели"
        )
        assert(
          push.get.iosBody === s"Ваше объявление 1-е в списке\nПоднимите его в поиске, чтобы покупатели его видели"
        )
    }

  }

  test("second push") {
    val offerBuilder = TestUtils.createOffer()
    addLowRatingNotification(offerBuilder, 1)
    addLowRatingNotification(offerBuilder, 2)
    val res = renderer.render(offerBuilder.build(), Seq("2"))
    res.push match {
      case push: Option[PushTemplateV1] =>
        assert(push.nonEmpty)
        assert(push.get.androidTitle === Some("📞 Мало звонков по объявлению?"))
        assert(
          push.get.androidBody === s"Объявление уже на 2-м месте\nПоднимите его в поиске — звонить будут чаще"
        )
        assert(
          push.get.iosBody === s"Объявление уже на 2-м месте\nПоднимите его в поиске — звонить будут чаще"
        )
    }
  }

  test("third push") {
    val offerBuilder = TestUtils.createOffer()
    addLowRatingNotification(offerBuilder, 1)
    addLowRatingNotification(offerBuilder, 2)
    addLowRatingNotification(offerBuilder, 3)
    val res = renderer.render(offerBuilder.build(), Seq("3"))
    res.push match {
      case push: Option[PushTemplateV1] =>
        assert(push.nonEmpty)
        assert(push.get.androidTitle === Some("📞 Мало звонков по объявлению?"))
        assert(
          push.get.androidBody === s"Ваше объявление 3-е в списке\nПоднимите его в поиске, и покупатели его заметят"
        )
        assert(
          push.get.iosBody === s"Ваше объявление 3-е в списке\nПоднимите его в поиске, и покупатели его заметят"
        )
    }
  }

  test("fourth push") {
    val offerBuilder = TestUtils.createOffer()
    addLowRatingNotification(offerBuilder, 1)
    addLowRatingNotification(offerBuilder, 2)
    addLowRatingNotification(offerBuilder, 3)
    addLowRatingNotification(offerBuilder, 4)
    val res = renderer.render(offerBuilder.build(), Seq("4"))
    res.push match {
      case push: Option[PushTemplateV1] =>
        assert(push.nonEmpty)
        assert(push.get.androidTitle === Some("📞 Мало звонков по объявлению?"))
        assert(
          push.get.androidBody === s"Покупатели редко видят ваше объявление\nВедь оно 4-е в списке :( Поднимите его в поиске"
        )
        assert(
          push.get.iosBody === s"Покупатели редко видят ваше объявление\nВедь оно 4-е в списке :( Поднимите его в поиске"
        )
    }
  }

}
