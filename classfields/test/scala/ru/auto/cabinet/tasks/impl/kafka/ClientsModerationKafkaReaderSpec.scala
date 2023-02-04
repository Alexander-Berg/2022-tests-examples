package ru.auto.cabinet.tasks.impl.kafka

import akka.actor.Scheduler
import com.google.protobuf.Timestamp
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.consumer.{
  ConsumerRecord,
  MockConsumer,
  OffsetResetStrategy
}
import org.apache.kafka.common.TopicPartition
import ru.auto.cabinet.dao.jdbc.{
  ClientsChangedBufferDao,
  JdbcClientDao,
  JdbcPremoderationBufferDao
}
import ru.auto.cabinet.model.{
  Client,
  ClientId,
  ClientProperties,
  ClientStatus,
  ClientStatuses
}
import ru.auto.cabinet.service.instr.EmptyInstr
import ru.auto.cabinet.service.moderation.ModerationService
import ru.auto.cabinet.test.ScalamockCallHandlers
import ru.auto.cabinet.trace.Context
import ru.auto.cabinet.{environment, TestActorSystem}
import ru.yandex.vertis.application.runtime.VertisRuntime
import ru.yandex.vertis.moderation.proto.Model.Metadata.DealerMetadata
import ru.yandex.vertis.moderation.proto.Model.Metadata.DealerMetadata.LoyaltyInfo
import ru.yandex.vertis.moderation.proto.Model.Metadata.DealerMetadata.LoyaltyInfo.Loyal
import ru.yandex.vertis.moderation.proto.Model.Opinions.Entry
import ru.yandex.vertis.moderation.proto.Model._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future, TimeoutException}
import scala.jdk.CollectionConverters._

class ClientsModerationKafkaReaderSpec
    extends KafkaSpecBase
    with ScalamockCallHandlers
    with TestActorSystem {
  import Fixture._

  implicit private val scheduler: Scheduler = system.scheduler
  implicit private val instr: EmptyInstr = new EmptyInstr("test")

  private val consumer =
    new MockConsumer[String, UpdateJournalRecord](OffsetResetStrategy.EARLIEST)

  "ClientsModerationKafkaListener" should {

    System.setProperty("config.resource", "test.conf")

    val premoderationBufferDao = mock[JdbcPremoderationBufferDao]
    val clientsChangedBufferDao = mock[ClientsChangedBufferDao]
    val moderationService = mock[ModerationService]

    val clientDao = mock[JdbcClientDao]
    val kafkaConfig =
      ClientsModerationKafkaConfig(
        ConfigFactory.load().getConfig("kafka"),
        VertisRuntime)
    val topic = kafkaConfig.topic

    val partition = new TopicPartition(topic, 0)
    consumer.subscribe(List(topic).asJava)
    consumer.rebalance(List(partition).asJava)
    consumer.updateBeginningOffsets(
      Map(partition -> java.lang.Long.valueOf(0)).asJava)

    val clientsModerationReader: ClientsModerationKafkaReader =
      new ClientsModerationKafkaReader(consumer, kafkaConfig)

    val listener =
      new ClientModerationListener(
        clientsModerationReader,
        clientDao,
        premoderationBufferDao,
        clientsChangedBufferDao,
        moderationService)

    "consume USER_BANNED Reason from topic" in {
      consumer.addRecord(consumerRecord(topic, Reason.USER_BANNED, 1))
      (clientDao
        .appendModerationLog(_: ClientId, _: String)(_: Context))
        .expects(*, *, *)
        .anyNumberOfTimes()
        .returning(Future.successful(0))

      (clientDao
        .get(_: ClientId)(_: Context))
        .expects(1L, *)
        .returning(Future.successful(client))
      (clientDao
        .updateStatus(_: Client, _: ClientStatus)(_: Context))
        .expects(*, ClientStatuses.Freeze, *)
        .returning(Future.successful(null))
        .once()

      try Await.ready(listener.execute(), 1.second)
      catch { case _: TimeoutException => }
    }

    "consume NOT_VERIFIED Reason from topic" in {
      pending // todo implement
      consumer.addRecord(consumerRecord(topic, Reason.NOT_VERIFIED, 2))
      (clientDao
        .get(_: ClientId)(_: Context))
        .expects(2L, *)
        .returning(Future.successful(client))

      (clientDao
        .updateFirstModerated(_: ClientId, _: Boolean)(_: Context))
        .expects(*, false, *)
        .returning(Future.successful(0))
        .once()

      try Await.ready(listener.execute(), 1.second)
      catch { case _: TimeoutException => }
    }

    "consume ANOTHER Reason from topic for stopped client" in {
      pending // todo implement
      val stoppedClient = client.copy(
        properties = client.properties.copy(status = ClientStatuses.Stopped))
      consumer.addRecord(consumerRecord(topic, Reason.ANOTHER, 3))
      (clientDao
        .get(_: ClientId)(_: Context))
        .expects(3L, *)
        .returningF(stoppedClient)
      (clientDao
        .updateStatus(_: Client, _: ClientStatus)(_: Context))
        .expects(*, ClientStatuses.Active, *)
        .never()
      (clientDao
        .updateFirstModerated(_: ClientId, _: Boolean)(_: Context))
        .expects(*, true, *)
        .returningF(0)
      (clientDao
        .appendModerationLog(_: ClientId, _: String)(_: Context))
        .expects(*, "STOPPED", *)
        .returningF(0)

      try Await.ready(listener.execute(), 1.second)
      catch { case _: TimeoutException => }
    }

    "consume ANOTHER Reason from topic" in {
      pending // todo implement
      consumer.addRecord(consumerRecord(topic, Reason.ANOTHER, 3))
      (clientDao
        .get(_: ClientId)(_: Context))
        .expects(3L, *)
        .returning(Future.successful(client))
      (clientDao
        .updateStatus(_: Client, _: ClientStatus)(_: Context))
        .expects(*, ClientStatuses.Active, *)
        .once()

      try Await.ready(listener.execute(), 1.second)
      catch { case _: TimeoutException => }
    }
  }

}

object Fixture {

  val client = Client(
    123,
    properties = ClientProperties(
      0,
      1,
      "test",
      ClientStatuses.Active,
      environment.now,
      "",
      Some("website.test"),
      "test@yandex.ru",
      Some("manager@yandex.ru"),
      None,
      None,
      multipostingEnabled = true,
      firstModerated = false,
      isAgent = false
    )
  )

  def consumerRecord(topic: String, reason: Reason, id: Long) =
    new ConsumerRecord(topic, 0, id, "", kafkaRecord(reason, id))

  def kafkaRecord(reason: Reason, dealerUser: Long): UpdateJournalRecord = {
    val opinion =
      Opinion.newBuilder().setVersion(0).addReasons(reason).build()
    val entry = Entry
      .newBuilder()
      .setVersion(0)
      .setOpinion(opinion)
      .build()
    val opinions =
      Opinions.newBuilder().setVersion(0).addEntries(entry).build()
    val user = User
      .newBuilder()
      .setVersion(0)
      .setDealerUser(dealerUser.toString)
      .build()
    val externalId =
      ExternalId.newBuilder().setVersion(0).setUser(user).build()
    val metadata = Metadata
      .newBuilder()
      .setDealerMetadata(
        DealerMetadata
          .newBuilder()
          .setForPeriod(
            LoyaltyInfo
              .newBuilder()
              .setDay(Timestamp.newBuilder().setNanos(0).setSeconds(0))
              .setLoyal(Loyal.newBuilder().setLevel(1)))
          .build())
    val instance =
      Instance
        .newBuilder()
        .setVersion(0)
        .setHashVersion(0)
        .setOpinions(opinions)
        .setExternalId(externalId)
        .addMetadata(metadata)
        .build()
    val record = UpdateJournalRecord
      .newBuilder()
      .setVersion(0)
      .setInstance(instance)
      .build()

    record
  }
}
