package ru.auto.api.model.moderation

import org.scalatest.Inspectors
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.BaseSpec
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class BanReasonsParserSpec extends BaseSpec with ScalaCheckPropertyChecks with Inspectors {

  "BanReasonsParser" should {
    "make correct parsing" in {
      val json =
        """
          |{
          |  "test_reason": {
          |    "content": {
          |      "text": "text",
          |      "text_app": "text_app",
          |      "title_lk": "title_lk",
          |      "text_app_html": "text_app_html",
          |      "text_lk_dealer": "text_lk_dealer",
          |      "text_lk_dealer_app": "text_lk_dealer_app"
          |    }
          |  }
          |}
        """.stripMargin
      val is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))
      val (key, reason) = BanReasons.parse(is).data.head
      key shouldBe "test_reason"
      reason.getTextAppHtml shouldBe "text_app_html"
      reason.getText shouldBe "text"
      reason.getTextApp shouldBe "text_app"
      reason.getTitle shouldBe "title_lk"
      reason.getTextLkDealer shouldBe "text_lk_dealer"
      reason.getTextLkDealerApp shouldBe "text_lk_dealer_app"
    }
  }

}
