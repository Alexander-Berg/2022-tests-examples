package ru.yandex.vertis.billing.tasks

import org.mockito.ArgumentCaptor
import org.mockito.Mockito.{clearInvocations, times, verify, verifyNoInteractions}
import org.scalatest.BeforeAndAfterEach
import ru.yandex.vertis.billing.async.AsyncSpecBase
import ru.yandex.vertis.billing.dao.impl.jdbc.{JdbcHoboDao, JdbcSpecTemplate}
import ru.yandex.vertis.billing.event.Extractor
import ru.yandex.vertis.billing.model_core.ServiceObject.Kinds
import ru.yandex.vertis.billing.model_core.gens.{CallComplaintGen, CallModerationTaskGen, ModerationTaskGen, Producer}
import ru.yandex.vertis.billing.model_core.{CallFactTagParser, RealtyCallTagParser}
import ru.yandex.vertis.billing.service.EpochService
import ru.yandex.vertis.billing.tasks.HoboPushTaskSpec.{EpochMock, HoboClientMock}
import ru.yandex.vertis.hobo.client.HoboClient
import ru.yandex.vertis.hobo.proto.Model
import ru.yandex.vertis.hobo.proto.Model.{QueueId, TaskSource}
import ru.yandex.vertis.mockito.MockitoSupport.?

import scala.jdk.CollectionConverters._
import scala.concurrent.Future
import scala.util.Success

/**
  * @author ruslansd
  */
class HoboPushTaskSpec extends AsyncSpecBase with JdbcSpecTemplate with BeforeAndAfterEach {

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    clearInvocations(HoboClientMock)
  }

  private val hoboDao = new JdbcHoboDao(campaignEventDatabase)

  private val commonQueueId = QueueId.TELEPONY_AUTORU_PAID_CALL
  private val callCenterCallQueueId = QueueId.TELEPONY_REALTY_PAID_CALL
  private val callCarUsedQueueId = QueueId.AUTO_RU_USED_CALL_AUCTION

  private def task(tagParser: Option[CallFactTagParser] = None) =
    new HoboPushTask(
      hoboDao,
      HoboClientMock,
      EpochMock,
      "test",
      commonQueueId.toString,
      Some(callCenterCallQueueId.toString),
      Some(callCarUsedQueueId.toString),
      tagParser
    )

  "HoboPushTask" should {

    "do nothing on empty set" in {
      task().execute().futureValue
      verifyNoInteractions(HoboClientMock)
    }

    "push new tasks to hobo" in {
      val tasks = ModerationTaskGen.next(5).toList

      (hoboDao.add(tasks) should be).a(Symbol("Success"))

      task().execute().futureValue

      verify(HoboClientMock, times(5)).createTask(?, ?, ?)
    }

    "do not push already pushed tasks" in {
      task().execute().futureValue
      verifyNoInteractions(HoboClientMock)
    }

    "push only new tasks" in {

      val tasks = ModerationTaskGen.next(5).toList

      (hoboDao.add(tasks) should be).a(Symbol("Success"))

      task().execute().futureValue

      verify(HoboClientMock, times(5)).createTask(?, ?, ?)
    }

    "handle call with tags" in {
      val tagged = {
        val cmt = CallModerationTaskGen.next
        val taggedFact = cmt.fact.withTag(Some(s"${Extractor.BuildingType}=${Extractor.Suburban}"))
        cmt.copy(fact = taggedFact)
      }

      (hoboDao.add(Iterable(tagged)) should be).a(Symbol("Success"))
      val captor: ArgumentCaptor[TaskSource] = ArgumentCaptor.forClass(classOf[TaskSource])

      task(Some(RealtyCallTagParser)).execute().futureValue

      verify(HoboClientMock, times(1)).createTask(?, captor.capture(), ?)
      val taskSource = captor.getValue
      taskSource.getLabelsCount shouldBe 1
      taskSource.getLabelsList.asScala.head shouldBe RealtyCallTagParser.asString(tagged.fact.tag).get
    }

    "handle call without tag" in {
      val withOutTag = CallModerationTaskGen.next

      (hoboDao.add(Iterable(withOutTag)) should be).a(Symbol("Success"))
      val captor: ArgumentCaptor[TaskSource] = ArgumentCaptor.forClass(classOf[TaskSource])

      task(Some(RealtyCallTagParser)).execute().futureValue

      verify(HoboClientMock, times(1)).createTask(?, captor.capture(), ?)
      val taskSource = captor.getValue
      taskSource.getLabelsCount shouldBe 1
      taskSource.getLabelsList.asScala.head shouldBe Kinds.NewBuilding.toString
    }

    "handle call with tags and withouth call tag parser" in {
      val tagged = {
        val cmt = CallModerationTaskGen.next
        val taggedFact = cmt.fact.withTag(Some(s"${Extractor.BuildingType}=${Extractor.Suburban}"))
        cmt.copy(fact = taggedFact)
      }

      (hoboDao.add(Iterable(tagged)) should be).a(Symbol("Success"))
      val captor: ArgumentCaptor[TaskSource] = ArgumentCaptor.forClass(classOf[TaskSource])

      task().execute().futureValue

      verify(HoboClientMock, times(1)).createTask(?, captor.capture(), ?)
      val taskSource = captor.getValue
      taskSource.getLabelsList.asScala shouldBe empty
    }

    "push task to call center call queue" in {

      val taskForCommonQueue = CallModerationTaskGen.next.copy(callCenterCallId = None)
      val taskForCallCenterCallQueue = CallModerationTaskGen.next.copy(
        callCenterCallId = Some("beeper-123456"),
        complaint = None
      )
      val taskWithComplaint = taskForCallCenterCallQueue.copy(
        complaint = Some(CallComplaintGen.next)
      )

      (hoboDao.add(Iterable(taskForCommonQueue, taskForCallCenterCallQueue, taskWithComplaint)) should be)
        .a(Symbol("Success"))
      val captor: ArgumentCaptor[QueueId] = ArgumentCaptor.forClass(classOf[QueueId])
      val taskCaptor: ArgumentCaptor[TaskSource] = ArgumentCaptor.forClass(classOf[TaskSource])

      task().execute().futureValue

      verify(HoboClientMock, times(3)).createTask(captor.capture(), taskCaptor.capture(), ?)
      val queueIds = captor.getAllValues.asScala
      val taskIdToQueue = queueIds
        .zip(taskCaptor.getAllValues.asScala)
        .map { case (k, v) =>
          v.getPayload.getExternal.getIdsList.asScala.head -> k
        }
        .toMap

      taskIdToQueue(taskForCommonQueue.id) shouldBe commonQueueId
      taskIdToQueue(taskWithComplaint.id) shouldBe commonQueueId
      taskIdToQueue(taskForCallCenterCallQueue.id) shouldBe callCenterCallQueueId
    }

    "push task for used cars to call-car-used queue" in {

      val taskForCommonQueue = CallModerationTaskGen.next.copy(callCenterCallId = None)
      val taskForCallCarUsedQueue = {
        val cmt = CallModerationTaskGen.next
        val taggedFact = cmt.fact.withTag(Some(s"section=USED"))
        cmt.copy(fact = taggedFact)
      }

      (hoboDao.add(Iterable(taskForCommonQueue, taskForCallCarUsedQueue)) should be)
        .a(Symbol("Success"))
      val captor: ArgumentCaptor[QueueId] = ArgumentCaptor.forClass(classOf[QueueId])
      val taskCaptor: ArgumentCaptor[TaskSource] = ArgumentCaptor.forClass(classOf[TaskSource])

      task().execute().futureValue

      verify(HoboClientMock, times(2)).createTask(captor.capture(), taskCaptor.capture(), ?)
      val queueIds = captor.getAllValues.asScala
      val taskIdToQueue = queueIds
        .zip(taskCaptor.getAllValues.asScala)
        .map { case (k, v) =>
          v.getPayload.getExternal.getIdsList.asScala.head -> k
        }
        .toMap

      taskIdToQueue(taskForCommonQueue.id) shouldBe commonQueueId
      taskIdToQueue(taskForCallCarUsedQueue.id) shouldBe callCarUsedQueueId
    }

  }

}

object HoboPushTaskSpec extends AsyncSpecBase {
  import ru.yandex.vertis.mockito.MockitoSupport.{mock, stub}

  private val HoboClientMock = {
    val m = mock[HoboClient]

    stub(m.createTask _) { case _ =>
      Future(Model.Task.newBuilder().setVersion(1).build())
    }
    m
  }

  private val EpochMock = {
    var epoch = 0L
    val m = mock[EpochService]

    stub(m.get _) { case marker =>
      Future.successful(epoch)
    }
    stub(m.set _) { case (_, e) =>
      epoch = e
      Future.successful(())
    }
    m
  }
}
