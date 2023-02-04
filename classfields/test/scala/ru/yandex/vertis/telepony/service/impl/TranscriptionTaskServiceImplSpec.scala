package ru.yandex.vertis.telepony.service.impl

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.dao.TranscriptionTaskDao
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.{CallDialog, CallbackGenerator, RecordId, TranscriptionTask}
import ru.yandex.vertis.telepony.service.CallbackCallService
import ru.yandex.vertis.telepony.service.logging.LoggingTranscriptionTaskService
import ru.yandex.vertis.telepony.util.{AutomatedContext, RequestContext}

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * @author neron
  */
class TranscriptionTaskServiceImplSpec extends SpecBase with MockitoSupport {

  implicit private val rc: RequestContext =
    AutomatedContext("transcription-task-service")

  private def createServiceAndMocks = {
    val dao = mock[TranscriptionTaskDao]
    val calls = mock[CallbackCallService]
    val service = new TranscriptionTaskServiceImpl(dao, calls) with LoggingTranscriptionTaskService
    (service, dao, calls)
  }

  private val recordId: RecordId = ShortStr.next

  "TranscriptionTaskService" should {
    "create" in {
      val (service, dao, _) = createServiceAndMocks
      val task = TranscriptionTask.newTask(recordId)
      def matcher = argThat { t: TranscriptionTask =>
        t == task.copy(createTime = t.createTime, updateTime = t.updateTime)
      }
      when(dao.create(matcher)).thenReturn(Future.successful(true))
      service.create(recordId).futureValue
      Mockito.verify(dao).create(?)
    }
    "return full dialog" in {
      val (service, dao, calls) = createServiceAndMocks
      val call = CallbackGenerator.CallbackGen.next
      val dialog = DialogGen.next
      when(dao.getDialog(call.id)).thenReturn(Future.successful(Some(dialog)))
      service.getDialog(call.id, cutIntro = false).futureValue.value shouldEqual dialog
      Mockito.verifyZeroInteractions(calls)
      Mockito.verify(dao).getDialog(?)
    }
    "return cutted dialog" in {
      val (service, dao, calls) = createServiceAndMocks
      val call = CallbackGenerator.CallbackGen.next.copy(duration = 40.millis, talkDuration = 20.millis)
      val dialog = CallDialog(
        Seq(
          DialogPhraseGen.next.copy(startTimeMillis = 0, endTimeMillis = 20),
          DialogPhraseGen.next.copy(startTimeMillis = 20, endTimeMillis = 40)
        )
      )
      when(dao.getDialog(call.id)).thenReturn(Future.successful(Some(dialog)))
      when(calls.getByRecordIdOpt(call.id)).thenReturn(Future.successful(Some(call)))

      service.getDialog(call.id, cutIntro = true).futureValue.value.phrases.sizeIs == 1
      Mockito.verify(calls).getByRecordIdOpt(?)(?)
      Mockito.verify(dao).getDialog(?)
    }
  }
}
