package auto.dealers.dealer_pony.model.test

import auto.dealers.dealer_pony.model.PhoneNumber
import auto.dealers.dealer_pony.model.WhitelistListResponse
import auto.dealers.dealer_pony.model.WhitelistUpdateRequest.Antifraud
import io.circe.syntax._
import io.circe.parser._
import zio.test._
import zio.test.Assertion._
import java.time.OffsetDateTime

object WhitelistListResponseTest extends DefaultRunnableSpec {

  val Right(json) =
    parse("""
        {
          "source": "+79111234567",
          "domain": "autoru_def",
          "antifraud": "enable",
          "comment": "string",
          "endTime": "2021-05-17T17:16:00.148Z"
        }""")

  val Right(number) = PhoneNumber
    .fromString("+7 (911) 123-45-67")

  val record = WhitelistListResponse(
    source = number,
    domain = "autoru_def",
    antifraud = Antifraud.Enable,
    comment = Some("string"),
    endTime = OffsetDateTime.parse("2021-05-17T17:16:00.148Z")
  )

  override val spec =
    suite("WhitelistListResponse")(
      test("Properly decoded from json") {
        assert(json.as[WhitelistListResponse])(isRight(equalTo(record)))
      }
    )

}
