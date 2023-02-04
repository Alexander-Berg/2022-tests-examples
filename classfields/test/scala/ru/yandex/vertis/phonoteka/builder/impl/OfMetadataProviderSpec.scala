package ru.yandex.vertis.phonoteka.builder.impl

import java.time.Instant

import cats.implicits.catsSyntaxApplicativeId
import ru.yandex.vertis.phonoteka.builder.impl.OfMetadataProviderSpec.{
  OfMetadataTestCaseFailure,
  OfMetadataTestCaseSuccess
}
import ru.yandex.vertis.phonoteka.client.impl.HttpOfClient
import ru.yandex.vertis.phonoteka.model.Phone
import ru.yandex.vertis.phonoteka.model.metadata.OfMetadata
import ru.yandex.vertis.phonoteka.util.stub.StubOfUtil._
import ru.yandex.vertis.quality.cats_utils.Awaitable.AwaitableSyntax
import ru.yandex.vertis.quality.test_utils.SpecBase
import sttp.client.{Identity, NothingT, RequestT, SttpBackend}

class OfMetadataProviderSpec extends SpecBase {

  implicit val stubBackend = mock[SttpBackend[F, Nothing, NothingT]]
  stub(stubBackend.send(_: RequestT[Identity, String, Nothing])) { case request =>
    getResponse(request.body).pure
  }
  private val stubClient = new HttpOfClient[F](testConfig.url)

  val provider = new OfMetadataProvider[F](stubClient)

  val testCasesSuccess: Seq[OfMetadataTestCaseSuccess] =
    Seq(
      OfMetadataTestCaseSuccess(
        "successfully get metadata",
        phone1,
        OfMetadata(phone1, Instant.now(), Some(5), Some(2))
      ),
      OfMetadataTestCaseSuccess(
        "successfully get metadata 2",
        phone2,
        OfMetadata(phone1, Instant.now(), Some(1), Some(4))
      ),
      OfMetadataTestCaseSuccess(
        "successfully get metadata when variable is None",
        phone4,
        OfMetadata(phone1, Instant.now(), Some(1), None)
      )
    )

  val testCasesFailure: Seq[OfMetadataTestCaseFailure] =
    Seq(
      OfMetadataTestCaseFailure(
        "fail when variable is decimal",
        phone3,
        "Unexpected OF variable value"
      ),
      OfMetadataTestCaseFailure(
        "fail while parse variables with unexpected status",
        phone5,
        "not a member of enum"
      ),
      OfMetadataTestCaseFailure(
        "fail for unexpected json body",
        phone6,
        "failed cursor"
      )
    )

  "OfMetadataProviderSpec" should {
    testCasesSuccess.foreach { case OfMetadataTestCaseSuccess(description, phone, expected) =>
      description in {
        val result = provider.get(phone).await
        result.ownershipFactor shouldBe expected.ownershipFactor
        result.communicationFactor shouldBe expected.communicationFactor
      }
    }

    testCasesFailure.foreach { case OfMetadataTestCaseFailure(description, phone, expected) =>
      description in {
        val caught =
          intercept[Exception] {
            provider.get(phone).await
          }
        caught.getMessage == expected
      }
    }
  }
}

object OfMetadataProviderSpec {
  case class OfMetadataTestCaseSuccess(description: String, phone: Phone, expected: OfMetadata)
  case class OfMetadataTestCaseFailure(description: String, phone: Phone, expected: String)
}
