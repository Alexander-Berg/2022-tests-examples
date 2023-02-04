package ru.yandex.realty.context.v2.palma

import scala.collection.JavaConverters._
import org.scalatest.{FlatSpec, Matchers}
import realty.palma.PushTemplateOuterClass.PushTemplate
import ru.yandex.realty.context.v2.palma.PushTemplatesProvider.prepareTemplates

class PushTemplatesProviderSpec extends FlatSpec with Matchers {

  behavior of "prepareTemplates"

  private def newTemplateBuilder() = {
    PushTemplate
      .newBuilder()
      .setPushId("FIRST_RENT_PAYMENT")
      .setIosMinVersion("1.0.0")
      .setAndroidMinVersion("1.0.0")
      .setMetrikaId("TENANT_RENT_FIRST_PAYMENT")
      .setPushInfoId("tenant_rent_first_payment")

  }

  it should "provide working renderables for title, text, url" in {
    val template = prepareTemplates(
      Seq(
        newTemplateBuilder()
          .setTitle("${title}")
          .setText("${text}")
          .setDeeplink("${url}")
          .build()
      )
    ).head
    val data = Map(
      "title" -> "TITLE",
      "text" -> "TEXT",
      "url" -> "URL"
    ).asJava
    template.renderTitle(data) shouldBe "TITLE"
    template.renderText(data) shouldBe "TEXT"
    template.renderUrl(data) shouldBe Some("URL")
  }

  it should "support optional urls" in {
    val template = prepareTemplates(
      Seq(
        newTemplateBuilder()
          .setTitle("x")
          .setText("y")
          .setDeeplink("")
          .build()
      )
    ).head
    template.renderUrl("abc") shouldBe None
  }

}
