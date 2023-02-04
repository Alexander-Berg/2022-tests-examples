package ru.yandex.vertis.telepony.service

import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.Timeout
import org.joda.time.DateTime
import org.scalatest.Ignore
import org.scalatest.time.{Seconds, Span}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.telepony.client.records.RecordsBaseSpec
import ru.yandex.vertis.telepony.http.HttpClientImpl
import ru.yandex.vertis.telepony.model._
import ru.yandex.vertis.telepony.service.RecordRecognitionService.Completed
import ru.yandex.vertis.telepony.service.impl.SpeechKitRecordRecognitionService
import ru.yandex.vertis.telepony.service.impl.recognize.SpeechKitCloudClientImpl
import ru.yandex.vertis.telepony.service.logging.LoggingSpeechKitCloudClient
import ru.yandex.vertis.telepony.util.http.client.PipelineBuilder
import ru.yandex.vertis.telepony.util.records.impl.{AudioConverterImpl, FileSystemExecutorImpl}
import ru.yandex.vertis.telepony.util.{AutomatedContext, RequestContext, Threads}
import ru.yandex.vertis.telepony.{IntegrationSpecTemplate, SpecBase}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

@Ignore
class RecordRecognitionServiceIntSpec
  extends RecordsBaseSpec
  with SpecBase
  with MockitoSupport
  with IntegrationSpecTemplate {

  implicit private val ec: ExecutionContext = Threads.lightWeightTasksEc
  implicit val rc: RequestContext = AutomatedContext(id = "test")

  implicit override def patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(125, Seconds), interval = Span(1, Seconds))

  implicit val mat: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(system), "SK")

  protected val sendReceive = PipelineBuilder
    .buildSendReceive(
      proxy = None,
      maxConnections = 3,
      requestTimeout = Timeout(120.seconds),
      readTimeout = Timeout(120.seconds)
    )

  private val httpClient = new HttpClientImpl("default-spec-client", sendReceive, None)

  private val speechKitClient: SpeechKitCloudClient =
    new SpeechKitCloudClientImpl(httpClient, speechKitClientSettings) with LoggingSpeechKitCloudClient with DefaultTskv

  private val RecordId: RecordId = "1"

  def record(bytes: Array[Byte]): Record = Record(
    RecordMeta(
      RecordId,
      OperatorAccounts.BeelineShared,
      "url",
      name = Some("x.wav"),
      None,
      DateTime.now(),
      customS3Prefix = None
    ),
    bytes
  )

  trait Test {
    val rs = mock[RecordService]
    when(rs.get(?)(?)).thenReturn(Future.successful(record(beeline_new_record)))

    val fileSystemExecutor = new FileSystemExecutorImpl(Threads.blockingEc)

    val PhrasesMergeIntervalMillis: Long = 350L

    val service: RecordRecognitionService = new SpeechKitRecordRecognitionService(
      recordService = rs,
      audioConverter = new AudioConverterImpl(fileSystemExecutor),
      s3ForSpeechKitClient = s3CloudClient,
      speechKitCloudClient = speechKitClient,
      phrasesMergeIntervalMillis = PhrasesMergeIntervalMillis
    )

  }

  "RecordRecognitionService" should {
    "recognize records" in new Test {
      val recognitionId = service.startRecognition("1").futureValue
      eventually {
        Thread.sleep(3000)
        val Completed(t, _) = service.getStatus(RecordId, recognitionId).futureValue
        println(t)
      }
    }

  }

}
