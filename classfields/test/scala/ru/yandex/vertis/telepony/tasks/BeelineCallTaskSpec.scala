package ru.yandex.vertis.telepony.tasks

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime
import org.mockito.Mockito
import org.mockito.Mockito.verify
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => equ}
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.backend.ControllerComponent.SharedDbAvailable
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.journal.WriteJournal
import ru.yandex.vertis.telepony.model.BeelineGenerator._
import ru.yandex.vertis.telepony.model.{RawCall, TypedDomains}
import ru.yandex.vertis.telepony.service.{BeelineClient, BeelineStaticRawCallBuilder, DateTimeStorage, SharedPoolService}
import ru.yandex.vertis.telepony.tasks.shared.BeelineCallTask
import ru.yandex.vertis.telepony.util.CallTaskSettings

import scala.concurrent.Future

class BeelineCallTaskSpec extends SpecBase with MockitoSupport {

  implicit val ac = ActorSystem("test", ConfigFactory.empty())
  implicit val mat = ActorMaterializer()

  private val domain = TypedDomains.autoru_def

  trait Test {

    val mockedSharedPool = mock[SharedPoolService]
    val mockedDateTimeStorage = mock[DateTimeStorage]
    val mockedBeelineClient = mock[BeelineClient]
    val mockedJournal = mock[WriteJournal[RawCall]]
    val mockedCallBuilder = mock[BeelineStaticRawCallBuilder]

    val task: BeelineCallTask = new BeelineCallTask(
      sharedPoolService = mockedSharedPool,
      lastLoadedTimeStorage = mockedDateTimeStorage,
      beelineClient = mockedBeelineClient,
      callJournals = Map(domain -> mockedJournal),
      callBuilders = Map(domain -> mockedCallBuilder),
      sharedDb = SharedDbAvailable(() => true)
    )
  }

  "BeelineCallTask" should {
    "completed successfully when no calls and with empty config" in new Test {
      val fromTime = BeelineCallTask.MinimumDateTime.minusHours(BeelineCallTask.OverlapHours)
      val toTime = DateTime.now()
      val intervals = BeelineCallTask.buildIntervals(fromTime, toTime)
      when(mockedDateTimeStorage.get()).thenReturn(Future.successful(None))
      when(mockedBeelineClient.getCallHistory(?, ?)(?))
        .thenReturn(Future.successful(Seq()))

      task.run(CallTaskSettings(to = Some(toTime))).futureValue

      verify(mockedDateTimeStorage).get()
      verify(mockedBeelineClient, Mockito.times(intervals.size)).getCallHistory(?, ?)(?)
    }

    "completed successfully when no calls and config" in new Test {
      val fromStr = "2019-03-20T00:00"
      val fromTime = DateTime.parse(fromStr)
      val overlappedFromTime = fromTime.minusHours(BeelineCallTask.OverlapHours)
      val toTime = DateTime.parse("2019-03-26T23:10")
      val intervals = BeelineCallTask.buildIntervals(overlappedFromTime, toTime)
      when(mockedDateTimeStorage.set(fromTime)).thenReturn(Future.unit)
      when(mockedDateTimeStorage.get()).thenReturn(Future.successful(Some(fromTime)))
      when(mockedBeelineClient.getCallHistory(?, ?)(?))
        .thenReturn(Future.successful(Seq()))

      val config = ConfigFactory.parseString(s"""{ "reset-from": "$fromStr", "to": "$toTime" }""")
      task.payload(config).futureValue

      verify(mockedDateTimeStorage).get()
      verify(mockedBeelineClient, Mockito.times(intervals.size)).getCallHistory(?, ?)(?)
    }

    "failed when cant write call" in new Test {
      val proxy = PhoneGen.next
      val lastLoadedTime = DateTime.now().minusMonths(1)
      val toTime = DateTime.now()
      val overlappedLoadedTime = lastLoadedTime.minusHours(BeelineCallTask.OverlapHours)
      val rawCall = RawCallGen.next.copy(proxy = proxy)
      val entry = CallHistoryEntryGen.next.copy(proxyNumber = Some(proxy))
      val sharedNumber = SharedOperatorNumberGen.next.copy(domain = Some(domain))
      val th = new Exception

      when(mockedDateTimeStorage.get()).thenReturn(Future.successful(Some(lastLoadedTime)))
      when(mockedBeelineClient.getCallHistory(equ(overlappedLoadedTime), ?)(?))
        .thenReturn(Future.successful(Seq(entry)))
      when(mockedSharedPool.get(equ(proxy))(?)).thenReturn(Future.successful(sharedNumber))
      when(mockedCallBuilder.build(entry)).thenReturn(Some(rawCall))
      when(mockedJournal.send(rawCall)).thenReturn(Future.failed(th))

      task.run(CallTaskSettings(to = Some(toTime))).failed.futureValue should ===(th)

      verify(mockedDateTimeStorage).get()
      verify(mockedBeelineClient).getCallHistory(equ(overlappedLoadedTime), ?)(?)
      verify(mockedSharedPool).get(equ(proxy))(?)
      verify(mockedCallBuilder).build(entry)
      verify(mockedJournal).send(rawCall)
    }

    "complete and write call" in new Test {
      val proxy = PhoneGen.next
      val lastLoadedTime = DateTime.now().minusHours(1)
      val toTime = DateTime.now()
      val overlappedLoadedTime = lastLoadedTime.minusHours(BeelineCallTask.OverlapHours)
      val rawCall = RawCallGen.next.copy(proxy = proxy)
      val entry = CallHistoryEntryGen.next.copy(proxyNumber = Some(proxy))
      val sharedNumber = SharedOperatorNumberGen.next.copy(domain = Some(domain))

      when(mockedDateTimeStorage.get()).thenReturn(Future.successful(Some(lastLoadedTime)))
      when(mockedBeelineClient.getCallHistory(equ(overlappedLoadedTime), ?)(?))
        .thenReturn(Future.successful(Seq(entry)))
      when(mockedSharedPool.get(equ(proxy))(?)).thenReturn(Future.successful(sharedNumber))
      when(mockedCallBuilder.build(entry)).thenReturn(Some(rawCall))
      when(mockedJournal.send(rawCall)).thenReturn(Future.successful(null))
      when(mockedDateTimeStorage.set(rawCall.startTime))
        .thenReturn(Future.unit)

      task.run(CallTaskSettings(to = Some(toTime))).futureValue

      verify(mockedDateTimeStorage).get()
      verify(mockedBeelineClient).getCallHistory(equ(overlappedLoadedTime), ?)(?)
      verify(mockedSharedPool).get(equ(proxy))(?)
      verify(mockedCallBuilder).build(entry)
      verify(mockedJournal).send(rawCall)
      verify(mockedDateTimeStorage).set(rawCall.startTime)
    }

  }

}
