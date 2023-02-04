package ru.yandex.vertis.feedprocessor.autoru.scheduler.pipeline

import akka.Done
import akka.kafka.ConsumerMessage.{CommittableMessage, CommittableOffset, PartitionOffset}
import akka.kafka.testkit.ConsumerResultFactory
import akka.stream.scaladsl.Sink
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.mockito.Mockito._
import org.scalacheck.Gen
import play.api.libs.json.Json
import ru.auto.api.ApiOfferModel.OfferStatus
import ru.auto.feedprocessor.FeedprocessorModel.{Entity, MessageType, OffersResponse, UpdateStatus}
import ru.yandex.common.tokenization.TokensDistribution
import ru.yandex.vertis.feature.model.{Feature, FeatureRegistry}
import ru.yandex.vertis.feedprocessor.app.TestApplication
import ru.yandex.vertis.feedprocessor.autoru.app.{CoreComponents, UtilizatorService}
import ru.yandex.vertis.feedprocessor.autoru.dao.MainOffice7Dao.Client
import ru.yandex.vertis.feedprocessor.autoru.dao.{MainOffice7Dao, TasksDao}
import ru.yandex.vertis.feedprocessor.autoru.model
import ru.yandex.vertis.feedprocessor.autoru.model.Generators._
import ru.yandex.vertis.feedprocessor.autoru.model.ModelUtils._
import ru.yandex.vertis.feedprocessor.autoru.model.{OfferAnswers, Task, TaskContext}
import ru.yandex.vertis.feedprocessor.autoru.scheduler.app.SchedulerComponents
import ru.yandex.vertis.feedprocessor.autoru.scheduler.model.AutoruExternalOffer
import ru.yandex.vertis.feedprocessor.autoru.scheduler.pipeline.AutoruAnswerPipeline.AnswersMessage
import ru.yandex.vertis.feedprocessor.autoru.scheduler.pipeline.loader.Loader
import ru.yandex.vertis.feedprocessor.autoru.scheduler.pipeline.mapper.MapperFlow
import ru.yandex.vertis.feedprocessor.autoru.scheduler.pipeline.parser.Parser
import ru.yandex.vertis.feedprocessor.autoru.scheduler.pipeline.picker.TaskPicker
import ru.yandex.vertis.feedprocessor.autoru.scheduler.services.cabinet.CabinetClient
import ru.yandex.vertis.feedprocessor.autoru.scheduler.services.salesman.SalesmanClient.{Goods, GoodsRequest, Products, _}
import ru.yandex.vertis.feedprocessor.autoru.scheduler.services.salesman.{OfferBatch, SalesmanClient, SalesmanService}
import ru.yandex.vertis.feedprocessor.util.{DummyOpsSupport, StreamTestBase}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => argEq}

import java.lang
import java.time.LocalTime
import java.util.concurrent.CompletionStage
import scala.concurrent.Future

/**
  * @author pnaydenov
  */
class AutoruAnswerPipelineTest extends StreamTestBase with MockitoSupport {

  val config = ConfigFactory.parseString("""
      |kafka {
      |  from-vos {
      |    host = "test-host.ru"
      |    group_id = "test-group"
      |    topic = "test-topic"
      |  }
      |}
    """.stripMargin)

  val committableOffset = ConsumerResultFactory.committableOffset("","", 0, 0L, "")

  def buildAnswerMessage(taskContext: TaskContext): CommittableMessage[lang.Long, OfferAnswers[OffersResponse]] = {
    val responseBuilder = OffersResponse.newBuilder().setTimestamp(1).setType(MessageType.OFFER_STREAM_BATCH)
    responseBuilder
      .addEntities(
        Entity
          .newBuilder()
          .setPosition(0)
          .setStatus(UpdateStatus.UPDATE)
          .setOfferId("1071967714-948c3")
          .setOfferStatus(OfferStatus.ACTIVE)
          .addBadges("FOO")
      )
      .addEntities(
        Entity
          .newBuilder()
          .setPosition(1)
          .setStatus(UpdateStatus.UPDATE)
          .setOfferId("1072717118-d4bbef52")
          .setOfferStatus(OfferStatus.ACTIVE)
          .addActiveServices("special")
          .addBadges("BAR")
      )
      .addEntities(
        Entity
          .newBuilder()
          .setPosition(2)
          .setStatus(UpdateStatus.UPDATE)
          .setOfferId("1072717120-1dbb6a1e")
          .setOfferStatus(OfferStatus.ACTIVE)
      )

    val answerPayload = OfferAnswers[OffersResponse](responseBuilder.build(), Some(taskContext))
    CommittableMessage(
      new ConsumerRecord[java.lang.Long, OfferAnswers[OffersResponse]]("", 0, 1L, 123L, answerPayload),
      committableOffset
    )
  }

  trait Fixture {
    lazy val task = tasksGen(Gen.const(Task.Status.Processing), serviceInfoGen()).next
    val taskContext = model.TaskContext(task)
    val components = mock[CoreComponents]
    val autoruComponents = mock[SchedulerComponents]

    val tasksDao = mock[TasksDao]
    val salesmanClient = mock[SalesmanClient]
    val cabinetClient = mock[CabinetClient]
    val featureRegistry = mock[FeatureRegistry]
    val office7Dao = mock[MainOffice7Dao]
    lazy val pipeline = new TestAutoruAnswerPipeline(components, autoruComponents)

    private val emptyStringFeature = new Feature[String] {
      override val name = ""
      override def value = ""
    }

    when(components.appConf).thenReturn(config)
    when(components.tasksDao).thenReturn(tasksDao)
    when(components.featureRegistry).thenReturn(featureRegistry)
    when(autoruComponents.coreComponents).thenReturn(components)
    when(autoruComponents.salesmanClient).thenReturn(salesmanClient)
    when(autoruComponents.cabinetClient).thenReturn(cabinetClient)
    when(autoruComponents.coreComponents.mainOffice7Dao).thenReturn(office7Dao)
    when(featureRegistry.register[String](argEq("regions-with-disabled-turbo-cars-used"), ?, ?, ?)(?))
      .thenReturn(emptyStringFeature)
    when(featureRegistry.register[String](argEq("regions-with-disabled-premium-and-boost-cars-used"), ?, ?, ?)(?))
      .thenReturn(emptyStringFeature)
  }

  "AutoruAnswerPipeline" should {
    "send salesman requests correctly in case of cabinet api failure" in new Fixture {
      val goods = Json.parse(getClass.getResourceAsStream("/salesman_goods.json")).as[Seq[Goods]]
      when(office7Dao.getClientsByIds(?)).thenReturn(Seq(Client(4, 4, "", None, 123213)))
      when(salesmanClient.getGoods(?, ?)).thenReturn(Future.successful(goods))
      when(salesmanClient.getCampaigns(?, ?, ?)).thenReturn(Future.successful(Seq.empty[Campaign]))
      when(salesmanClient.getSchedules(?, ?)).thenReturn(Future.successful(Nil))
      when(salesmanClient.deleteGoods(?, ?, ?)).thenReturn(Future.unit)
      when(salesmanClient.deleteGoods(?, ?, ?)).thenReturn(Future.unit)
      when(cabinetClient.updateBadges(?, ?, ?, ?)).thenReturn(Future.unit)

      val (pub, sub) = createPubSub(pipeline.billingFlow())
      sub.request(3)
      pub.sendNext(buildAnswerMessage(taskContext))
      sub.expectNextN(3)

      verify(salesmanClient).getSchedules(?, ?)
      verify(salesmanClient).deleteGoods(?, argEq("1071967714-948c3"), ?)
      verify(salesmanClient).deleteGoods(?, argEq("1072717118-d4bbef52"), ?)
      verify(salesmanClient).createGoods(
        ?,
        argEq(Seq(GoodsRequest("1072717118-d4bbef52", task.saleCategory, task.section, Products.Special)))
      )
      verify(cabinetClient).updateBadges(task.category, task.clientId, 1071967714L, Seq("FOO"))
      verify(cabinetClient).updateBadges(task.category, task.clientId, 1072717118L, Seq("BAR"))
    }

    "handle leaveServices flag" in new Fixture {
      override lazy val task = tasksGen(Gen.const(Task.Status.Processing), serviceInfoGen(leaveServices = true)).next

      val goods = Json.parse(getClass.getResourceAsStream("/salesman_goods.json")).as[Seq[Goods]]
      when(salesmanClient.getGoods(?, ?)).thenReturn(Future.successful(goods))
      when(salesmanClient.getSchedules(?, ?))
        .thenReturn(
          Future.successful(
            Seq(Schedule("1071967714-948c3", List(1, 2, 3), LocalTime.now(), SalesmanClient.DefaulTimezone))
          )
        )
      when(salesmanClient.deleteGoods(?, ?, ?)).thenReturn(Future.unit)
      when(salesmanClient.deleteGoods(?, ?, ?)).thenReturn(Future.unit)
      when(cabinetClient.updateBadges(?, ?, ?, ?)).thenReturn(Future.unit)

      val (pub, sub) = createPubSub(pipeline.billingFlow())
      sub.request(3)
      pub.sendNext(buildAnswerMessage(taskContext))
      sub.expectNextN(3)

      verify(salesmanClient).getSchedules(?, ?)
      verify(salesmanClient, never()).deleteGoods(?, ?, ?)
      verify(salesmanClient, never()).deleteSchedule(?, ?)
    }

    "don't touch goods in case of list request failure" in new Fixture {
      when(salesmanClient.getGoods(?, ?)).thenReturn(Future.failed(new RuntimeException("Expected salesman error")))
      when(salesmanClient.getSchedules(?, ?)).thenReturn(Future.failed(new RuntimeException("Expected salesman error")))

      val (pub, sub) = createPubSub(pipeline.billingFlow())
      sub.request(3)

      pub.sendNext(buildAnswerMessage(taskContext))
      sub.expectNextN(3)

      verify(salesmanClient).getSchedules(?, ?)
      verify(salesmanClient).getGoods(?, ?)
    }
  }
}

class TestAutoruAnswerPipeline(val components: CoreComponents, val schedulerComponents: SchedulerComponents)
  extends AutoruAnswerPipeline
  with TestApplication
  with PipelineComponents[AutoruExternalOffer]
  with DummyOpsSupport {
  override def parallelism: Int = 2

  override protected def failedGetGoods(): Sink[(Throwable, OfferBatch), _] = Sink.ignore

  override protected def failedCreateGoods(): Sink[(Throwable, OfferBatch), _] = Sink.ignore

  override protected def failedDeleteGoods(): Sink[(Throwable, SalesmanService.SingleMessage), _] = Sink.ignore

  override protected def failedGetSchedules(): Sink[(Throwable, OfferBatch), _] = Sink.ignore

  override protected def failedAddSchedules(): Sink[(Throwable, SalesmanService.SingleMessage), _] = Sink.ignore

  override protected def failedDeleteSchedules(): Sink[(Throwable, SalesmanService.SingleMessage), _] = Sink.ignore

  override protected def failedUpdateBadges(): Sink[(Throwable, SalesmanService.SingleMessage), _] = Sink.ignore

  override protected def failedReportAnswers(): Sink[(Throwable, AnswersMessage), _] = Sink.ignore

  override def loader: Loader = ???

  override def taskDistribution: TokensDistribution = ???

  override def utilizationDistribution: TokensDistribution = ???

  override def taskPicker: TaskPicker = ???

  override def parser: Parser[AutoruExternalOffer] = ???

  override def mapper: MapperFlow[AutoruExternalOffer] = ???

  override def utilizator: UtilizatorService = ???
}
