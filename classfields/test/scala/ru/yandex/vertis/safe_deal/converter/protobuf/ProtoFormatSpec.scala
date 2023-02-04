package ru.yandex.vertis.safe_deal.converter.protobuf

import cats.syntax.validated._
import io.circe.parser.decode
import io.circe.syntax._
import org.scalacheck.{magnolia, Arbitrary}
import org.scalactic.anyvals.{PosInt, PosZDouble}
import ru.yandex.vertis.zio_baker.scalapb_utils._
import ru.yandex.vertis.safe_deal.model.Arbitraries._
import ru.yandex.vertis.safe_deal.model.Deal.DealFlags
import ru.yandex.vertis.safe_deal.model.DealView.{BuyerInfo, SellerInfo}
import ru.yandex.vertis.safe_deal.model._
import ru.yandex.vertis.safe_deal.model.event._
import ru.yandex.vertis.safe_deal.model.tinkoff.{TinkoffDeal, TinkoffRefillBankDetails}
import ru.yandex.vertis.safe_deal.model.ParsedPayment.ParsedOperation
import ru.yandex.vertis.safe_deal.proto.{model => proto}
import ru.yandex.vertis.safe_deal.proto.{dictionaries => dict}
import ru.yandex.vertis.test_utils.SpecBase
import ru.yandex.vertis.zio_baker.scalapb_utils.ProtoJson._
import scalapb.GeneratedEnum

import scala.reflect.ClassTag

class ProtoFormatSpec extends SpecBase {

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration = PropertyCheckConfiguration(
    minSuccessful = PosInt(100),
    maxDiscardedFactor = PosZDouble(5.0),
    minSize = 100,
    sizeRange = 10,
    workers = PosInt(8)
  )

  private val testCases: Seq[TestCase] = {

    import Implicits._
    import magnolia._

    Seq( // order with Implicits.scala preserved
      MessageTestCase[Entity.NameEntity, proto.Entity.NameEntity](),
      MessageTestCase[Entity.PassportRfEntity, proto.Entity.PassportRfEntity](),
      MessageTestCase[Entity.PhoneEntity, proto.Entity.PhoneEntity](),
      MessageTestCase[Entity.EmailEntity, proto.Entity.EmailEntity](),
      MessageTestCase[Entity.BankingEntity, proto.Entity.BankingEntity](),
      MessageTestCase[DocumentPhoto, proto.DocumentPhoto](),
      MessageTestCase[
        PushNotification.PayloadContent.PlatformUrl,
        proto.Notification.MessagePayload.Push.Payload.Action.PlatformUrl
      ](),
      MessageTestCase[PushNotification.PayloadContent.Action, proto.Notification.MessagePayload.Push.Payload.Action](),
      MessageTestCase[PushNotification.Content, proto.Notification.MessagePayload.Push](),
      MessageTestCase[ChatNotification.Content, proto.Notification.MessagePayload.Chat](),
      MessageTestCase[EmailNotification.Attachment, proto.Notification.MessagePayload.Email.Attachment](),
      MessageTestCase[EmailNotification.Content, proto.Notification.MessagePayload.Email](),
      MessageTestCase[SmsNotification.Content, proto.Notification.MessagePayload.Sms](),
      MessageTestCase[NotificationTemplate.PushContent, proto.NotificationTemplate.Push](),
      MessageTestCase[NotificationTemplate.ChatContent, proto.NotificationTemplate.Chat](),
      MessageTestCase[Api.DealsResponse, proto.Api.DealListResponse](),
      MessageTestCase[Api.RawDealsResponse, proto.Api.RawDealListResponse](),
      MessageTestCase[Api.DealResponse, proto.Api.DealResponse](),
      MessageTestCase[Api.UploadedPhotoResponse, proto.Api.UploadedPhotoResponse](),
      MessageTestCase[DealCreateRequest, proto.Api.DealCreateRequest](),
      MessageTestCase[DealCreateRequest.Subject, proto.Api.DealCreateRequest.Subject](),
      // MessageTestCase[DealUpdateRequest, proto.Api.DealUpdateRequest](), //TODO AUTORUBACK-2167 тут не хватает экшенов в протомодели
      MessageTestCase[Deal, proto.Deal](),
      MessageTestCase[DealSubject, proto.Deal.Subject](),
      MessageTestCase[AutoruSubject, proto.Deal.Subject](),
      MessageTestCase[AutoruDeal.Offer, proto.Deal.Subject.Autoru.Offer](),
      MessageTestCase[AutoruSubject.OfferCarInfo, proto.Deal.Subject.Autoru.OfferCarInfo](),
      MessageTestCase[AutoruSubject.StsCarInfo, proto.Deal.Subject.Autoru.StsCarInfo](),
      MessageTestCase[AutoruSubject.PtsCarInfo, proto.Deal.Subject.Autoru.PtsCarInfo](),
      MessageTestCase[DealView, proto.DealView](),
      MessageTestCase[DealChangedEvent, proto.DealChangedEvent](),
      MessageTestCase[PersonProfile, proto.PersonProfile](),
      MessageTestCase[DealParty.ConfirmationCode, proto.Deal.ConfirmationCode](),
      MessageTestCase[Escrow, proto.Deal.Escrow](),
      MessageTestCase[RegionInfo, proto.RegionInfo](),
      MessageTestCase[Seller, proto.Deal.Seller](),
      MessageTestCase[Buyer, proto.Deal.Buyer](),
      MessageTestCase[SellerInfo, proto.DealView.Party.SellerInfo](),
      MessageTestCase[BuyerInfo, proto.DealView.Party.BuyerInfo](),
      MessageTestCase[SafeDealAccount, proto.Deal.SafeDealAccount](),
      MessageTestCase[DocumentPhotoSizes, proto.DocumentPhotoSizes](),
      MessageTestCase[DealEvent, proto.DealEvent](),
      MessageTestCase[Notification, proto.Notification](),
      MessageTestCase[NotificationSource, proto.NotificationSource](),
      MessageTestCase[NotificationTemplate, proto.NotificationTemplate](),
      MessageTestCase[Api.DefaultResponse, proto.Api.DefaultResponse](),
      MessageTestCase[Api.UserAttentionsResponse, proto.Api.UserAttentionsResponse](),
      MessageTestCase[Api.Result.FieldErrors.Entity, proto.Api.Result.Error.FieldErrors.Entity](),
      MessageTestCase[Api.Result, proto.Api.Result](),
      MessageTestCase[CommissionTariff, dict.CommissionTariff](),
      MessageTestCase[Api.CommissionTariffResponse, proto.Api.CommissionTariffResponse](),
      MessageTestCase[Api.CurrentCommissionTariffResponse, proto.Api.CurrentCommissionTariffResponse](),
      MessageTestCase[DealFlags, proto.Deal.DealFlags](),
      MessageTestCase[ParsedOperation, proto.BankOperationEvent](),
      MessageTestCase[Payment, proto.Deal.PaymentInfo](),
      MessageTestCase[TinkoffRefillBankDetails, proto.PersonProfile.TinkoffRefillBankDetails](),
      MessageTestCase[TinkoffDeal.RefillItem, proto.Deal.TinkoffDeal.RefillItem](),
      MessageTestCase[TinkoffDeal.PaymentItem, proto.Deal.TinkoffDeal.PaymentItem](),
      MessageTestCase[TinkoffDeal, proto.Deal.TinkoffDeal]()
    )
  }

  testCases.foreach { testCase =>
    testCase.check()
  }

  trait TestCase {
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
