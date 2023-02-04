package ru.auto.comeback.consumer.test.subscription

import auto.common.clients.carfax.testkit.CarfaxMock
import auto.common.model.user.AutoruUser.UserRef.DealerId
import com.google.protobuf.ByteString
import common.zio.logging.Logging
import common.zio.ops.tracing.testkit.TestTracing
import ru.auto.api.comeback_model.ComebackListingRequest.Filter
import ru.auto.api.comeback_model.SearchSubscription.{Settings, Status}
import ru.auto.api.vin.vin_report_model.{DtpBlock, Header, RawVinEssentialsReport}
import ru.auto.comeback.consumer.testkit.{ComebackMailSenderMock, OfferServiceMock}
import ru.auto.comeback.model.subscriptions.SearchSubscription
import ru.auto.comeback.model.testkit.{ComebackGen, VosOfferGen}
import ru.auto.comeback.sender.ComebackMailSender.NotificationData
import ru.auto.comeback.services.testkit.{ComebackServiceMock, PaymentServiceMock, SearchSubscriptionServiceMock}
import ru.auto.comeback.subscription.{SubscriptionNotifierEventProcessor, SubscriptionNotifierEventProcessorImpl}
import ru.yandex.vertis.subscriptions.model.MatchedDocument
import ru.yandex.vertis.subscriptions.model.notifier.broker.BrokerEvent
import zio.ZIO
import zio.clock.Clock
import zio.test.Assertion._
import zio.test.TestAspect.sequential
import zio.test.mock.Expectation._
import zio.test.{assertM, checkM, DefaultRunnableSpec}

import java.time.Instant
import java.util.UUID
import scala.concurrent.duration._
import scala.util.Random

object SubscriptionNotifierEventProcessorImplSpec extends DefaultRunnableSpec {

  private val baseEnv = Clock.live ++ TestTracing.noOp ++ Logging.live

  override def spec = {
    suite("SubscriptionNotifierEventProcessorImpl")(
      failedByRequiredPayments,
      failedByDeletedSubscription,
      failedByUnknownEvent,
      successfulCase
    ) @@ sequential
  }

  private def createEvent(namespace: String, userId: String, subscriptionId: String) = BrokerEvent(
    service = namespace,
    user = userId,
    subscriptionId = subscriptionId,
    document = Seq(
      MatchedDocument(
        id = new Random().nextLong(Long.MaxValue - 1).toString,
        rawContent = ByteString.EMPTY
      )
    )
  )

  private def createSubscription(status: Status, user: DealerId) =
    SearchSubscription(1, user, Instant.now(), UUID.randomUUID().toString, Settings(Filter()), status)

  private val failedByRequiredPayments = testM("Not send email if dealer doesn't have required payment history") {
    val user = DealerId(1234L)
    val subscription = createSubscription(Status.ACTIVE, user)
    val event = createEvent("autoru_comeback", user.toString, subscription.subscriptionId)

    val paymentService = PaymentServiceMock.HasRegularPayments(
      equalTo(user.raw),
      value(false)
    )

    val subscriptionService = SearchSubscriptionServiceMock.GetOneBy(
      equalTo((user, subscription.subscriptionId)),
      value(Some(subscription))
    )

    val sender = ComebackMailSenderMock.empty
    val offerService = OfferServiceMock.empty
    val carfax = CarfaxMock.empty
    val comebackService = ComebackServiceMock.empty

    val effect = for {
      processor <- ZIO.service[SubscriptionNotifierEventProcessor.Service]
      _ <- processor.processMessages(List(event))
    } yield ()

    val env = baseEnv ++ paymentService ++ subscriptionService ++ sender ++ offerService ++ carfax ++ comebackService

    assertM(effect)(isUnit)
      .provideCustomLayer(env >>> SubscriptionNotifierEventProcessorImpl.live)
  }

  private val failedByDeletedSubscription = testM("Not send email if subscription was deleted") {
    val user = DealerId(1234L)
    val subscription = createSubscription(Status.DELETED, user)
    val event = createEvent("autoru_comeback", user.toString, subscription.subscriptionId)

    val paymentService = PaymentServiceMock.HasRegularPayments(
      equalTo(user.raw),
      value(true)
    )

    val subscriptionService = SearchSubscriptionServiceMock.GetOneBy(
      equalTo((user, subscription.subscriptionId)),
      value(Some(subscription))
    )

    val sender = ComebackMailSenderMock.empty
    val offerService = OfferServiceMock.empty
    val carfax = CarfaxMock.empty
    val comebackService = ComebackServiceMock.empty

    val effect = for {
      processor <- ZIO.service[SubscriptionNotifierEventProcessor.Service]
      _ <- processor.processMessages(List(event))
    } yield ()

    val env = baseEnv ++ paymentService ++ subscriptionService ++ sender ++ offerService ++ carfax ++ comebackService

    assertM(effect)(isUnit)
      .provideCustomLayer(env >>> SubscriptionNotifierEventProcessorImpl.live)
  }

  private val failedByUnknownEvent = testM("Not send email if event unknown") {
    val user = DealerId(1234L)
    val subscription = createSubscription(Status.ACTIVE, user)
    val event = createEvent("unknown_comeback", user.toString, subscription.subscriptionId)

    val paymentService = PaymentServiceMock.empty

    val subscriptionService = SearchSubscriptionServiceMock.empty

    val sender = ComebackMailSenderMock.empty
    val offerService = OfferServiceMock.empty
    val carfax = CarfaxMock.empty
    val comebackService = ComebackServiceMock.empty

    val effect = for {
      processor <- ZIO.service[SubscriptionNotifierEventProcessor.Service]
      _ <- processor.processMessages(List(event))
    } yield ()

    val env = baseEnv ++ paymentService ++ subscriptionService ++ sender ++ offerService ++ carfax ++ comebackService

    assertM(effect)(isUnit)
      .provideCustomLayer(env >>> SubscriptionNotifierEventProcessorImpl.live)
  }

  private val successfulCase = testM("send email if everything is okay") {
    checkM(ComebackGen.anyComeback, VosOfferGen.offer()) { (comeback, offer) =>
      val user = DealerId(1234L)
      val subscription = createSubscription(Status.ACTIVE, user)
      val event = createEvent("autoru_comeback", user.toString, subscription.subscriptionId)

      val creationTime = Instant.now.plusMillis(10.seconds.toMillis)

      val correctComeback =
        comeback.copy(
          offer = comeback.offer
            .copy(
              activated = creationTime,
              ref = comeback.offer.ref.copy(id = offer.id, category = offer.category),
              car = comeback.offer.car.copy(vin = offer.getDocuments.vin)
            ),
          clientId = subscription.clientId.raw
        )

      val report = RawVinEssentialsReport()
        .withVin(offer.getDocuments.vin)
        .withDtp(DtpBlock().withHeader(Header().withIsUpdating(true)))

      val paymentService = PaymentServiceMock
        .HasRegularPayments(
          equalTo(user.raw),
          value(true)
        )

      val subscriptionService = SearchSubscriptionServiceMock
        .GetOneBy(
          equalTo((user, subscription.subscriptionId)),
          value(Some(subscription))
        )

      val sender = ComebackMailSenderMock
        .SendEmails(
          equalTo(
            NotificationData(subscription = subscription, comeback = correctComeback, report = report, offer = offer)
          ),
          unit
        )

      val offerService = OfferServiceMock
        .GetOffer(
          equalTo((correctComeback.offer.ref, true)),
          value(Some(offer))
        )

      val carfax = CarfaxMock
        .GetEssentialsRawReport(
          equalTo(offer.getDocuments.vin),
          value(report)
        )

      val comebackService = ComebackServiceMock
        .GetByIds(
          equalTo(event.document.map(_.id.toLong).toList),
          value(List(correctComeback))
        )

      val effect = for {
        processor <- ZIO.service[SubscriptionNotifierEventProcessor.Service]
        _ <- processor.processMessages(List(event))
      } yield ()

      val env = baseEnv ++ paymentService ++ subscriptionService ++ sender ++ offerService ++ carfax ++ comebackService

      assertM(effect)(isUnit)
        .provideCustomLayer(env >>> SubscriptionNotifierEventProcessorImpl.live)
    }
  }
}
