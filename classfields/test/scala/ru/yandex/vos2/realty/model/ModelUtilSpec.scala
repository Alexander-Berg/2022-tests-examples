package ru.yandex.vos2.realty.model

import org.junit.runner.RunWith
import org.scalacheck.Arbitrary
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.Checkers
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.UserModel.User
import ru.yandex.vos2.model.ModelUtils._
import ru.yandex.vos2.model.user.{UserGenerator, UserStatusHistoryGenerator}
import ru.yandex.vos2.realty.model.offer.{OfferStatusHistoryGenerator, RealtyOfferGenerator}

/**
  * @author roose
  */
@RunWith(classOf[JUnitRunner])
class ModelUtilSpec extends WordSpecLike with Matchers with Checkers {

  implicit val arbOffer = Arbitrary(RealtyOfferGenerator.offerGen())
  implicit val arbUser = Arbitrary(UserGenerator.NewUserGen)

//  implicit override val generatorDrivenConfig =
//    PropertyCheckConfig(minSuccessful = 20)

  "RichOfferBuilder" should {
    "have limited status history size after updateStatusHistory()" in {
      val offer = createOfferWithRichHistory()
      offer.getStatusHistoryCount shouldBe MaxStatusHistorySize
    }

    "remove oldest elements when reached size limit after updateStatusHistory" in {
      val offer = createOfferWithRichHistory()
      val oldest = offer.getStatusHistory(0)
      val second = offer.getStatusHistory(1)
      val latest = offer.getStatusHistory(offer.getStatusHistoryCount - 1)
      offer.updateStatusHistory(OfferStatusHistoryGenerator.OshItemGen.sample.get)
      offer.getStatusHistoryList shouldNot contain(oldest)
      offer.getStatusHistory(0) shouldBe second
      offer.getStatusHistory(offer.getStatusHistoryCount - 2) shouldBe latest
    }

    def createOfferWithRichHistory(): Offer.Builder = {
      val offer = TestUtils.createOffer()
      for (_ <- 1 to MaxStatusHistorySize + 1) {
        offer.updateStatusHistory(OfferStatusHistoryGenerator.OshItemGen.sample.get)
      }
      offer
    }
  }

  "RichUserBuilder" should {
    "have limited status history size after updateStatusHistory()" in {
      val user = createUserWithRichHistory()
      user.getStatusHistoryCount shouldBe MaxStatusHistorySize
    }

    "remove oldest elements when reached size limit after updateStatusHistory" in {
      val user = createUserWithRichHistory()
      val oldest = user.getStatusHistory(0)
      val second = user.getStatusHistory(1)
      val latest = user.getStatusHistory(user.getStatusHistoryCount - 1)
      user.updateStatusHistory(UserStatusHistoryGenerator.UshItemGen.sample.get)
      user.getStatusHistoryList shouldNot contain(oldest)
      user.getStatusHistory(0) shouldBe second
      user.getStatusHistory(user.getStatusHistoryCount - 2) shouldBe latest
    }

    def createUserWithRichHistory(): User.Builder = {
      val user = TestUtils.createUser()
      for (_ <- 1 to MaxStatusHistorySize + 1) {
        user.updateStatusHistory(UserStatusHistoryGenerator.UshItemGen.sample.get)
      }
      user
    }
  }
}
