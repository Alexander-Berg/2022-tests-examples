package auto.dealers.dealer_pony.model.test

import auto.dealers.dealer_pony.model.{PhoneNumber, WhitelistDeleteRequest}
import io.circe.syntax._
import io.circe.parser._
import zio.test._
import zio.test.Assertion._

object WhitelistDeleteRequestTest extends DefaultRunnableSpec {

  val Right(json) =
    parse("""
      {
        "ownerId" : "0",
        "sourcePhones" : [
          "+79001234567"
        ],
        "domains" : [
          "autoru_def"
        ],
        "allDomainsFlag" : true
      }""")

  val Right(number) = PhoneNumber
    .fromString("+7 (900) 123-45-67")

  val record = WhitelistDeleteRequest(
    ownerId = 0,
    sourcePhones = List(number),
    domains = List("autoru_def"),
    allDomainsFlag = true
  )

  override val spec =
    suite("WhitelistDeleteRequest")(
      test("Properly encoded to json") {
        assert(record.asJson)(equalTo(json))
      }
    )

}
