package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.mockito.Mockito
import org.mockito.Mockito.{doNothing, verify}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.ModerationFieldsModel.ModerationFields
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => eqq}
import ru.yandex.vertis.moderation.proto.Model.DetailedReason.Details
import ru.yandex.vertis.moderation.proto.Model.{DetailedReason, Reason}
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer.PaidService
import ru.yandex.vos2.OfferModel
import ru.yandex.vos2.OfferModel.OfferFlag
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.commonfeatures.FeaturesManager
import ru.yandex.vos2.util.ExternalAutoruUserRef

import scala.jdk.CollectionConverters._

class CheckShouldStayActiveWorkerYdbTest
  extends AnyWordSpec
  with MockitoSupport
  with Matchers
  with BeforeAndAfterAll
  with InitTestDbs {
  implicit val traced: Traced = Traced.empty
  private val chatClient = components.chatClient

  abstract private class Fixture {

    val worker = new CheckShouldStayActiveWorkerYdb(
      components.chatClient
    ) with YdbWorkerTestImpl {
      override def features: FeaturesManager = components.featuresManager
    }
  }

  private val paidService = PaidService.newBuilder
    .setIsActive(true)
    .setServiceType(PaidService.ServiceType.ADD)
    .setCreated(111)
    .build()

  private val reason = DetailedReason.newBuilder
    .setReason(Reason.USER_RESELLER)
    .setDetails {
      val b = Details.newBuilder
      b.getUserResellerBuilder.setShouldStayActive(true)
      b.build()
    }
    .build

  ("shouldProcess: has active ADD") in new Fixture {

    val offer: OfferModel.Offer = createOffer(
      shouldStayActive = true,
      services = List(paidService),
      reasons = List(reason)
    )
    assert(worker.shouldProcess(offer, None).shouldProcess)
  }

  ("shouldProcess: is not special reseller") in new Fixture {
    val offer: OfferModel.Offer = createOffer(shouldStayActive = true)
    assert(worker.shouldProcess(offer, None).shouldProcess)
  }

  ("shouldProcess: doesn't have active ADD") in new Fixture {
    val offer: OfferModel.Offer = createOffer(shouldStayActive = true, reasons = List(reason))
    assert(!worker.shouldProcess(offer, None).shouldProcess)
  }

  ("process: clear should_stay_active and chat_only") in new Fixture {
    doNothing().when(components.chatClient).unbanUser(?)(?)

    val offer: OfferModel.Offer = createOffer(
      shouldStayActive = true,
      chatOnly = true,
      flags = List(OfferFlag.OF_INACTIVE),
      moderationFields = List(ModerationFields.Fields.CHAT_ONLY)
    )

    val result = worker.process(offer, None).updateOfferFunc.get(offer)

    assert(!result.getShouldStayActiveInSearcher)
    assert(!result.getOfferAutoru.getSeller.getChatOnly)
    assert(!result.getOfferAutoru.getModerationProtectedFieldsList.contains(ModerationFields.Fields.CHAT_ONLY))
    assert(result.getFlagList.contains(OfferFlag.OF_INACTIVE))
    val user: String = ExternalAutoruUserRef.fromUserRef(offer.getUserRef).get
    verify(chatClient).unbanUser(eqq(user))(?)
    Mockito.reset(chatClient)
  }

  private def createOffer(
      shouldStayActive: Boolean = false,
      chatOnly: Boolean = false,
      flags: List[OfferFlag] = List(),
      services: List[PaidService] = List(),
      reasons: List[DetailedReason] = List(),
      moderationFields: List[ModerationFields.Fields] = List(),
      category: Category = Category.CARS
  ) = {
    val b = TestUtils.createOffer(category = category)

    b.setShouldStayActiveInSearcher(shouldStayActive)
    b.addAllFlag(flags.asJava)
    b.getOfferAutoruBuilder.addAllServices(services.asJava)
    b.getOfferAutoruBuilder.getSellerBuilder.setChatOnly(chatOnly)
    b.getOfferAutoruBuilder.addAllModerationProtectedFields(moderationFields.asJava)
    b.addAllDetailedBanReasons(reasons.asJava)

    val offer = b.build()
    offer
  }
}
