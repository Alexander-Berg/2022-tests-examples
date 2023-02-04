package auto.carfax.common.clients.cv.model

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.Json
import auto.carfax.common.clients.cv.model.RecognizeMarkModelResponseModels.{Prediction, RecognizeMarkModelResponse}
import auto.carfax.common.utils.misc.ResourceUtils

class RecognizeMarkModelResponseSpec extends AnyWordSpecLike with Matchers {

  val p1 = Prediction(0.6859286427, "FERRARI", "599", None, None)
  val p2 = Prediction(0.1126770675, "PORSCHE", "CAYMAN", Some("II (981)"), None)
  val p3 = Prediction(0.06006165966, "PORSCHE", "BOXSTER", Some("III (981)"), Some("Cabri"))
  val p4 = Prediction(0.06006165966, "TOYOTA", "COROLLA_LEVIN", Some("VI (AE100/AE101)"), None)

  "RecognizeMarkModelResponse" should {

    "be correctly parsed from cv response" in {

      val response: String = ResourceUtils.getStringFromResources("/recognize_mark_model_response.json")
      val res = Json.parse(response).as[RecognizeMarkModelResponse]

      res.predictions.length shouldBe 4
      res.predictions should contain(p1)
      res.predictions should contain(p2)
      res.predictions should contain(p3)
      res.predictions should contain(p4)
    }
  }
}
