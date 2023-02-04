package ru.yandex.vertis.shark.converter.protobuf

import cats.syntax.validated._
import io.circe.parser.decode
import io.circe.syntax._
import org.scalacheck.{magnolia, Arbitrary}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.zio_baker.scalapb_utils.ProtoJson._
import ru.yandex.vertis.zio_baker.scalapb_utils._
import ru.yandex.vertis.shark.model.Arbitraries._
import ru.yandex.vertis.shark.model._
import ru.yandex.vertis.shark.model.event._
import ru.yandex.vertis.shark.proto.{common => common_proto, model => proto}
import scalapb.GeneratedEnum

import scala.reflect.ClassTag

class ProtoFormatSpec extends AnyWordSpecLike with ScalaCheckPropertyChecks with Matchers {

  private val testCases: Seq[TestCase] = {

    import Implicits._
    import magnolia._

    Seq(
      MessageTestCase[Bank, proto.Bank](),
      MessageTestCase[CreditProduct.AmountRange, common_proto.AmountRange](),
      MessageTestCase[CreditProduct.InterestRateRange, common_proto.InterestRateRange](),
      MessageTestCase[CreditProduct.TermMonthsRange, common_proto.TermMonthsRange](),
      MessageTestCase[CreditProduct.CreditProposal, proto.CreditProduct.CreditProposal.Entity](),
      MessageTestCase[CreditProduct.BorrowerConditions, proto.CreditProduct.BorrowerConditions](),
      MessageTestCase[CreditProduct.BorrowerConditions.Score, proto.CreditProduct.BorrowerConditions.Score](),
      MessageTestCase[CreditProduct.RateLimit, proto.CreditProduct.RateLimit](),
      MessageTestCase[CreditProduct.ObjectPayload, proto.CreditProduct.ObjectPayload](),
      MessageTestCase[CreditProduct, proto.CreditProduct](),
      MessageTestCase[Entity.NameEntity, proto.Entity.NameEntity](),
      MessageTestCase[Entity.PassportRfEntity, proto.Entity.PassportRfEntity](),
      MessageTestCase[Entity.InsuranceNumberEntity, proto.Entity.InsuranceNumberEntity](),
      MessageTestCase[Entity.DriverLicenseEntity, proto.Entity.DriverLicenseEntity](),
      MessageTestCase[Entity.AddressEntity.Kladr, proto.Entity.AddressEntity.Kladr](),
      MessageTestCase[Entity.AddressEntity.Fias, proto.Entity.AddressEntity.Fias](),
      MessageTestCase[Entity.AddressEntity, proto.Entity.AddressEntity](),
      MessageTestCase[Entity.PhoneEntity, proto.Entity.PhoneEntity](),
      MessageTestCase[Entity.EmailEntity, proto.Entity.EmailEntity](),
      MessageTestCase[Block.NameBlock, proto.Block.NameBlock](),
      MessageTestCase[Block.OldNameBlock, proto.Block.OldNameBlock](),
      MessageTestCase[Block.GenderBlock, proto.Block.GenderBlock](),
      MessageTestCase[Block.PassportRfBlock, proto.Block.PassportRfBlock](),
      MessageTestCase[Block.OldPassportRfBlock, proto.Block.OldPassportRfBlock](),
      MessageTestCase[Block.ForeignPassportBlock, proto.Block.ForeignPassportBlock](),
      MessageTestCase[Block.InsuranceNumberBlock, proto.Block.InsuranceNumberBlock](),
      MessageTestCase[Block.DriverLicenseBlock, proto.Block.DriverLicenseBlock](),
      MessageTestCase[Block.BirthDateBlock, proto.Block.BirthDateBlock](),
      MessageTestCase[Block.BirthPlaceBlock, proto.Block.BirthPlaceBlock](),
      MessageTestCase[Block.ResidenceAddressBlock, proto.Block.ResidenceAddressBlock](),
      MessageTestCase[Block.RegistrationAddressBlock, proto.Block.RegistrationAddressBlock](),
      MessageTestCase[Block.EducationBlock, proto.Block.EducationBlock](),
      MessageTestCase[Block.MaritalStatusBlock, proto.Block.MaritalStatusBlock](),
      MessageTestCase[Block.DependentsBlock, proto.Block.DependentsBlock](),
      MessageTestCase[Block.IncomeBlock, proto.Block.IncomeBlock](),
      MessageTestCase[Block.ExpensesBlock, proto.Block.ExpensesBlock](),
      MessageTestCase[Block.PropertyOwnershipBlock, proto.Block.PropertyOwnershipBlock](),
      MessageTestCase[Block.VehicleOwnershipBlock, proto.Block.VehicleOwnershipBlock](),
      MessageTestCase[Block.EmploymentBlock, proto.Block.EmploymentBlock](),
      MessageTestCase[Block.RelatedPersonsBlock, proto.Block.RelatedPersonsBlock](),
      MessageTestCase[Block.PhonesBlock, proto.Block.PhonesBlock](),
      MessageTestCase[Block.EmailsBlock, proto.Block.EmailsBlock](),
      MessageTestCase[Block.ControlWordBlock, proto.Block.ControlWordBlock](),
      MessageTestCase[Block.OkbStatementAgreementBlock, proto.Block.OkbStatementAgreementBlock](),
      MessageTestCase[Block.AdvertStatementAgreementBlock, proto.Block.AdvertStatementAgreementBlock](),
      MessageTestCase[PersonProfile, proto.PersonProfile](),
      MessageTestCase[Score, proto.Score](),
      MessageTestCase[
        PushNotification.PayloadContent.PlatformUrl,
        proto.Notification.MessagePayload.Push.Payload.Action.PlatformUrl
      ](),
      MessageTestCase[
        PushNotification.PayloadContent.Action,
        proto.Notification.MessagePayload.Push.Payload.Action
      ](),
      MessageTestCase[PushNotification.Content, proto.Notification.MessagePayload.Push](),
      MessageTestCase[ChatNotification.Content, proto.Notification.MessagePayload.Chat](),
      MessageTestCase[Notification, proto.Notification](),
      MessageTestCase[NotificationSource, proto.NotificationSource](),
      MessageTestCase[CreditApplication.Info, proto.CreditApplication.Info](),
      MessageTestCase[CreditApplication.Requirements, proto.CreditApplication.Requirements](),
      MessageTestCase[
        CreditApplication.AutoruClaim.OfferEntity,
        proto.CreditApplication.Claim.ClaimPayload.Autoru.OfferEntity
      ](),
      MessageTestCase[
        CreditApplication.Claim.SentSnapshot.CreditProductProperties,
        proto.CreditApplication.Claim.SentSnapshot.CreditProductProperties
      ](),
      MessageTestCase[CreditApplication.Claim.SentSnapshot, proto.CreditApplication.Claim.SentSnapshot](),
      MessageTestCase[CreditApplication.Claim, proto.CreditApplication.Claim](),
      MessageTestCase[AutoruCreditApplication.Offer, proto.CreditApplication.Payload.Autoru.Offer](),
      MessageTestCase[
        AutoruCreditApplication.ExternalCommunication.ClaimEntity,
        proto.CreditApplication.Communication.AutoruExternal.ClaimEntity
      ](),
      MessageTestCase[
        AutoruCreditApplication.ExternalCommunication,
        proto.CreditApplication.Communication.AutoruExternal
      ](),
      MessageTestCase[
        AutoruCreditApplication.ExternalCommunicationSource,
        proto.CreditApplication.Communication.AutoruExternalSource
      ](),
      MessageTestCase[CreditApplication.UserSettings, proto.CreditApplication.UserSettings](),
      MessageTestCase[CreditApplication, proto.CreditApplication](),
      MessageTestCase[CreditApplicationSource.Payload, proto.CreditApplicationSource.Payload](),
      MessageTestCase[CreditApplicationSource, proto.CreditApplicationSource](),
      MessageTestCase[
        CreditApplicationClaimSource.AutoruPayload.OfferEntity,
        proto.CreditApplicationClaimSource.Payload.Autoru.OfferEntity
      ](),
      MessageTestCase[CreditApplicationClaimSource.Payload, proto.CreditApplicationClaimSource.Payload](),
      MessageTestCase[CreditApplicationClaimSource, proto.CreditApplicationClaimSource](),
      MessageTestCase[Subscription, proto.Subscription](),
      MessageTestCase[Api.BanksResponse, proto.Api.BanksResponse](),
      MessageTestCase[Api.CreditProductsRequest, proto.Api.CreditProductsRequest](),
      MessageTestCase[Api.CreditProductsResponse, proto.Api.CreditProductsResponse](),
      MessageTestCase[Api.CreditProductDependenciesResponse, proto.Api.CreditProductDependenciesResponse](),
      MessageTestCase[Api.AddProductsResponse.Item, proto.Api.AddProductsResponse.Item](),
      MessageTestCase[Api.AddProductsResponse, proto.Api.AddProductsResponse](),
      MessageTestCase[Api.PersonProfilesResponse, proto.Api.PersonProfilesResponse](),
      MessageTestCase[Api.CreditApplicationRequest, proto.Api.CreditApplicationRequest](),
      MessageTestCase[Api.CreditApplicationsRequest, proto.Api.CreditApplicationsRequest](),
      MessageTestCase[Api.CreditApplicationsResponse, proto.Api.CreditApplicationsResponse](),
      MessageTestCase[Api.CreditApplicationResponse, proto.Api.CreditApplicationResponse](),
      MessageTestCase[Api.CreateCreditClaimsRequest, proto.Api.CreateCreditClaimsRequest](),
      MessageTestCase[Api.NotificationSubscriptionsResponse, proto.Api.NotificationSubscriptionsResponse](),
      MessageTestCase[Api.DictionaryHistoryResponse, proto.Api.DictionaryHistoryResponse](),
      MessageTestCase[Api.DefaultResponse, proto.Api.DefaultResponse](),
      MessageTestCase[Api.Result.FieldErrors.Entity, proto.Api.Result.Error.FieldErrors.Entity](),
      MessageTestCase[Api.Result, proto.Api.Result](),
      MessageTestCase[CreditApplicationEvent, proto.CreditApplicationEvent](),
      MessageTestCase[CreditApplicationClaimEvent, proto.CreditApplicationClaimEvent](),
      MessageTestCase[CreditApplicationClaimSentEvent, proto.CreditApplicationClaimSentEvent](),
      MessageTestCase[
        CreditApplicationAutoruExternalCommunicationEvent.ObjectPayload.Photo,
        proto.CreditApplicationAutoruExternalCommunicationEvent.ObjectPayload.Photo
      ](),
      MessageTestCase[
        CreditApplicationAutoruExternalCommunicationEvent.ObjectPayload,
        proto.CreditApplicationAutoruExternalCommunicationEvent.ObjectPayload
      ](),
      MessageTestCase[
        CreditApplicationAutoruExternalCommunicationEvent.UserPayload,
        proto.CreditApplicationAutoruExternalCommunicationEvent.UserPayload
      ](),
      MessageTestCase[
        CreditApplicationAutoruExternalCommunicationEvent,
        proto.CreditApplicationAutoruExternalCommunicationEvent
      ](),
      MessageTestCase[CreditApplicationCallCenterEvent, proto.CreditApplicationCallCenterEvent](),
      MessageTestCase[CreditApplication.Claim.BankPayload, proto.CreditApplication.Claim.BankPayload](),
      MessageTestCase[CreditApplication.Claim.NotSentReason, proto.CreditApplication.Claim.NotSentReason](),
      MessageTestCase[CreditApplication.UpdateRequest.Payload, proto.CreditApplicationUpdateRequest.Payload](),
      MessageTestCase[CreditApplication.UpdateRequest, proto.CreditApplicationUpdateRequest](),
      MessageTestCase[CheckRequirements.Matching, proto.Suitable.CheckRequirements.Matching](),
      MessageTestCase[CheckRequirements, proto.Suitable.CheckRequirements](),
      MessageTestCase[CheckBorrower, proto.Suitable.CheckBorrower](),
      MessageTestCase[CheckBorrower.CheckScore, proto.Suitable.CheckBorrower.CheckScore](),
      MessageTestCase[MissingBlocks, proto.Suitable.MissingBlocks](),
      MessageTestCase[CheckObject, proto.Suitable.CheckObject](),
      MessageTestCase[Suitable, proto.Suitable](),
      MessageTestCase[Validation.Error, proto.ValidationError](),
      MessageTestCase[DictionaryUpdate, proto.DictionaryUpdate](),
      MessageTestCase[Api.ObjectInfo, proto.Api.PreconditionsRequest.ObjectInfo](),
      MessageTestCase[Api.PreconditionsRequest.ObjectPayload, proto.Api.PreconditionsRequest.ObjectPayload](),
      MessageTestCase[Api.PreconditionsRequest, proto.Api.PreconditionsRequest](),
      MessageTestCase[Api.ProductPrecondition, proto.Api.PreconditionsResponse.Precondition.ProductPrecondition](),
      MessageTestCase[Api.Precondition, proto.Api.PreconditionsResponse.Precondition](),
      MessageTestCase[Api.PreconditionsResponse, proto.Api.PreconditionsResponse](),
      MessageTestCase[Api.EcreditCalculationRequest, proto.Api.EcreditCalculationRequest](),
      MessageTestCase[Api.EcreditCalculationResponse, proto.Api.EcreditCalculationResponse](),
      MessageTestCase[CreditProductResourceContainer, proto.CreditProductResourceContainer](),
      MessageTestCase[BankResourceContainer, proto.BankResourceContainer](),
      MessageTestCase[FiasGeobaseEntity, proto.FiasGeobaseEntity](),
      MessageTestCase[FiasGeobaseResourceContainer, proto.FiasGeobaseResourceContainer]()
    )
  }

  testCases.foreach { testCase =>
    testCase.check()
  }

  sealed trait TestCase {
    def check(): Unit
  }

  private case class MessageTestCase[T, M <: ProtoMessage[M]](
    )(implicit val format: ProtoMessageFormat[T, M],
      val arb: Arbitrary[T],
      classTag: ClassTag[T])
    extends TestCase {

    override def check(): Unit =
      s"ProtoMessageFormat for $classTag" should {
        "round-trip through protobuf" in forAll { x: T =>
          format.fromProto(format.toProto(x)) shouldBe x.valid
        }
        "round-trip through JSON" in forAll { x: T =>
          decode(x.asJson.toString) shouldBe Right(x)
        }
      }
  }

  case class EnumTestCase[T, M <: GeneratedEnum](
    )(implicit val format: ProtoEnumFormat[T, M],
      val arb: Arbitrary[T],
      classTag: ClassTag[T])
    extends TestCase {

    override def check(): Unit =
      s"ProtoEnumFormat for $classTag" should {
        "round-trip through protobuf" in forAll { x: T =>
          format.fromProto(format.toProto(x)) shouldBe x.valid
        }
      }
  }
}
