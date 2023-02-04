package amogus.webhooks

import io.circe.Json
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class FormDataToJsonSpec extends AnyWordSpecLike with Matchers {

  "FormDataToJson" should {
    "Convert data properly with html unescaping" in {
      val formData = Map("field" -> " &quot; & &lt; ; &gt; \" &amp;")

      val extractedJson = Json.fromFields(
        Seq("field" -> Json.fromString(" \" & < ; > \" &"))
      )

      FormDataToJson.convert(formData) shouldBe extractedJson
    }
  }
}
