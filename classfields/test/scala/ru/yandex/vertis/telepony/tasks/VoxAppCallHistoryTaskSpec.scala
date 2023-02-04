package ru.yandex.vertis.telepony.tasks

import org.joda.time.DateTime
import org.scalatest.Ignore
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.journal.Journal.{EventEnvelope, KafkaMetadata}
import ru.yandex.vertis.telepony.journal.WriteJournal
import ru.yandex.vertis.telepony.model._
import ru.yandex.vertis.telepony.model.vox.GetCallHistoryItem
import ru.yandex.vertis.telepony.service.{DateTimeStorage, SharedRedirectService}
import ru.yandex.vertis.telepony.tasks.vox.VoxAppCallHistoryTask

import scala.concurrent.Future

@Ignore
class VoxAppCallHistoryTaskSpec extends VoxAbstractCallHistoryTaskSpec with SpecBase with MockitoSupport {

  trait Test {

    val mockedDateTimeStorage: DateTimeStorage = mock[DateTimeStorage]
    val mockedJournal: WriteJournal[RawAppCall] = mock[WriteJournal[RawAppCall]]
    val sharedRedirectService: SharedRedirectService = mock[SharedRedirectService]

    val task: VoxAppCallHistoryTask = new VoxAppCallHistoryTask(
      voxClient = client,
      sharedRedirectService = sharedRedirectService,
      lastLoadedTimeStorage = mockedDateTimeStorage,
      callJournals = Map(TypedDomains.autoru_def -> mockedJournal),
      callRuleName = "test_app2app_new_rule"
    )
  }

  "VoxAppCallHistoryTaskSpec" should {
    "execute getAllCalls and processElem" in new Test {
      when(sharedRedirectService.getDomain(?)).thenReturn(Future.successful(domain))
      val mockedEvent: EventEnvelope[KafkaMetadata, RawAppCall] = mock[EventEnvelope[KafkaMetadata, RawAppCall]]
      when(mockedJournal.send(?)).thenReturn(Future.successful(mockedEvent))

      val fromDateTime: DateTime = DateTime.parse("2021-02-18T12:00:00+03:00")
      val toDateTime: DateTime = DateTime.parse("2021-02-18T13:00:00+03:00")

      val calls: Iterable[GetCallHistoryItem] = task.getAllCalls(fromDateTime, toDateTime).futureValue
      calls.foreach { item =>
        task.processElem(item).futureValue
      }
    }
  }
}
