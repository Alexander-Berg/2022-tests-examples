package ru.auto.api.managers.garage

import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.BaseSpec
import ru.auto.api.model.{RequestParams, UserRef}
import ru.auto.api.util.{Request, RequestImpl}
import ru.auto.promo_dispenser.PromoActionsPalmaModel.PromoActionView.PlatformString
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import scala.annotation.nowarn
import scala.jdk.CollectionConverters._

@nowarn
class GaragePromoDispenserManagerSpec extends AnyWordSpecLike with MockitoSupport with BaseSpec {

  implicit private val trace: Traced = Traced.empty

  implicit private val request: Request = {
    val r = new RequestImpl
    r.setTrace(trace)
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r.setUser(UserRef.anon("42"))
    r
  }

  "GaragePromoDispenserManagerSpec.convertDisclaimer" should {
    def makePlatformString(defaultOpt: Option[String],
                           iosOpt: Option[String] = None,
                           androidOpt: Option[String] = None,
                           webOpt: Option[String] = None): PlatformString = {
      val platformStringB = PlatformString.newBuilder()
      defaultOpt.foreach(platformStringB.setDefault)
      iosOpt.foreach(platformStringB.setIos)
      androidOpt.foreach(platformStringB.setAndroid)
      webOpt.foreach(platformStringB.setWeb)
      platformStringB.build()
    }

    "hasTemplateData = false where text without links" in {
      val someText = "Какой-то текст без ссылок"
      val templateString = GaragePromoDispenserManager.convertDisclaimer(makePlatformString(Some(someText)))
      templateString.getGeneratedText shouldBe someText
      templateString.hasTemplateData shouldBe false
    }

    "hasTemplateData = true text with link" in {
      val someText = """Какой-то текст с одной ссылкой\u00a0 <a href="http://lol.kek">тут</a>"""
      val someTextWithoutLink = s"""Какой-то текст с одной ссылкой\u00a0 $${link_1}"""
      val templateString = GaragePromoDispenserManager.convertDisclaimer(makePlatformString(Some(someText)))
      templateString.getGeneratedText shouldBe someText
      templateString.hasTemplateData shouldBe true
      templateString.getTemplateData.getText shouldBe someTextWithoutLink
      templateString.getTemplateData.getLinksCount shouldBe 1
      templateString.getTemplateData.getLinksList.asScala.exists(el => el.getText == "тут")
      templateString.getTemplateData.getLinksList.asScala.exists(el => el.getUrl == "http://lol.kek")
    }

    "hasTemplateData = true text with 2 links and more" in {
      val someText =
        """Какой-то <a href = 'http://lol.kek.cheburek'>текст</a> с двумя ссылками <a href="http://lol.kek">тут</a>"""
      val someTextWithoutLink = s"""Какой-то $${link_1} с двумя ссылками $${link_2}"""
      val templateString = GaragePromoDispenserManager.convertDisclaimer(makePlatformString(Some(someText)))
      templateString.getGeneratedText shouldBe someText
      templateString.hasTemplateData shouldBe true
      templateString.getTemplateData.getText shouldBe someTextWithoutLink
      templateString.getTemplateData.getLinksCount shouldBe 2
      templateString.getTemplateData.getLinksList.asScala.exists(el => el.getText == "тут")
      templateString.getTemplateData.getLinksList.asScala.exists(el => el.getUrl == "http://lol.kek")
      templateString.getTemplateData.getLinksList.asScala.exists(el => el.getText == "текст")
      templateString.getTemplateData.getLinksList.asScala.exists(el => el.getUrl == "http://lol.kek.cheburek")
    }

  }
}
