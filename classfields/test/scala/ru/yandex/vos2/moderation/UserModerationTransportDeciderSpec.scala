package ru.yandex.vos2.moderation

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.vos2.realty.model.TestUtils
import ru.yandex.vos2.realty.services.moderation._

import scala.util.{Failure, Success}

/**
  * Spec for [[UserModerationTransportDecider]]
  *
  * @author potseluev
  */
@RunWith(classOf[JUnitRunner])
class UserModerationTransportDeciderSpec extends WordSpec with Matchers {

  private val decider: UserModerationTransportDecider.Default.type = UserModerationTransportDecider.Default

  "UserModerationDecider" should {

    "return Failure if offer has user without Yandex uid" in {
      val offer = TestUtils.createOffer().build()
      decider(offer) match {
        case Failure(_: IllegalArgumentException) => ()
        case other => fail(s"Failure with IllegalArgumentException is expected here bur was $other")
      }
    }

    "return Verdict.NotChanged if user isn't changed" in {
      val offerBuilder = TestUtils.createOffer()
      val user = offerBuilder.getUserBuilder.setUserRef("uid_123").build()
      val instance = ModerationUserConverter(user).get
      val userHash = decider.hash(instance)
      val offer =
        offerBuilder
          .setUser(user)
          .setHashUserModeration(userHash)
          .build()
      decider(offer) shouldBe Success(NotChanged)
    }

    "return Verdict.ShouldBeSent if offer hasn't user hash" in {
      val offerBuilder = TestUtils.createOffer()
      val user = offerBuilder.getUserBuilder.setUserRef("uid_123").setTimestampAnyUpdate(1586176875000L).build()
      val offer =
        offerBuilder
          .setUser(user)
          .clearHashUserModeration()
          .build()
      decider(offer) match {
        case Success(ShouldBeSent(_, _)) => ()
        case other => fail(s"Unexpected result $other")
      }
    }

    "return Verdict.ShouldBeSent if offer has not actual user hash" in {
      val offerBuilder = TestUtils.createOffer()
      val user = offerBuilder.getUserBuilder.setUserRef("uid_123").build()
      val offer =
        offerBuilder
          .setUser(user)
          .setHashUserModeration("123")
          .build()
      decider(offer) match {
        case Success(ShouldBeSent(_, _)) => ()
        case other => fail(s"Unexpected result $other")
      }
    }
  }
}
