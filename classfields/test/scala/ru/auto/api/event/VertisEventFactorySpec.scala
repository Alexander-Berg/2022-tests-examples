package ru.auto.api.event

import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.BaseSpec
import ru.auto.api.app.Environment
import ru.auto.api.event.VertisEventFactory._
import ru.auto.api.managers.TestRequest
import ru.auto.api.model.ModelGenerators
import ru.auto.api.services.billing.BankerClient.BankerDomains
import ru.auto.api.testkit.TestEnvironment

import scala.jdk.CollectionConverters._

class VertisEventFactorySpec extends BaseSpec with ScalaCheckPropertyChecks with TestRequest {

  implicit val environment: Environment = TestEnvironment

  private val PhonesGen: Gen[Set[String]] = for {
    phones <- Gen.listOf(Gen.numStr)
  } yield phones.toSet

  private val UserIdGen: Gen[String] = Gen.numStr

  "VertisEventFactory" should {

    "CreateOfferEvent" in {
      forAll(ModelGenerators.OfferGen) { offer =>
        val actual = VertisEventFactory(CreateOfferEvent(offer), None)
        actual.getOfferEvent.getCreate.getOffer.getAuto shouldBe offer
      }
    }

    "UpdateOfferEvent" in {
      forAll(ModelGenerators.OfferGen) { offer =>
        val actual = VertisEventFactory(UpdateOfferEvent(offer), None)
        actual.getOfferEvent.getUpdate.getOffer.getAuto shouldBe offer
      }
    }

    "DeleteOfferEvent" in {
      forAll(ModelGenerators.OfferGen) { offer =>
        val actual = VertisEventFactory(DeleteOfferEvent(offer), None)
        actual.getOfferEvent.getDelete.getOffer.getAuto shouldBe offer
      }
    }

    "CreateUserEvent" in {
      forAll(UserIdGen, PhonesGen) { (userId, phones) =>
        val actual = VertisEventFactory(CreateUserEvent(userId, phones), None)
        val actualUser = actual.getUserEvent.getCreate.getUser
        actualUser.getId shouldBe userId
        actualUser.getPhonesList.asScala.toSet shouldBe phones
      }
    }

    "UpdateUserEvent" in {
      forAll(UserIdGen, PhonesGen) { (userId, phones) =>
        val actual = VertisEventFactory(UpdateUserEvent(userId, phones), None)
        val actualUser = actual.getUserEvent.getUpdate.getUser
        actualUser.getId shouldBe userId
        actualUser.getPhonesList.asScala.toSet shouldBe phones
      }
    }

    "AuthUserEvent" in {
      forAll(UserIdGen, PhonesGen) { (userId, phones) =>
        val actual = VertisEventFactory(AuthUserEvent(userId, phones), None)
        val actualUser = actual.getUserEvent.getAuthorisation.getUser
        actualUser.getId shouldBe userId
        actualUser.getPhonesList.asScala.toSet shouldBe phones
      }
    }

    "DeleteTiedCardUserEvent" in {
      forAll(UserIdGen) { (userId) =>
        val mask = "123456|1234"
        val event = DeleteTiedCardUserEvent(userId, TiedCard(BankerDomains.Autoru, mask))
        val actual = VertisEventFactory(event, None)
        val actualUser = actual.getUserEvent.getDeleteTiedCard.getUser
        actualUser.getId shouldBe userId
        val actualTiedCard = actual.getUserEvent.getDeleteTiedCard.getTiedCard
        actualTiedCard.getBankerDomain shouldBe BankerDomains.Autoru.toString
        actualTiedCard.getMask shouldBe mask
      }
    }

    "ActualTiedCardUserEvent" in {
      forAll(UserIdGen) { (userId) =>
        val mask = "123456|1234"
        val tiedCards = List(TiedCard(BankerDomains.Autoru, mask))
        val event = ActualTiedCardUserEvent(userId, tiedCards)
        val actual = VertisEventFactory(event, None)
        val actualUser = actual.getUserEvent.getActualTiedCard.getUser
        actualUser.getId shouldBe userId
        val actualTiedCard = actual.getUserEvent.getActualTiedCard.getTiedCardsList.asScala
        actualTiedCard.length shouldBe 1
      }
    }
  }
}
