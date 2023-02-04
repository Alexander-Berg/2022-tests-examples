package ru.yandex.vertis.billing.banker.service

import org.scalacheck.{Gen, ShrinkLowPriority}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.application.environment.Environments
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.billing.banker.service.EnvironmentFilteredMailerSpec.VerifiableMailer
import ru.yandex.vertis.billing.banker.model.gens.EmailGen
import ru.yandex.vertis.billing.banker.util.email.EmailMatcher
import ru.yandex.vertis.billing.banker.mailing.Body.Text
import ru.yandex.vertis.billing.banker.mailing.{Address, Letter, LetterContent, Mailer, Recipient}

import scala.concurrent.Future

class EnvironmentFilteredMailerSpec
  extends AnyWordSpec
  with Matchers
  with AsyncSpecBase
  with ScalaCheckPropertyChecks
  with ShrinkLowPriority {

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 100)

  private def replaceDomain(email: String, domain: String): String = {
    val emailParts = email.split('@')
    val tail = emailParts(1).reverse.takeWhile(_ != '.')
    s"${emailParts(0)}@$domain.$tail"
  }

  private val YandexTeamEmailGen =
    EmailGen.map { email =>
      replaceDomain(email, "yandex-team")
    }

  private val NotYandexTeamEmailGen =
    EmailGen.map { email =>
      val oldDomain = EmailMatcher(email).domainOpt.get
      if (oldDomain == "yandex-team") {
        replaceDomain(email, "lazy-random")
      } else {
        email
      }
    }

  private def emptyLetter(emails: List[String]): Letter = {
    val addresses = emails.map(Address(_))
    Letter(
      LetterContent("empty", Text("empty")),
      Address("from"),
      Recipient(addresses.head, addresses.drop(1): _*)
    )
  }

  "EnvironmentFilteredMailer" should {

    "send to yandex-team domain in testing" in {
      val testingMailer = new VerifiableMailer(Environments.Testing) with EnvironmentFilteredMailer
      var expected = 0
      forAll(Gen.nonEmptyListOf(YandexTeamEmailGen)) { emails =>
        testingMailer.send(emptyLetter(emails)).await
        expected = expected + emails.length
        testingMailer.sended shouldBe expected
      }
    }

    "not send to not yandex-team domain in testing" in {
      val testingMailer = new VerifiableMailer(Environments.Testing) with EnvironmentFilteredMailer
      forAll(Gen.nonEmptyListOf(NotYandexTeamEmailGen)) { emails =>
        testingMailer.send(emptyLetter(emails)).await
        testingMailer.sended shouldBe 0
      }
    }

    "send all in stable" in {
      val testingMailer = new VerifiableMailer(Environments.Stable) with EnvironmentFilteredMailer
      val genAll = Gen.oneOf(
        Gen.nonEmptyListOf(YandexTeamEmailGen),
        Gen.nonEmptyListOf(NotYandexTeamEmailGen)
      )
      var expected = 0
      forAll(genAll) { emails =>
        testingMailer.send(emptyLetter(emails)).await
        expected = expected + emails.length
        testingMailer.sended shouldBe expected
      }
    }

  }

}

object EnvironmentFilteredMailerSpec {

  class VerifiableMailer(val env: Environments.Value) extends Mailer {

    var sended: Int = 0

    override def send(letter: Letter): Future[Unit] = {
      sended = sended + letter.recipient.all.length
      Future.unit
    }

  }

}
