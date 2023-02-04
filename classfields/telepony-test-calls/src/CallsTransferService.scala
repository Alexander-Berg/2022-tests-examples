package ru.yandex.vertis.etc.telepony.telepony_test_calls

import common.yt.Yt
import doobie.Transactor
import doobie.implicits._
import doobie.util.fragment.Fragment
import org.joda.time.DateTime
import ru.yandex.inside.yt.kosher.cypress.YPath
import ru.yandex.vertis.broker.client.marshallers.GoogleProtoMarshaller.googleProtoMarshaller
import ru.yandex.vertis.broker.client.simple.BrokerClient
import ru.yandex.vertis.etc.telepony.telepony_test_calls.TestCallsTask.TestCallsConfig
import ru.yandex.vertis.telepony.model.proto.CallResult
import ru.yandex.vertis.telepony.call.CallsSink
import ru.yandex.vertis.telepony.proto.ModelProtoConversions
import ru.yandex.vertis.telepony.dao.jdbc.SharedOperatorNumberDao
import ru.yandex.vertis.telepony.dao.{HistoryRedirectDaoV2, TranscriptionTaskDao}
import ru.yandex.vertis.telepony.model.{
  AntiFraudOptions,
  CallDialog,
  CallResults,
  CallTranscription,
  CallV2,
  HistoryRedirect,
  ObjectId,
  OperatorAccounts,
  OperatorNumber,
  Operators,
  Phone,
  PhoneTypes,
  RecordMeta,
  RedirectId,
  RedirectKey,
  RefinedSource,
  SharedNumberStatusValues,
  SharedOperatorNumber,
  Status,
  Tag,
  TeleponyCall,
  TranscriptionTaskStatusValues,
  TypedDomains
}
import ru.yandex.vertis.telepony.proto.CallTranscriptionProto
import ru.yandex.vertis.telepony.service.dust.DustClient
import ru.yandex.vertis.telepony.service.{ActualCallService, CallService, RecordService, TranscriptionTaskService}
import ru.yandex.vertis.telepony.tasks.RecordRecognitionTask
import ru.yandex.vertis.telepony.time.TimeZone
import ru.yandex.vertis.telepony.util.AutomatedContext
import ru.yandex.vertis.telepony.util.db.DualDatabase
import zio.interop.catz._
import zio.clock.Clock
import zio.{Task, ZIO}

import java.time.{Instant, ZoneId}
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID
import scala.concurrent.Future
import scala.concurrent.duration._

class CallsTransferService(
    yt: Yt.Service,
    ytTransactor: Transactor[Task],
    db: DualDatabase,
    redirectDao: HistoryRedirectDaoV2,
    config: TestCallsConfig,
    clock: Clock,
    callService: ActualCallService,
    sharedOperatorNumberDao: SharedOperatorNumberDao,
    transcriptionTaskService: TranscriptionTaskService,
    recordService: RecordService,
    callsSink: CallsSink,
    brokerClient: BrokerClient,
    dustClient: DustClient) {
  import CallsTransferService._

  def run: Task[Unit] =
    for {
      now <- clock.get.instant
      _ <- prepareEntriesTable(now)
      _ <- getEntries
        .chunkN(10)
        .foreachChunk(chunk => ZIO.foreachPar_(chunk)(handleEntry))
    } yield ()

  private def handleEntry(entry: YtEntry): Task[Unit] = {
    val domain = TypedDomains.withName(entry.domain)

    val rc = AutomatedContext("test-calls-task")

    val historyRedirect = HistoryRedirect(
      id = RedirectId(entry.redirect_id),
      key = RedirectKey(ObjectId(entry.object_id), Phone(entry.target_phone), Tag.apply(entry.tag.filter(_.nonEmpty))),
      createTime = DateTime.parse(entry.redirect_create_time).minusHours(3).withZone(TimeZone),
      endTime = entry.redirect_end_time.map(t => DateTime.parse(t).minusHours(3).withZone(TimeZone)),
      number = Phone(entry.redirect_call_info.proxy_number),
      geoId = entry.redirect_call_info.proxy_geo_id,
      operator = Operators(entry.operator_id),
      originOperator = entry.origin_operator.map(Operators.apply),
      account = OperatorAccounts.byId(entry.account_id),
      antiFraud = AntiFraudOptions.unpack(entry.redirect_antifraud.toLong),
      options = None
    )

    val createRequest = CallService.CreateRequest(
      externalId = UUID.randomUUID().toString,
      source = Some(RefinedSource.from(entry.source_phone)),
      target = Phone(entry.target_phone),
      proxy = Phone(entry.redirect_call_info.proxy_number),
      time = new DateTime(entry.time / 1000, TimeZone),
      duration = entry.duration_seconds.seconds,
      talkDuration = entry.talk_duration_seconds.seconds,
      hasRecord = true,
      redirect = historyRedirect,
      callResult =
        ModelProtoConversions.CallResultProtoConversion.from(CallResult.valueOf(entry.redirect_call_info.call_result)),
      fallbackCall = None,
      overrideCallId = Some(entry.call_id)
    )

    val operatorNumber = OperatorNumber(
      number = Phone(entry.redirect_call_info.proxy_number),
      account = historyRedirect.account,
      originOperator = Operators.apply(entry.operator_id),
      geoId = historyRedirect.geoId,
      phoneType = PhoneTypes.Mobile,
      status = Status.Fake(None),
      lastTarget = Some(historyRedirect.target)
    )

    val sharedOperatorNumber = SharedOperatorNumber(
      number = Phone(entry.redirect_call_info.proxy_number),
      account = historyRedirect.account,
      originOperator = entry.origin_operator.map(Operators.apply),
      geoId = historyRedirect.geoId,
      phoneType = PhoneTypes.Mobile,
      domain = None,
      status = SharedNumberStatusValues.Fake
    )

    val callDialog = CallDialog(
      entry.dialog.phrases.map { phrase =>
        CallDialog.Phrase(
          speaker = if (phrase.speaker == "SOURCE") CallDialog.Speakers.Source else CallDialog.Speakers.Target,
          startTimeMillis = phrase.start_time_millis,
          endTimeMillis = phrase.end_time_millis,
          text = phrase.text,
          tags = Seq.empty
        )
      }
    )
    val transcriptionCreate = TranscriptionTaskDao.UpdateRequest(
      status = TranscriptionTaskStatusValues.Successful,
      payload = Some(callDialog)
    )

    val recordMeta = RecordMeta(
      id = createRequest.callId,
      account = operatorNumber.account,
      url = "",
      name = None,
      groupId = None,
      time = DateTime.now,
      lastUploadTime = Some(DateTime.now),
      status = true,
      customS3Prefix = Some("")
    )

    for {
      _ <- ZIO.fromFuture(_ => sharedOperatorNumberDao.upsert(sharedOperatorNumber))
      _ <- ZIO.fromFuture(_ =>
        db.master.run("save_history_redirect", redirectDao.saveHistory(historyRedirect, Some(domain)))
      )
      call: CallV2 <-
        ZIO.fromFuture { implicit ec =>
          callService.getCallOpt(createRequest.callId)(rc).flatMap {
            case Some(call) => Future.successful(call)
            case None => callService.create(createRequest)(rc)
          }
        }
      _ <- ZIO.fromFuture(_ => recordService.saveExisting(recordMeta, domain)(rc))
      teleponyCall = TeleponyCall.from(call, domain, recordLoaded = true, Set.empty)
      _ <- ZIO.fromFuture(_ => transcriptionTaskService.create(createRequest.callId, Some(domain)))
      _ <- ZIO.fromFuture(_ => transcriptionTaskService.update(createRequest.callId, transcriptionCreate))
      _ <- ZIO.fromFuture(_ => callsSink.write(teleponyCall))
      callTranscription = CallTranscription(
        createRequest.callId,
        domain,
        callDialog,
        DateTime.now(),
        entry.raw_transcription
      )
      _ <- ZIO.fromFuture(_ =>
        brokerClient.send(
          Some(createRequest.callId),
          CallTranscriptionProto.CallTranscriptionConversion.to(callTranscription)
        )
      )
      dustClusterTypes = RecordRecognitionTask.getSendingClusterTypes(teleponyCall, callDialog)
      _ <- ZIO
        .fromFuture(_ => dustClient.sendTranscription(teleponyCall, callDialog, dustClusterTypes))
        .when(dustClusterTypes.nonEmpty)
    } yield ()
  }

  private def getEntries =
    yt.tables.read[YtEntry](YPath.simple(config.ytEntries))

  private def prepareEntriesTable(now: Instant) = {
    val yesterday = now.minus(1, ChronoUnit.HOURS)
    val date = DateTimeFormatter.ISO_LOCAL_DATE.format(yesterday.atZone(ZoneId.of("Europe/Moscow")))

    val nowMicros = yesterday.toEpochMilli * 1000
    val previousHourMicros = yesterday.minus(1, ChronoUnit.HOURS).toEpochMilli * 1000

    val yql =
      s"""
         |USE hahn;
         |PRAGMA yt.TmpFolder = "//home/verticals/.tmp";
         |
         |$$calls = select * from `${config.ytCalls}/$date`
         |    where redirect_call_info is not null and has_record = true
         |      and time > CAST($previousHourMicros AS Timestamp)
         |      and time <= CAST($nowMicros AS Timestamp);
         |
         |insert into `${config.ytEntries}` with truncate
         |
         |select
         |    c.call_id as call_id,
         |    c.object_id as object_id,
         |    c.tag as tag,
         |    c.domain as domain,
         |    c.time as time,
         |    c.duration_seconds as duration_seconds,
         |    c.talk_duration_seconds as talk_duration_seconds,
         |    c.source_phone as source_phone,
         |    c.target_phone as target_phone,
         |    Yson::From(c.redirect_call_info) as redirect_call_info,
         |    Yson::From(t.dialog) as dialog,
         |    t.raw_transcription as raw_trascription,
         |    r.id as redirect_id,
         |    r.operator_id as operator_id,
         |    r.origin_operator as origin_operator,
         |    r.account_id as account_id,
         |    r.create_time as redirect_create_time,
         |    r.end_time as redirect_end_time,
         |    r.antifraud as redirect_antifraud
         |from
         |    $$calls as c inner join `${config.ytTranscriptions}/$date` as t on c.call_id = t.call_id
         |    inner join `${config.ytRedirectHistory}` as r on c.redirect_call_info.redirect_id = r.id
         |""".stripMargin

    Fragment
      .const0(yql)
      .update
      .run
      .transact(ytTransactor)
  }
}

object CallsTransferService {

  case class Phrase(text: String, speaker: String, tags: List[String], start_time_millis: Int, end_time_millis: Int)

  case class Dialog(phrases: List[Phrase])

  case class RedirectCallInfo(
      call_result: String,
      origin_operator: Option[String],
      proxy_geo_id: Int,
      proxy_number: String,
      proxy_operator: String,
      redirect_id: String)

  case class YtEntry(
      call_id: String,
      object_id: String,
      tag: Option[String],
      domain: String,
      time: Long,
      duration_seconds: Int,
      talk_duration_seconds: Int,
      source_phone: String,
      target_phone: String,
      redirect_call_info: RedirectCallInfo,
      dialog: Dialog,
      raw_transcription: String,
      redirect_id: String,
      operator_id: Int,
      origin_operator: Option[Int],
      account_id: Int,
      redirect_create_time: String,
      redirect_end_time: Option[String],
      redirect_antifraud: Int)
}
