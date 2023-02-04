package auto.dealers.dealer_pony.model.test

import auto.dealers.dealer_pony.model.PhoneNumber
import zio.test._
import zio.test.Assertion._

object PhoneNumberTest extends DefaultRunnableSpec {

  override def spec =
    suite("PhoneNumber")(
      test("Not a phone number") {
        assert(PhoneNumber.fromString("not a number"))(isLeft)
      },
      test("Malformed number") {
        assert(PhoneNumber.fromString("8 (100) 99-00-11"))(isLeft)
      },
      test("Proper number") {
        assert {
          PhoneNumber
            .fromString("+7 (800) 999-00-11")
            .map(_.number)
        }(isRight(equalTo("+78009990011")))
      }
    )

}
