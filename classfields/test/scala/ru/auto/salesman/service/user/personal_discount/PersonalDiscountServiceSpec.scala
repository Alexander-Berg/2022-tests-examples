package ru.auto.salesman.service.user.personal_discount

import org.joda.time.DateTime
import ru.auto.salesman.client.PromocoderClient.Services.AutoRuUsers
import ru.auto.salesman.client.impl.AutoRuPromocoderClient
import ru.auto.salesman.client.pushnoy.PushnoyClientV2
import ru.auto.salesman.environment.{now, startOfToday}
import ru.auto.salesman.model.push._
import ru.auto.salesman.model.user.product.ProductProvider.AutoruBundles.Turbo
import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods.Top
import ru.auto.salesman.model.user.product.ProductProvider.AutoruSubscriptions.OffersHistoryReports
import ru.auto.salesman.model.{AutoruUser, PromocoderUser}
import ru.auto.salesman.service.broker.PersonalDiscountBrokerService
import ru.auto.salesman.service.impl.user.PromocoderServiceImpl
import ru.auto.salesman.service.impl.user.personal_discount.PersonalDiscountServiceImpl
import ru.auto.salesman.service.impl.user.personal_discount.PersonalDiscountServiceImpl._
import ru.auto.salesman.service.user.personal_discount.PersonalDiscountService.PersonalDiscountModel
import ru.auto.salesman.service.user.personal_discount.PersonalDiscountServiceSpec.nextUserId
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.docker.PromocoderContainer
import ru.auto.salesman.util.DateTimeInterval
import ru.auto.salesman.util.sttp.SttpClientImpl
import ru.yandex.pushnoy.PushResponseModel.ListPushSendResponse
import ru.yandex.vertis.ops.test.TestOperationalSupport
import zio.ZIO

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.duration.DurationInt

class PersonalDiscountServiceSpec extends BaseSpec {

  private val baseRequest = PersonalDiscountModel(
    AutoruUser(nextUserId.getAndIncrement()),
    Top,
    discount = 5,
    discountInterval = DateTimeInterval(
      from = startOfToday().minusDays(1),
      to = startOfToday().plusDays(3)
    ),
    pushRequest = None,
    experiment = None,
    featureCount = None
  )

  private val promocoderClient = new AutoRuPromocoderClient(
    AutoRuUsers,
    PromocoderContainer.address,
    SttpClientImpl(TestOperationalSupport)
  )

  private val broker = stub[PersonalDiscountBrokerService]
  (broker.sendDiscountInfo _).when(*, *, *).returningZ(())

  private val pushnoyClient = mock[PushnoyClientV2]

  private val service = new PersonalDiscountServiceImpl(
    new PromocoderServiceImpl(promocoderClient),
    pushnoyClient,
    broker
  )

  import service.createPersonalDiscount

  "PersonalDiscountService" should {

    "create feature with periodical-discount in id" in {
      createPersonalDiscountAndGetUserFeatures(baseRequest)
        .map(_.loneElement.id should include("personal-discount"))
        .success
    }

    "generate different promocoder features for different discount intervals" in {
      val earlierRequest = baseRequest.copy(discountInterval =
        DateTimeInterval(
          from = startOfToday().minusDays(3),
          to = startOfToday().plusDays(2)
        )
      )
      val laterRequest = baseRequest.copy(discountInterval =
        DateTimeInterval(
          from = startOfToday().minusDays(2),
          to = startOfToday().plusDays(4)
        )
      )
      createPersonalDiscount(earlierRequest) *>
        createPersonalDiscountAndGetUserFeatures(laterRequest).map(
          _ should have size 2
        )
    }.success

    "generate one promocoder feature for retrying of the same request" in {
      ZIO.foreach_(1 to 10)(_ => createPersonalDiscount(baseRequest)) *>
        getUserFeatures(baseRequest).map(_ should have size 1)
    }.success

    "set feature tag = product type" in {
      val request = baseRequest.copy(product = Turbo)
      createPersonalDiscountAndGetUserFeatures(request)
        .map(_.loneElement.tag shouldBe "turbo-package")
        .success
    }

    "set feature tag = offers-history-reports-10 for package" in {
      val request = baseRequest.copy(product = OffersHistoryReports(10))
      createPersonalDiscountAndGetUserFeatures(request)
        .map(_.loneElement.tag shouldBe "offers-history-reports-10")
        .success
    }

    "set feature lifetime based on discount interval duration" in {
      val request = baseRequest.copy(discountInterval =
        DateTimeInterval(
          from = DateTime.parse("2020-10-05T10:00+03:00"),
          to = DateTime.parse("2020-10-05T20:00+03:00")
        )
      )
      val result = toFeatureInstanceRequest(request)
      result.lifetime shouldBe 10.hours
    }

    "create feature for future usage, which isn't allowed to use right now" in {
      val request = baseRequest.copy(
        discountInterval = DateTimeInterval(
          from = now().plusDays(1),
          to = now().plusDays(2)
        )
      )
      createPersonalDiscountAndGetUserFeatures(request)
        .map(_ shouldBe Nil)
        .success
    }

    "create feature for current usage with given deadline" in {
      val deadline = startOfToday().plusDays(3)
      val request = baseRequest.copy(
        discountInterval = DateTimeInterval(
          from = startOfToday().minusDays(1),
          to = deadline
        )
      )
      createPersonalDiscountAndGetUserFeatures(request)
        .map(_.loneElement.deadline shouldBe deadline)
        .success
    }

    "create feature when featureCount is not set, feature should be created with default value of 200" in {
      createPersonalDiscountAndGetUserFeatures(baseRequest)
        .map(_.loneElement.count.count shouldBe 200)
        .success
    }

    "create feature when featureCount = 25" in {
      val request = baseRequest.copy(featureCount = Some(25))
      createPersonalDiscountAndGetUserFeatures(request)
        .map(_.loneElement.count.count shouldBe 25)
        .success
    }

    "pass push time boundaries validation" in {
      val pushRequest = createPushRequest(
        startOfToday().minusDays(1),
        startOfToday().plusDays(1)
      )

      val request = baseRequest.copy(
        discountInterval = DateTimeInterval(
          from = startOfToday().minusDays(2),
          to = startOfToday().plusDays(2)
        ),
        pushRequest = Some(pushRequest)
      )

      (pushnoyClient.sendPush _)
        .expects(*, *, *, *, *)
        .returningZ(ListPushSendResponse.getDefaultInstance)

      createPersonalDiscountAndGetUserFeatures(request).success
    }

    "pass push time boundaries validation for same start push time and discount" in {
      val pushRequest = createPushRequest(
        startOfToday().minusDays(1),
        startOfToday().plusDays(1)
      )

      val request = baseRequest.copy(
        discountInterval = DateTimeInterval(
          from = startOfToday().minusDays(1),
          to = startOfToday().plusDays(1)
        ),
        pushRequest = Some(pushRequest)
      )

      (pushnoyClient.sendPush _)
        .expects(*, *, *, *, *)
        .returningZ(ListPushSendResponse.getDefaultInstance)

      createPersonalDiscountAndGetUserFeatures(request).success
    }

    "push request start time fail validation" in {
      val pushRequest = createPushRequest(
        startOfToday().minusDays(10),
        startOfToday().plusDays(1)
      )

      val request = baseRequest.copy(
        discountInterval = DateTimeInterval(
          from = startOfToday().minusDays(1),
          to = startOfToday().plusDays(1)
        ),
        pushRequest = Some(pushRequest)
      )

      (pushnoyClient.sendPush _).expects(*, *, *, *, *).never

      createPersonalDiscountAndGetUserFeatures(request).failure
    }

    "push request end time fail validation" in {
      val pushRequest = createPushRequest(
        startOfToday().minusDays(1),
        startOfToday().plusDays(10)
      )

      val request = baseRequest.copy(
        discountInterval = DateTimeInterval(
          from = startOfToday().minusDays(1),
          to = startOfToday().plusDays(1)
        ),
        pushRequest = Some(pushRequest)
      )

      (pushnoyClient.sendPush _).expects(*, *, *, *, *).never

      createPersonalDiscountAndGetUserFeatures(request).failure
    }
  }

  private def createPushRequest(from: DateTime, to: DateTime) =
    PushRequest(
      PushName("push_name"),
      PushTitle("push_title"),
      PushBody("push_body"),
      DateTimeInterval(from, to)
    )

  private def createPersonalDiscountAndGetUserFeatures(
      request: PersonalDiscountModel
  ) =
    service.createPersonalDiscount(request) *> getUserFeatures(request)

  private def getUserFeatures(request: PersonalDiscountModel) =
    promocoderClient.getFeatures(PromocoderUser(request.user))
}

object PersonalDiscountServiceSpec {

  private val nextUserId = new AtomicInteger(1)
}
