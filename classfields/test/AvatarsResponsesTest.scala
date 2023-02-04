package auto.carfax.common.clients.avatars

import auto.carfax.common.clients.avatars.AvatarsImageInfoResponse
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json

import scala.io.Source

class AvatarsResponsesTest extends AnyFunSuite with Matchers {

  test("deserialization") {
    val jsValue = Json.parse(getClass.getResourceAsStream("/avatarsResponses.json"))
    assert(jsValue.validate[AvatarsImageInfoResponse].isSuccess)
  }
}
