package vertis.pushnoy.api.v2.device

import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.ScalacheckShapeless._
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.pushnoy.push_response_model.{PushSendSuccessResponse, ResponseStatus}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import vertis.pushnoy.dao.postgres.PgPushQueueDao
import vertis.pushnoy.dao.{Dao, TestDao}
import vertis.pushnoy.model.request.{PushMessageV1, RequestInfo, RequestInfoImpl}
import vertis.pushnoy.model.template.{
  CallWithoutVinReportPurchaseTemplate,
  ContactTheSellerTemplate,
  OfferBannedTemplate,
  OfferIncompleteFormTemplate,
  PersonalDiscountTemplate,
  PublishTheDraftTemplate,
  ReportPurchaseSummaryForOfferTemplate,
  SeveralDaysInactivityTemplate,
  Template,
  VinReportIsReadyTemplate
}
import vertis.pushnoy.model.{ClientType, DeliveryParams, Device}
import vertis.pushnoy.services.PushBanChecker
import vertis.pushnoy.util.enrich.EnrichManager

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

final case class DeviceManagerImplSpec()
  extends AnyWordSpec
  with ScalaFutures
  with Matchers
  with ScalaCheckPropertyChecks
  with TypeCheckedTripleEquals {

  import DeviceManagerImplSpec._

  implicit private val ctx: RequestInfo = RequestInfoImpl("", Traced.empty)

  "DeviceManagerImpl" when {
    "sending by template" should {
      // now that there are much business logic and no tests it is hard to write tests. If at some point in time
      // it is decided to cover the codebase with tests — this code can be used
      "send push" ignore new DeviceManagerEnv {
        forAll { (clientType: ClientType, device: Device, template: Template, deliveryParams: DeliveryParams) =>
          val actualValue = deviceManager.sendTemplate(clientType, device, template, deliveryParams).futureValue
          actualValue should ===(pushSendResponse)
        }
      }
    }
  }
}

object DeviceManagerImplSpec {
  private val messageGuid = new java.util.UUID(0L, 0L)

  private val pushSendResponse = PushSendSuccessResponse(ResponseStatus.SUCCESS, messageGuid.toString)

  trait DeviceManagerEnv extends MockitoSupport {
    private val pushQueue = mock[PgPushQueueDao]
    when(pushQueue.savePushRequest(?, ?, ?)(?)).thenReturn(Array(messageGuid.toString))

    private val dao: Dao = new TestDao

    private val enrichManager = mock[EnrichManager]

    when(enrichManager.enrichTemplate(?)(?)).thenAnswer { args =>
      val template = args.getArgument[Template](0)
      Future.successful(template)
    }

    protected val pushBanChecker: PushBanChecker = new PushBanChecker {
      override def isPushBanned(push: PushMessageV1): Boolean = false
      override def isPushBanned(template: Template): Boolean = false
    }

    protected val deviceManager = new DeviceManagerImpl(
      pushQueue,
      dao,
      enrichManager,
      pushBanChecker
    )
  }

  implicit val arbitraryTemplate: Arbitrary[Template] = Arbitrary(
    Gen.oneOf(
      implicitly[Arbitrary[CallWithoutVinReportPurchaseTemplate]].arbitrary,
      implicitly[Arbitrary[ContactTheSellerTemplate]].arbitrary,
      implicitly[Arbitrary[OfferBannedTemplate]].arbitrary,
      implicitly[Arbitrary[OfferIncompleteFormTemplate]].arbitrary,
      implicitly[Arbitrary[PersonalDiscountTemplate]].arbitrary,
      implicitly[Arbitrary[PublishTheDraftTemplate]].arbitrary,
      implicitly[Arbitrary[ReportPurchaseSummaryForOfferTemplate]].arbitrary,
      implicitly[Arbitrary[SeveralDaysInactivityTemplate]].arbitrary,
      implicitly[Arbitrary[VinReportIsReadyTemplate]].arbitrary
    )
  )
}
