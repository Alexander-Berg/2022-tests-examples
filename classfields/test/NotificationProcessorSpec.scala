package auto.dealers.trade_in_notifier.logic

import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

import com.google.protobuf.ByteString
import common.geobase.model.RegionIds.RegionId
import common.zio.doobie.schema.InitSchema
import common.zio.doobie.testkit.TestPostgresql
import doobie.implicits._
import doobie.util.transactor.Transactor
import auto.dealers.trade_in_notifier.logic.NotificationProcessor._
import auto.dealers.trade_in_notifier.model.{DealerId, SimplifiedOffer, TradeInSubsDomain}
import auto.dealers.trade_in_notifier.model.SimplifiedOffer.PriceRange
import auto.dealers.trade_in_notifier.storage.TradeInMQRepository.ResultRecord
import auto.dealers.trade_in_notifier.storage.postgres.{
  PgNotificationLogRepository,
  PgOfferLogRepository,
  PgTradeInMQRepository
}
import auto.dealers.trade_in_notifier.storage.{DealerInfoRepository, OfferLogRepository, TradeInMQRepository}
import ru.yandex.vertis.subscriptions.model.MatchedDocument
import ru.yandex.vertis.subscriptions.model.notifier.broker.BrokerEvent
import common.zio.logging.Logging
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.interop.catz._
import zio.test.Assertion._
import zio.test.TestAspect.{after, sequential}
import zio.test._

object NotificationProcessorSpec extends DefaultRunnableSpec {

  private val simpleCarInfo = SimplifiedOffer.SimplifiedCarInfo(
    mark = "BMW",
    model = "X5",
    body = "ALLROAD_5_DOORS",
    engine = "DIESEL",
    transmission = "AUTOMATIC",
    drive = "ALL",
    superGenId = Some(42L),
    configurationId = Some(43L),
    techParamId = Some(44L),
    complectationId = Some(45L)
  )

  private val offer = SimplifiedOffer(
    name = "Bob",
    phones = List("79111234567"),
    location = RegionId(0),
    year = 1998,
    mileage = 50000,
    info = simpleCarInfo,
    color = "0x5a5a5a",
    ownersNumber = 1,
    tradeInType = "FOR_NEW",
    priceRange = PriceRange(BigDecimal(300000), BigDecimal(330000)),
    currency = "RUB",
    offerId = Some("0123-4567")
  )

  private val now = OffsetDateTime
    .now()
    .truncatedTo(ChronoUnit.DAYS)
    .plusHours(1)

  private val successfullyProcessMessage = testM("successfully process incoming message user: 1") {
    for {
      ref <- Ref.make[Option[ResultRecord]](None)
      uuid <- OfferLogRepository.append(offer, now)
      event <- mockEvent(uuid.toString)
      _ <- NotificationProcessor.process(event)
      _ <- TradeInMQRepository.drainN(1, rs => ref.set(Some(rs)))
      result <- ref.get
    } yield assert(result)(isSome) && assert(result.map(_.email))(isSome(equalTo("test@test.ru")))
  }

  private val emptyDocumentList = testM("empty document list") {
    for {
      ref <- Ref.make[Option[ResultRecord]](None)
      uuid <- OfferLogRepository.append(offer, now)
      event <- mockEvent(uuid.toString).map(_.copy(document = Seq.empty))
      status <- NotificationProcessor.process(event).run
      _ <- TradeInMQRepository.drainN(1, rs => ref.set(Some(rs)))
      result <- ref.get
    } yield assert(result)(isNone)
  }

  private val incorrectDealerIdFormat = testM("incorrect dealer id format") {
    for {
      ref <- Ref.make[Option[ResultRecord]](None)
      uuid <- OfferLogRepository.append(offer, now)
      event <- mockEvent(uuid.toString).map(_.copy(user = "incorrect"))
      status <- NotificationProcessor.process(event).run
      _ <- TradeInMQRepository.drainN(1, rs => ref.set(Some(rs)))
      result <- ref.get
    } yield assert(result)(isNone) && assert(status)(fails(equalTo(UnexpectedUserIdFormat("incorrect"))))
  }

  private val incorrectUuidFormat = testM("incorrect uuid format") {
    for {
      ref <- Ref.make[Option[ResultRecord]](None)
      event <- mockEvent("incorrect-uuid").map(_.copy(user = "incorrect"))
      status <- NotificationProcessor.process(event).run
      _ <- TradeInMQRepository.drainN(1, rs => ref.set(Some(rs)))
      result <- ref.get
    } yield assert(result)(isNone) && assert(status)(fails(equalTo(IncorrectUuid("incorrect-uuid"))))
  }

  private val foreignKeyConstraintViolation = testM("foreign key constraint violation") {
    for {
      ref <- Ref.make[Option[ResultRecord]](None)
      event <- mockEvent(UUID.randomUUID().toString)
      status <- NotificationProcessor.process(event).run
      _ <- TradeInMQRepository.drainN(1, rs => ref.set(Some(rs)))
      result <- ref.get
    } yield assert(result)(isNone)
  }

  private def mockEvent(uuid: String): UIO[BrokerEvent] = ZIO.succeed(
    BrokerEvent(
      timestamp = None,
      service = TradeInSubsDomain,
      qualifier = "qualifier",
      user = "dealer:1",
      subscriptionId = "id",
      document = Seq(
        MatchedDocument(
          id = uuid,
          rawContent = ByteString.EMPTY
        )
      )
    )
  )

  private def createTestSpecificEnv(
      emails: Seq[String]) = {
    val transactor = ZLayer.requires[Has[Transactor[Task]]]
    val clock = Clock.live
    val blocking = Blocking.live
    val logging = Logging.live

    val dealerInfoRepository = ZLayer.succeed(new DealerInfoRepository.Service {
      override def tradeInEmails(dealerId: DealerId): ZIO[Any, DealerInfoRepository.Err, Seq[String]] =
        ZIO.succeed(emails)
    })

    val dao =
      transactor >>> (PgNotificationLogRepository.live ++ PgOfferLogRepository.live ++ PgTradeInMQRepository.live)

    val processor = NotificationProcessor.live

    dealerInfoRepository ++
      dao >+> processor ++
      blocking ++
      clock ++
      logging
  }

  private val dbClean = ZIO
    .service[Transactor[Task]]
    .flatMap { xa =>
      sql"DELETE FROM trade_in_notification_buffer".update.run.transact(xa) *>
        sql"DELETE FROM trade_in_notification_log".update.run.transact(xa) *>
        sql"DELETE FROM trade_in_offer_log".update.run.transact(xa)
    }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    (suite("NotificationProcessor")(
      successfullyProcessMessage.provideLayer(createTestSpecificEnv(emails = Seq("test@test.ru"))),
      incorrectDealerIdFormat.provideLayer(createTestSpecificEnv(emails = Seq("test@test.ru"))),
      incorrectUuidFormat.provideLayer(createTestSpecificEnv(emails = Seq("test@test.ru"))),
      emptyDocumentList.provideLayer(createTestSpecificEnv(emails = Seq("test@test.ru"))),
      foreignKeyConstraintViolation.provideLayer(createTestSpecificEnv(emails = Seq("test@test.ru")))
    ) @@ sequential @@ after(dbClean.orDie)).provideSomeLayerShared {
      val transactor = Blocking.live >>> TestPostgresql.managedTransactor

      val initSchema = transactor >>> (
        for {
          tx <- ZIO.service[Transactor[Task]]
          _ <- InitSchema("/schema.sql", tx)
        } yield ()
      ).orDie.toLayer

      transactor ++ initSchema
    }
}
