package auto.carfax.common.clients.cv.model

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import auto.carfax.common.clients.cv.model.DetectObjectsResponseModels.{DetectObjectsResponse, DetectedObject}
import auto.carfax.common.utils.misc.ResourceUtils

class DetectObjectsResponseSpec extends AnyWordSpec with Matchers {

  val o1 = DetectedObject(7, Crop(0.08997642994, 0.2306687534, 0.5907683969, 0.7077374458))
  val o2 = DetectedObject(10, Crop(0.6263181567, 0.1430835426, 0.8172876239, 0.8941915035))
  val o3 = DetectedObject(10, Crop(0.740367651, 0.8505629897, 0.8028669357, 0.9290814996))

  "DetectObjectsResponse" should {

    "be correctly parsed from cv response" in {
      val response: String = ResourceUtils.getStringFromResources("/detect_objects_response.json")
      val res = Json.parse(response).as[DetectObjectsResponse]

      res.objects.length shouldBe 3
      res.objects should contain(o1)
      res.objects should contain(o2)
      res.objects should contain(o3)
    }
  }
}
