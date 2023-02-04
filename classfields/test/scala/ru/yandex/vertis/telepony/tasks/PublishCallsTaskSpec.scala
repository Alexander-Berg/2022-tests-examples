package ru.yandex.vertis.telepony.tasks

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime
import org.mockito.Mockito.verify
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => equ}
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.call.CallsSink
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.TypedDomains
import ru.yandex.vertis.telepony.service.offsets.OffsetStorage
import ru.yandex.vertis.telepony.service.offsets.OffsetStorage.Offset
import ru.yandex.vertis.telepony.service.{ActualCallService, RecordService}
import ru.yandex.vertis.telepony.tasks.PublishCallsTask.TaskSettings
import ru.yandex.vertis.telepony.time._

import scala.concurrent.Future

/**
  * @author neron
  */
class PublishCallsTaskSpec extends SpecBase with MockitoSupport {

  implicit val as: ActorSystem = ActorSystem("callTranscriptionInspectionTaskSpec", ConfigFactory.empty())
  implicit val am: ActorMaterializer = ActorMaterializer()

  private val domain = TypedDomains.autoru_def

  trait TestEnv {
    val callService = mock[ActualCallService]
    when(callService.listUpdated(?, ?, ?)(?)).thenReturn(Future.successful(Nil))
    val recordService = mock[RecordService]
    when(recordService.existsLoaded(?)(?)).thenReturn(Future.successful(false))
    val offsetStorage = mock[OffsetStorage]
    when(offsetStorage.getLastCommitted).thenReturn(Future.successful(None))
    val callsSink = mock[CallsSink]
    when(callsSink.write(?)).thenReturn(Future.unit)
  }

  "PublishCallsTask" should {
    "reset last offset" in new TestEnv {
      val resetOffset = Offset(DateTime.now(), "id")
      when(offsetStorage.commit(equ(resetOffset))).thenReturn(Future.unit)
      val task = new PublishCallsTask(callService, recordService, offsetStorage, callsSink, domain)
      task.run(TaskSettings(resetFrom = Some(resetOffset))).futureValue

      verify(offsetStorage).commit(equ(resetOffset))
    }

    "process batch and commit" in new TestEnv {
      val calls = CallV2Gen.next(PublishCallsTask.CallBatchSize).toSeq.sortBy(_.updateTime)
      when(callService.listUpdated(?, ?, ?)(?)).thenReturn(Future.successful(calls))
      val newOffset = Offset(calls.last.updateTime, calls.last.id)
      when(offsetStorage.commit(equ(newOffset))).thenReturn(Future.unit)
      val task = new PublishCallsTask(callService, recordService, offsetStorage, callsSink, domain)
      task.run(TaskSettings()).futureValue

      verify(offsetStorage).commit(equ(newOffset))
    }
  }

}
