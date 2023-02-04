package ru.yandex.vertis.incite.consumers.favorite_message_sender

import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.apache.kafka.clients.consumer.{ConsumerRecord, ConsumerRecords}
import org.apache.kafka.common.TopicPartition
import org.joda.time.DateTime
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.{BeforeAndAfterAll, WordSpec}
import ru.auto.api.ApiOfferModel.{Offer, OfferStatus}
import ru.auto.incite.api_model.ApiModel.FavoriteHistory.{FavoriteHistoryRecord, MessageHistoryRecord}
import ru.auto.incite.api_model.ApiModel.{FavoriteHistory, FavoriteMessageDiscountMoney, Message}
import ru.auto.incite.broker_events.BrokerEvents.ConsumerSendMessageEvent
import ru.yandex.incite.MessagesByUser.UserMessages
import ru.yandex.vertis.incite.utils.Protobuf.{RichDateTime, RichTimestamp}
import ru.yandex.vertis.baker.components.monitor.service.Monitor
import ru.yandex.vertis.baker.components.time.TimeService
import ru.yandex.vertis.baker.lifecycle.{Application, DefaultApplication}
import ru.yandex.vertis.baker.util.workmoments.WorkMoments
import ru.yandex.vertis.broker.client.simple.BrokerClient
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.incite.commonfeatures.FeaturesManager
import ru.yandex.vertis.incite.consumers.extdata.{DataService, FavoriteMessageInfo}
import ru.yandex.vertis.incite.dao.InciteDao
import ru.yandex.vertis.incite.models.FavoriteTableRow
import ru.yandex.vertis.incite.services.chats.{ChatClient, ChatManager}
import ru.yandex.vertis.incite.services.vos.VosClient
import ru.yandex.vertis.incite.utils.KafkaUtils
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.OperationalSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport
import ru.yandex.vertis.tracing.Traced

import java.util.Properties
import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class FavoriteMessageSenderConsumerTest extends WordSpec with BeforeAndAfterAll with MockitoSupport {

  private val kafkaPort = 1234
  implicit private val kafkaConfig = EmbeddedKafkaConfig(kafkaPort = kafkaPort)

  override def beforeAll() = {
    EmbeddedKafka.start()
  }

  override def afterAll() = {
    EmbeddedKafka.stop()
  }

  "create right message with hours" in {
    val dataService = new DataService {
      override val favoriteMessageInfo: FavoriteMessageInfo = FavoriteMessageInfo(
        mainMessage =
          "Позвоните по{nbsp}объявлению{expires_text}, чтобы получить {discount_message}.\n{secret_text}Предложение действует, пока не{nbsp}найдётся первый покупатель.",
        discountMoneyText = "скидку {discount_money} ₽",
        discountPercentText = "скидку {discount_percent} %",
        presentText = "в подарок",
        secretText = "Скажите менеджеру кодовое слово «{secret_word}».\n",
        expiresText = " в{nbsp}течение {hours} часов"
      )
    }
    val message = Message
      .newBuilder()
      .setFavoriteMessageDiscountMoney(
        FavoriteMessageDiscountMoney.newBuilder().setDiscount(100).setExpireInHours(10).setKeyword("бурундук")
      )
    val resultMessage = FavoriteMessageSenderConsumer.getMessageText(message.build(), dataService)
    println(resultMessage)
    assert(
      resultMessage ==
        """Позвоните по объявлению в течение 10 часов, чтобы получить скидку 100 ₽.
            |Скажите менеджеру кодовое слово «бурундук».
            |Предложение действует, пока не найдётся первый покупатель.""".stripMargin
    )

  }
  "create right message without hours" in {
    val dataService = new DataService {
      override val favoriteMessageInfo: FavoriteMessageInfo = FavoriteMessageInfo(
        mainMessage =
          "Позвоните по{nbsp}объявлению{expires_text}, чтобы получить {discount_message}.\n{secret_text}Предложение действует, пока не{nbsp}найдётся первый покупатель.",
        discountMoneyText = "скидку {discount_money} ₽",
        discountPercentText = "скидку {discount_percent} %",
        presentText = "в подарок",
        secretText = "Скажите менеджеру кодовое слово «{secret_word}».\n",
        expiresText = " в{nbsp}течение {hours} часов"
      )
    }
    val message = Message
      .newBuilder()
      .setFavoriteMessageDiscountMoney(
        FavoriteMessageDiscountMoney.newBuilder().setDiscount(100).setKeyword("бурундук")
      )
    val resultMessage = FavoriteMessageSenderConsumer.getMessageText(message.build(), dataService)
    println(resultMessage)
    assert(
      resultMessage ==
        """Позвоните по объявлению, чтобы получить скидку 100 ₽.
            |Скажите менеджеру кодовое слово «бурундук».
            |Предложение действует, пока не найдётся первый покупатель.""".stripMargin
    )

  }

  val dataService = new DataService {

    override val favoriteMessageInfo: FavoriteMessageInfo = FavoriteMessageInfo(
      mainMessage =
        "Позвоните по{nbsp}объявлению{expires_text}, чтобы получить {discount_message}.\n{secret_text}Предложение действует, пока не{nbsp}найдётся первый покупатель.",
      discountMoneyText = "скидку {discount_money} ₽",
      discountPercentText = "скидку {discount_percent} %",
      presentText = "в подарок",
      secretText = "Скажите менеджеру кодовое слово «{secret_word}».\n",
      expiresText = " в{nbsp}течение {hours} часов"
    )
  }

  "sent message when have less then maxCount" in {
    val mockedDao = mock[InciteDao]

    stub(
      mockedDao.updateUserMessages[Boolean](_: String)(_: UserMessages => (Option[UserMessages], Boolean))(_: Traced)
    ) {
      case (userId, func, _) =>
        true
    }

    val mockedFeatureManager = mock[FeaturesManager]
    val mockedFeature = mock[Feature[Int]]
    when(mockedFeatureManager.MaxMessagesCount).thenReturn(mockedFeature)

    when(mockedFeature.value).thenReturn(2)
    val mockedMonitor = mock[Monitor]
    doNothing().when(mockedMonitor).gauge(?, ?, ?)(?, ?)(?)

    val mockedApplication = mock[Application]
    doNothing().when(mockedApplication).onStop(?)

    val testOfferId = "testOfferId"
    val testOwnerId = "ownerId"
    val testUserId = "userId"

    when(mockedDao.incrementSendCounters(?, ?, ?, ?, ?)(?)).thenReturn(true)

    when(mockedDao.getFavoriteTableRow(?, ?)(?)).thenReturn(
      Some(
        FavoriteTableRow(
          testOfferId,
          testOwnerId,
          testUserId,
          canSend = true,
          None,
          FavoriteHistory.newBuilder().build(),
          1,
          deleted = false
        )
      )
    )

    val mockedVos = mock[VosClient]

    val testOffer = Offer.newBuilder().setStatus(OfferStatus.ACTIVE).setId(testOfferId).build()
    when(mockedVos.getOffer(?, ?)(?)).thenReturn(Future.successful(Some(testOffer)))

    val properties = new Properties()
    properties.setProperty("bootstrap.servers", s"localhost:$kafkaPort")
    properties.setProperty("group.id", "test")
    properties.setProperty("topic", "test")

    val mockedChat = mock[ChatClient]

    when(mockedChat.getRoomById(?, ?, ?, ?)(?)).thenReturn(Future.successful(None))
    when(mockedChat.hideRoom(?, ?, ?, ?)(?)).thenReturn(Future.successful())
    when(mockedChat.sendMessage(?, ?, ?, ?, ?, ?, ?, ?)(?)).thenReturn(Future.successful())
    val chatManager = new ChatManager(mockedChat)

    val mockedBrokerClient = mock[BrokerClient]

    when(mockedBrokerClient.send(?)(?)).thenReturn(Future.successful())

    val sender = new FavoriteMessageSenderConsumer(
      KafkaUtils.KafkaConfig(consumerProps = properties, consumerTopic = "topic"),
      WorkMoments.every(1.minute)(new TimeService {
        override def getNow: DateTime = DateTime.now
      }),
      mockedVos,
      mockedDao,
      mockedBrokerClient,
      chatManager,
      new DefaultApplication {},
      mockedFeatureManager,
      dataService
    ) {
      override def operational: OperationalSupport = TestOperationalSupport

      override def shouldWork: Boolean = ???

      override def start(): Unit = ???

      override def stop(): Unit = ???

      override def monitor: Monitor = mockedMonitor

    }

    val message = Message
      .newBuilder()
      .setFavoriteMessageDiscountMoney(
        FavoriteMessageDiscountMoney.newBuilder().setDiscount(100).setKeyword("бурундук")
      )
      .build()

    when(mockedDao.getMessageForOffer(?)(?)).thenReturn(Some(message))

    val sendMessageEvent = ConsumerSendMessageEvent
      .newBuilder()
      .setMessage(message)
      .setTimestamp(DateTime.now().toProtobufTimestamp)
      .setOfferId(testOfferId)
      .setOwnerId(testOwnerId)
      .build()

    val partition = new TopicPartition("testTopic", 1)
    val record = new ConsumerRecord(partition.topic(), partition.partition(), 2131, "someKey", sendMessageEvent)
    val map = Map(partition -> Seq(record).asJava)
    val records = new ConsumerRecords(map.asJava)

    val res = sender.processRecordsWithResult(records).head.futureValue
    res()

    verify(mockedChat, times(1)).sendMessage(?, ?, ?, ?, ?, ?, ?, ?)(?)
  }
  "dont send message when have more then maxCount" in {
    val mockedDao = mock[InciteDao]

    stub(
      mockedDao.updateUserMessages[Boolean](_: String)(_: UserMessages => (Option[UserMessages], Boolean))(_: Traced)
    ) {
      case (userId, func, _) =>
        false
    }
    val message = Message
      .newBuilder()
      .setFavoriteMessageDiscountMoney(
        FavoriteMessageDiscountMoney.newBuilder().setDiscount(100).setKeyword("бурундук")
      )
      .build()

    val mockedFeatureManager = mock[FeaturesManager]
    val mockedFeature = mock[Feature[Int]]
    when(mockedFeatureManager.MaxMessagesCount).thenReturn(mockedFeature)

    when(mockedFeature.value).thenReturn(2)
    val mockedMonitor = mock[Monitor]
    doNothing().when(mockedMonitor).gauge(?, ?, ?)(?, ?)(?)

    val mockedApplication = mock[Application]
    doNothing().when(mockedApplication).onStop(?)

    val testOfferId = "testOfferId"
    val testOwnerId = "ownerId"
    val testUserId = "userId"

    when(mockedDao.incrementSendCounters(?, ?, ?, ?, ?)(?)).thenReturn(true)

    val favoriteHistoryBuilder = FavoriteHistory.newBuilder()

    for (idx <- 1 to 16) {
      val messageHistoryRecord = MessageHistoryRecord
        .newBuilder()
        .setMessage(message)
        .setTimestamp(DateTime.now().minusHours(idx).toProtobufTimestamp)

      val historyRecord = FavoriteHistoryRecord
        .newBuilder()
        .setMessageHistoryRecord(messageHistoryRecord.build())
      favoriteHistoryBuilder.addFavoriteHistory(historyRecord)
    }

    when(mockedDao.getFavoriteTableRow(?, ?)(?)).thenReturn(
      Some(
        FavoriteTableRow(
          testOfferId,
          testOwnerId,
          testUserId,
          canSend = true,
          None,
          favoriteHistoryBuilder
            .build(),
          1,
          deleted = false
        )
      )
    )

    val mockedVos = mock[VosClient]

    val testOffer = Offer.newBuilder().setStatus(OfferStatus.ACTIVE).setId(testOfferId).build()
    when(mockedVos.getOffer(?, ?)(?)).thenReturn(Future.successful(Some(testOffer)))

    val properties = new Properties()
    properties.setProperty("bootstrap.servers", s"localhost:$kafkaPort")
    properties.setProperty("group.id", "test")
    properties.setProperty("topic", "test")

    val mockedChat = mock[ChatClient]

    when(mockedChat.getRoomById(?, ?, ?, ?)(?)).thenReturn(Future.successful(None))
    when(mockedChat.hideRoom(?, ?, ?, ?)(?)).thenReturn(Future.successful())
    when(mockedChat.sendMessage(?, ?, ?, ?, ?, ?, ?, ?)(?)).thenReturn(Future.successful())
    val chatManager = new ChatManager(mockedChat)

    val mockedBrokerClient = mock[BrokerClient]

    when(mockedBrokerClient.send(?)(?)).thenReturn(Future.successful())

    val sender = new FavoriteMessageSenderConsumer(
      KafkaUtils.KafkaConfig(consumerProps = properties, consumerTopic = "topic"),
      WorkMoments.every(1.minute)(new TimeService {
        override def getNow: DateTime = DateTime.now
      }),
      mockedVos,
      mockedDao,
      mockedBrokerClient,
      chatManager,
      new DefaultApplication {},
      mockedFeatureManager,
      dataService
    ) {
      override def operational: OperationalSupport = TestOperationalSupport

      override def shouldWork: Boolean = ???

      override def start(): Unit = ???

      override def stop(): Unit = ???

      override def monitor: Monitor = mockedMonitor
    }

    when(mockedDao.getMessageForOffer(?)(?)).thenReturn(Some(message))

    val sendMessageEvent = ConsumerSendMessageEvent
      .newBuilder()
      .setMessage(message)
      .setTimestamp(DateTime.now().toProtobufTimestamp)
      .setOfferId(testOfferId)
      .setOwnerId(testOwnerId)
      .build()

    val partition = new TopicPartition("testTopic", 1)
    val record = new ConsumerRecord(partition.topic(), partition.partition(), 2131, "someKey", sendMessageEvent)
    val map = Map(partition -> Seq(record).asJava)
    val records = new ConsumerRecords(map.asJava)

    val res = sender.processRecordsWithResult(records).head.futureValue
    res()

    verify(mockedChat, times(0)).sendMessage(?, ?, ?, ?, ?, ?, ?, ?)(?)
  }

  "send message when 1 sent before" in {
    val mockedDao = mock[InciteDao]

    stub(
      mockedDao.updateUserMessages[Boolean](_: String)(_: UserMessages => (Option[UserMessages], Boolean))(_: Traced)
    ) {
      case (userId, func, _) =>
        true
    }
    val message = Message
      .newBuilder()
      .setFavoriteMessageDiscountMoney(
        FavoriteMessageDiscountMoney.newBuilder().setDiscount(100).setKeyword("бурундук")
      )
      .build()

    val mockedFeatureManager = mock[FeaturesManager]
    val mockedFeature = mock[Feature[Int]]
    when(mockedFeatureManager.MaxMessagesCount).thenReturn(mockedFeature)
    doNothing().when(mockedDao).unblockCounters(?)(?)

    when(mockedFeature.value).thenReturn(2)
    val mockedMonitor = mock[Monitor]
    doNothing().when(mockedMonitor).gauge(?, ?, ?)(?, ?)(?)

    val mockedApplication = mock[Application]
    doNothing().when(mockedApplication).onStop(?)

    val testOfferId = "testOfferId"
    val testOwnerId = "ownerId"
    val testUserId = "userId"

    when(mockedDao.incrementSendCounters(?, ?, ?, ?, ?)(?)).thenReturn(true)

    val favoriteHistoryBuilder = FavoriteHistory.newBuilder()

    val messageHistoryRecord = MessageHistoryRecord
      .newBuilder()
      .setMessage(message)
      .setTimestamp(DateTime.now().minusHours(1).toProtobufTimestamp)

    val historyRecord = FavoriteHistoryRecord
      .newBuilder()
      .setMessageHistoryRecord(messageHistoryRecord.build())
    favoriteHistoryBuilder.addFavoriteHistory(historyRecord)

    when(mockedDao.getFavoriteTableRow(?, ?)(?)).thenReturn(
      Some(
        FavoriteTableRow(
          testOfferId,
          testOwnerId,
          testUserId,
          canSend = true,
          None,
          favoriteHistoryBuilder
            .build(),
          1,
          deleted = false
        )
      )
    )

    val mockedVos = mock[VosClient]

    val testOffer = Offer.newBuilder().setStatus(OfferStatus.ACTIVE).setId(testOfferId).build()
    when(mockedVos.getOffer(?, ?)(?)).thenReturn(Future.successful(Some(testOffer)))

    val properties = new Properties()
    properties.setProperty("bootstrap.servers", s"localhost:$kafkaPort")
    properties.setProperty("group.id", "test")
    properties.setProperty("topic", "test")

    val mockedChat = mock[ChatClient]

    when(mockedChat.getRoomById(?, ?, ?, ?)(?)).thenReturn(Future.successful(None))
    when(mockedChat.hideRoom(?, ?, ?, ?)(?)).thenReturn(Future.successful())
    when(mockedChat.sendMessage(?, ?, ?, ?, ?, ?, ?, ?)(?)).thenReturn(Future.successful())
    val chatManager = new ChatManager(mockedChat)

    val mockedBrokerClient = mock[BrokerClient]

    when(mockedBrokerClient.send(?)(?)).thenReturn(Future.successful())

    val sender = new FavoriteMessageSenderConsumer(
      KafkaUtils.KafkaConfig(consumerProps = properties, consumerTopic = "topic"),
      WorkMoments.every(1.minute)(new TimeService {
        override def getNow: DateTime = DateTime.now
      }),
      mockedVos,
      mockedDao,
      mockedBrokerClient,
      chatManager,
      new DefaultApplication {},
      mockedFeatureManager,
      dataService
    ) {
      override def operational: OperationalSupport = TestOperationalSupport

      override def shouldWork: Boolean = ???

      override def start(): Unit = ???

      override def stop(): Unit = ???

      override def monitor: Monitor = mockedMonitor
    }

    when(mockedDao.getMessageForOffer(?)(?)).thenReturn(Some(message))

    val sendMessageEvent = ConsumerSendMessageEvent
      .newBuilder()
      .setMessage(message)
      .setTimestamp(DateTime.now().toProtobufTimestamp)
      .setOfferId(testOfferId)
      .setOwnerId(testOwnerId)
      .build()

    val partition = new TopicPartition("testTopic", 1)
    val record = new ConsumerRecord(partition.topic(), partition.partition(), 2131, "someKey", sendMessageEvent)
    val map = Map(partition -> Seq(record).asJava)
    val records = new ConsumerRecords(map.asJava)

    val res = sender.processRecordsWithResult(records).head.futureValue
    res()

    verify(mockedChat, times(1)).sendMessage(?, ?, ?, ?, ?, ?, ?, ?)(?)
  }

}
