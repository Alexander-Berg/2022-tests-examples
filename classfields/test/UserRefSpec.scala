package ru.yandex.vertis.shark.model

import cats.implicits.catsSyntaxOptionId
import zio.test.Assertion.equalTo
import zio.test.{assert, DefaultRunnableSpec, ZSpec}

object UserRefSpec extends DefaultRunnableSpec {

  private val invalidUserRef: String = "Some WTF"
  private val dealerUserRef: String = "dealer:1234567890"
  private val dealerInvalidUserRef: String = "dealer:WTF"
  private val privateUserRef: String = "user:2345678900"
  private val privateInvalidUserRef: String = "user:WTF"

  private case class TestCase(description: String, source: String, expected: Option[UserRef])

  private val testCases: Seq[TestCase] = Seq(
    TestCase("Invalid userRef", invalidUserRef, None),
    TestCase("Valid dealer userRef", dealerUserRef, UserRef.AutoruDealer(1234567890L).some),
    TestCase("Invalid dealer userRef", dealerInvalidUserRef, None),
    TestCase("Valid private userRef", privateUserRef, UserRef.AutoruUser(2345678900L).some),
    TestCase("Invalid private userRef", privateInvalidUserRef, None)
  )

  override def spec: ZSpec[Environment, Failure] =
    suite("UserRef")(
      testCases.map { item =>
        test(item.description)(assert(UserRef.unapply(item.source))(equalTo(item.expected)))
      }: _*
    )
}
