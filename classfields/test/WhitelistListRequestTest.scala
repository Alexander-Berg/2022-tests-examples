package auto.dealers.dealer_pony.model.test

import auto.dealers.dealer_pony.model.WhitelistListRequest
import auto.dealers.dealer_pony.model.WhitelistListRequest.Page
import io.circe.syntax._
import io.circe.parser._
import zio.test._
import zio.test.Assertion._

object WhitelistListRequestTest extends DefaultRunnableSpec {

  val Right(json) =
    parse("""
      {
        "ownerId" : "0",
        "domains" : [
          "autoru_def"
        ],
        "slice" : {
          "num" : 0,
          "size" : 1000
        }
      }""")

  val record = WhitelistListRequest(
    ownerId = 0,
    domains = List("autoru_def"),
    slice = Some(Page(0, 1000))
  )

  override val spec =
    suite("WhitelistListRequest")(
      test("Properly encoded to json") {
        assert(record.asJson)(equalTo(json))
      }
    )

}
