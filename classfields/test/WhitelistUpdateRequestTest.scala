package auto.dealers.dealer_pony.model.test

import auto.dealers.dealer_pony.model.{PhoneNumber, WhitelistUpdateRequest}
import auto.dealers.dealer_pony.model.WhitelistUpdateRequest.Antifraud
import scala.concurrent.duration._
import io.circe.syntax._
import io.circe.parser._
import zio.test._
import zio.test.Assertion._

object WhitelistUpdateRequestTest extends DefaultRunnableSpec {

  val Right(json) =
    parse("""{
           "ownerId": "0",
           "sourcePhones": [
             "+79001234567"
           ],
           "domains": [
             "autoru_def"
           ],
           "allDomainsFlag": true,
           "ttl": 3000,
           "antifraud": "enable",
           "comment": "whitelist"
         }""")

  val Right(number) = PhoneNumber
    .fromString("+7 (900) 123-45-67")

  val record = WhitelistUpdateRequest(
    ownerId = 0,
    sourcePhones = List(number),
    domains = List("autoru_def"),
    allDomainsFlag = true,
    ttl = 3000.seconds,
    antifraud = Antifraud.Enable,
    comment = Some("whitelist")
  )

  override val spec =
    suite("WhitelistUpdateRequest")(
      test("Properly encoded to json") {
        assert(record.asJson)(equalTo(json))
      }
    )

}
