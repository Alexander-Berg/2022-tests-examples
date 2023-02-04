package auto.carfax.common.utils.misc

import org.scalatest.funsuite.AnyFunSuite
import auto.carfax.common.utils.misc.StringUtils._

import java.util.Base64

class StringUtilsTest extends AnyFunSuite {

  test("md5 hash from string") {
    assert("".md5hash() == "d41d8cd98f00b204e9800998ecf8427e")
    assert("1".md5hash() == "c4ca4238a0b923820dcc509a6f75849b")
    // check hash with leading zero
    assert("iwrupvqb346386".md5hash() == "0000045c5e2b3911eb937d9d8c574f09")
  }

  test("encode/decode to base64") {
    assert(
      """{"type":"GRZ","body":"С762УА178"}""".encodeBase64() ==
        "eyJ0eXBlIjoiR1JaIiwiYm9keSI6ItChNzYy0KPQkDE3OCJ9"
    )
    assert(
      "eyJ0eXBlIjoiR1JaIiwiYm9keSI6ItChNzYy0KPQkDE3OCJ9".decodeBase64() ==
        """{"type":"GRZ","body":"С762УА178"}"""
    )
  }

  test("encode hmacSha1") {
    assert(Base64.getEncoder.encodeToString("123456".hmacSHA1("secret")) == "dG5I084YIOGp756FJ/OCasfIP3o=")
  }
}
