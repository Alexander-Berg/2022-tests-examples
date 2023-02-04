package ru.yandex.vertis.vsquality.callgate.converters

import io.circe.CursorOp.DownField
import io.circe.DecodingFailure
import io.circe.parser._
import ru.yandex.vertis.vsquality.callgate.converters.CleanWebFormat._
import ru.yandex.vertis.vsquality.callgate.model.cleanweb.CleanWebVerdictView
import ru.yandex.vertis.vsquality.utils.test_utils.SpecBase

class CleanWebFormatSpec extends SpecBase {

  "CleanWebFormat" should {
    "correctly decode CleanWebVerdictView for boolean verdict" in {
      val json =
        """{
          |  "entity": "header",
          |  "key": "082sometask",
          |  "name": "text_toloka_rude",
          |  "source": "clean-web",
          |  "subsource": "custom",
          |  "value": true
          |}""".stripMargin
      val expected = CleanWebVerdictView("082sometask", "text_toloka_rude", true, None, None)
      decode[CleanWebVerdictView](json) shouldBe Right(expected)
    }

    "correctly decode CleanWebVerdictView for probability verdict" in {
      val probs = List(1, 0.05, 0, -1, -1.0)
      probs.foreach { probability =>
        val json =
          s"""{
            |  "entity": "image",
            |  "key": "082sometask",
            |  "name": "media_auto_porn_probability",
            |  "source": "clean-web",
            |  "subsource": "cbir",
            |  "value": $probability,
            |  "data": {
            |    "url": "some_url"
            |  }
            |}""".stripMargin
        val expected =
          CleanWebVerdictView("082sometask", "media_auto_porn_probability", true, Some(probability), Some("some_url"))
        decode[CleanWebVerdictView](json) shouldBe Right(expected)
      }
    }

    "fail in case of unexpected value for probability verdict" in {
      val json =
        """{
          |  "entity": "image",
          |  "key": "082sometask",
          |  "name": "media_auto_porn_probability",
          |  "source": "clean-web",
          |  "subsource": "cbir",
          |  "value": true
          |}""".stripMargin
      decode[CleanWebVerdictView](json) shouldBe Left(DecodingFailure("Double", List(DownField("value"))))
    }
  }
}
