package ru.auto.api.model

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 09.02.17
  */
class UserRefTest extends AnyFunSuite with ScalaCheckPropertyChecks {
  test("parse user ref") {
    UserRef.parse("user:123") shouldBe UserRef.user(123L)
  }

  test("parse dealer ref") {
    UserRef.parse("dealer:554") shouldBe UserRef.dealer(554L)
  }

  test("parsing of invalid red") {
    intercept[IllegalArgumentException] {
      UserRef.parse("aab:55")
    }
  }

  test("distinct users from dealers") {
    UserRef.user(123L) should not be UserRef.dealer(123L)
  }

  test("equals") {
    UserRef.user(123L) shouldBe UserRef.user(123L)
    UserRef.dealer(123L) shouldBe UserRef.dealer(123L)
    UserRef.anon("12345678901234567890123456789012345") shouldBe UserRef.anon("123456789012345678901234567")
  }

  test("isPrivate") {
    forAll(ModelGenerators.PrivateUserRefGen)(_.isPrivate shouldBe true)
    forAll(ModelGenerators.DealerUserRefGen)(_.isPrivate shouldBe false)
  }

  test("isDealer") {
    forAll(ModelGenerators.PrivateUserRefGen)(_.isDealer shouldBe false)
    forAll(ModelGenerators.DealerUserRefGen)(_.isDealer shouldBe true)
  }

  test("correct toString") {
    UserRef.parse("user:554").toString shouldBe "user:554"
    UserRef.parse("dealer:5576").toString shouldBe "dealer:5576"
    forAll(ModelGenerators.RegisteredUserRefGen)(ref => UserRef.parse(ref.toString) shouldBe ref)
  }

  test("pattern match") {
    "user:123" match {
      case UserRef(ref) => ref shouldBe UserRef.user(123L)
      case _ => fail()
    }
    "dealer:554" match {
      case UserRef(ref) => ref shouldBe UserRef.dealer(554L)
      case _ => fail()
    }
    "aab:f-" match {
      case UserRef(_) => fail()
      case _ => succeed
    }
  }

}
