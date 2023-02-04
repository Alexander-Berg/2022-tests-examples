package ru.auto.api.model

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 16.01.17
  */
class OfferIDTest extends AnyFunSuite with ScalaCheckPropertyChecks {
  test("parse offer id") {
    val id = OfferID.parse("123-fff")
    id should have(
      Symbol("id")(123L),
      Symbol("hash")(Some("fff"))
    )
  }

  test("parse offer without hash") {
    val id = OfferID.parse("132")
    id should have(
      Symbol("id")(132L),
      Symbol("hash")(None)
    )
  }

  test("parsing of invalid id") {
    intercept[IllegalArgumentException] {
      OfferID.parse("551f")
    }
  }

  test("correct to string") {
    val id = "123441-bbaf"
    OfferID.parse(id).toString shouldBe id
    OfferID.parse(id).toPlain shouldBe id

    forAll(ModelGenerators.OfferIDGen)(id => OfferID.parse(id.toString) shouldBe id)
  }

  test("error on incorrect hash") {
    an[IllegalArgumentException] shouldBe thrownBy(OfferID.parse("123-g-"))
    an[IllegalArgumentException] shouldBe thrownBy(OfferID.parse("123-/"))
    an[IllegalArgumentException] shouldBe thrownBy(OfferID.parse("123-*"))
    an[IllegalArgumentException] shouldBe thrownBy(OfferID.parse("123-\n"))
  }

  test("pattern match") {
    "12551-fae" match {
      case OfferID(id) =>
        id.hash shouldBe defined
      case _ => fail()
    }

    "5513" match {
      case OfferID(id) =>
        id.hash shouldBe empty
      case _ => fail()
    }

    "551f" match {
      case OfferID(_) => fail()
      case _ => succeed
    }
  }
}
