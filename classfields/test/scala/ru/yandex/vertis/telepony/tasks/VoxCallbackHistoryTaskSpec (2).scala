package ru.yandex.vertis.telepony.tasks

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime
import org.scalatest.Ignore
import ru.yandex.vertis.application.runtime.VertisRuntime
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.prometheus.PrometheusRegistry
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.journal.WriteJournal
import ru.yandex.vertis.telepony.model.CallbackGenerator.CallbackOrderSourceGen
import ru.yandex.vertis.telepony.model.CallbackOrder._
import ru.yandex.vertis.telepony.model.vox.GetCallHistoryItem
import ru.yandex.vertis.telepony.model._
import ru.yandex.vertis.telepony.operational.Operational
import ru.yandex.vertis.telepony.service.impl.vox.{VoxClient, VoxClientImpl}
import ru.yandex.vertis.telepony.service.{CallbackOrderService, DateTimeStorage}
import ru.yandex.vertis.telepony.tasks.vox.VoxCallbackHistoryTask
import ru.yandex.vertis.telepony.util.Threads
import ru.yandex.vertis.telepony.util.http.client.{HttpClientBuilder, PipelineBuilder}

import scala.concurrent.Future

@Ignore
class VoxCallbackHistoryTaskSpec extends VoxAbstractCallHistoryTaskSpec with SpecBase with MockitoSupport {

  trait Test {

    val mockedDateTimeStorage: DateTimeStorage = mock[DateTimeStorage]
    val mockedJournal: WriteJournal[RawCallback] = mock[WriteJournal[RawCallback]]
    val callbackService: CallbackOrderService = mock[CallbackOrderService]

    val task: VoxCallbackHistoryTask = new VoxCallbackHistoryTask(
      voxClient = client,
      callbackService = callbackService,
      lastLoadedTimeStorage = mockedDateTimeStorage,
      callJournals = Map(TypedDomains.autoru_def -> mockedJournal),
      callRuleName = "test_callback_rule"
    )
  }

  "VoxCallbackHistoryTask" should {
    "execute getAllCallbacks and toRawCallback" in new Test {
      val toDateTime: DateTime = DateTime.now()
      val fromDateTime: DateTime = toDateTime.minusDays(1)
      val phone = Phone("+79123456789")
      val callPeriodSeq: Seq[CallPeriod] = Seq(CallPeriod(fromDateTime, toDateTime))
      val callbackOrderSource = CallbackOrderSourceGen.next
      val callbackOrder = new CallbackOrder(
        id = CallbackOrder.Id("id"),
        objectId = ObjectId("objectId"),
        tag = Tag.Empty,
        payload = PayloadGen.next,
        createTime = fromDateTime,
        status = CallbackOrderStatus(Statuses.Scheduled, fromDateTime),
        source = SourceInfo(phone, CallerIdModes.SystemNumber),
        target = TargetInfo(phone, CallerIdModes.SystemNumber, callPeriodSeq, NotificationOptGen.next),
        domain = domain,
        callbackOrderSource = callbackOrderSource
      )
      val callbackOrders: Future[Iterable[CallbackOrder]] = Future.successful(Seq(callbackOrder))

      when(callbackService.list(?, ?)(?)).thenReturn(callbackOrders)

      val callbacks: Iterable[GetCallHistoryItem] = task.getAllCalls(fromDateTime, toDateTime).futureValue
      callbacks.foreach { item =>
        task.processElem(item).futureValue
      }
    }
  }
}

object VoxCallbackHistoryTaskSpec {
  val ApplicationName = "test.yavert-test.voximplant.com"
  val CallbackRule = "test_callback_rule"
}
